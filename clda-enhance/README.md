# CLDA Image Enhancement Service

FastAPI microservice providing 4x image super-resolution via Real-ESRGAN. Falls back to OpenCV-based enhancement (bilateral filter + unsharp mask) when Real-ESRGAN is unavailable.

## Run locally

```bash
cd clda-enhance
pip install -r requirements.txt
python app.py
```

The service starts on port 8090 by default.

## Run via Docker

```bash
docker build -t clda-enhance .
docker run -p 8090:8090 -v /opt/clda/uploadPath:/opt/clda/uploadPath clda-enhance
```

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8090` | Server listen port |
| `UPLOAD_BASE_PATH` | `/opt/clda/uploadPath` | Root directory for saving enhanced images |
| `REALESRGAN_MODEL_PATH` | *(remote URL)* | Path or URL to RealESRGAN_x4plus.pth model weights |

## API

**POST /enhance** -- Upload an image for 4x super-resolution.

```bash
curl -X POST http://localhost:8090/enhance \
  -F "file=@photo.jpg"
```

Response:
```json
{
  "success": true,
  "enhanced_path": "/opt/clda/uploadPath/enhanced/photo_enhanced_a1b2c3d4.jpg",
  "backend": "realesrgan"
}
```

**GET /health** -- Health check.

```json
{"status": "ok"}
```
