/** 是否应减少动效（系统无障碍设置） */
export function prefersReducedMotion() {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** GSAP 默认 duration：尊重 reduced-motion */
export function motionDuration(normalSeconds = 0.5) {
  return prefersReducedMotion() ? 0.01 : normalSeconds;
}
