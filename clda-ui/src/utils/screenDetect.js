/**
 * 屏幕模式检测
 * PC横屏 vs 机器人8寸竖屏
 */

/**
 * 判断是否为机器人竖屏模式
 */
export function isRobotScreen() {
  return window.innerWidth <= 820 && window.matchMedia('(orientation: portrait)').matches
}
