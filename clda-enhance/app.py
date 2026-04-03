"""
CLDA Image Enhancement Microservice

Provides Real-ESRGAN 4x super-resolution via a FastAPI endpoint.
Falls back to OpenCV-based enhancement when Real-ESRGAN is unavailable
(e.g. no GPU, missing model weights, import failure).
"""

import logging
import os
import uuid
from pathlib import Path

import cv2
import numpy as np
import uvicorn
from fastapi import BackgroundTasks, FastAPI, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from PIL import Image

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

PORT = int(os.getenv("PORT", "8090"))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger("clda-enhance")

# ---------------------------------------------------------------------------
# Real-ESRGAN initialisation (best-effort)
# ---------------------------------------------------------------------------

_upsampler = None

try:
    from basicsr.archs.rrdbnet_arch import RRDBNet
    from realesrgan import RealESRGANer

    model = RRDBNet(
        num_in_ch=3, num_out_ch=3, num_feat=64, num_block=23, num_grow_ch=32
    )
    model_path = os.getenv(
        "REALESRGAN_MODEL_PATH",
        "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth",
    )
    _upsampler = RealESRGANer(
        scale=4,
        model_path=model_path,
        model=model,
        tile=0,
        tile_pad=10,
        pre_pad=0,
        half=False,  # CPU-safe; set True when running on CUDA fp16
    )
    logger.info("Real-ESRGAN model loaded successfully")
except Exception as exc:
    logger.warning(
        "Real-ESRGAN unavailable (%s); falling back to OpenCV enhancement", exc
    )

# ---------------------------------------------------------------------------
# Enhancement helpers
# ---------------------------------------------------------------------------

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".webp"}


def _enhance_realesrgan(img_bgr: np.ndarray) -> np.ndarray:
    """Upscale using Real-ESRGAN."""
    output, _ = _upsampler.enhance(img_bgr, outscale=4)
    return output


def _enhance_opencv(img_bgr: np.ndarray) -> np.ndarray:
    """CPU fallback: 4x bicubic upscale + bilateral filter + unsharp mask."""
    h, w = img_bgr.shape[:2]
    upscaled = cv2.resize(img_bgr, (w * 4, h * 4), interpolation=cv2.INTER_CUBIC)

    # Bilateral filter for edge-preserving smoothing
    smooth = cv2.bilateralFilter(upscaled, d=9, sigmaColor=75, sigmaSpace=75)

    # Unsharp mask for sharpening
    blurred = cv2.GaussianBlur(smooth, (0, 0), sigmaX=3)
    sharpened = cv2.addWeighted(smooth, 1.5, blurred, -0.5, 0)

    return sharpened


def enhance_image(img_bgr: np.ndarray) -> np.ndarray:
    """Route to the best available enhancement backend."""
    if _upsampler is not None:
        return _enhance_realesrgan(img_bgr)
    return _enhance_opencv(img_bgr)


# ---------------------------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------------------------

app = FastAPI(title="CLDA Image Enhancement Service", version="1.0.0")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/enhance")
async def enhance(background_tasks: BackgroundTasks, file: UploadFile = File(...)):
    # Validate file type
    if file.filename is None:
        raise HTTPException(status_code=400, detail="Filename is required")

    ext = Path(file.filename).suffix.lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported file type '{ext}'. Allowed: {', '.join(sorted(ALLOWED_EXTENSIONS))}",
        )

    # Read uploaded bytes
    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(status_code=400, detail="Empty file")
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Failed to read upload: %s", exc)
        raise HTTPException(status_code=400, detail="Failed to read uploaded file")

    # Decode image
    nparr = np.frombuffer(contents, np.uint8)
    img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img_bgr is None:
        raise HTTPException(status_code=400, detail="Could not decode image")

    # Enhance
    try:
        result_bgr = enhance_image(img_bgr)
    except Exception as exc:
        logger.error("Enhancement failed: %s", exc)
        raise HTTPException(status_code=500, detail="Image enhancement failed")

    # Save to temp file
    stem = Path(file.filename).stem
    out_name = f"{stem}_enhanced_{uuid.uuid4().hex[:8]}{ext}"
    out_path = Path("/tmp") / out_name

    try:
        if ext in {".jpg", ".jpeg"}:
            cv2.imwrite(str(out_path), result_bgr, [cv2.IMWRITE_JPEG_QUALITY, 95])
        elif ext == ".png":
            cv2.imwrite(str(out_path), result_bgr, [cv2.IMWRITE_PNG_COMPRESSION, 3])
        else:
            cv2.imwrite(str(out_path), result_bgr)
    except Exception as exc:
        logger.error("Failed to save enhanced image: %s", exc)
        raise HTTPException(status_code=500, detail="Failed to save enhanced image")

    backend = "realesrgan" if _upsampler is not None else "opencv"
    logger.info("Enhanced %s -> %s (backend=%s)", file.filename, out_path, backend)

    # Clean up temp file after response is sent
    background_tasks.add_task(out_path.unlink, missing_ok=True)

    media_type = "image/jpeg" if ext in {".jpg", ".jpeg"} else "image/png"
    return FileResponse(str(out_path), media_type=media_type, filename=out_name)


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=PORT, log_level="info")
