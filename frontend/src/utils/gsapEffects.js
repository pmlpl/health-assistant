import { gsap } from '../plugins/gsap';
import { motionDuration, prefersReducedMotion } from './motion';

/** 提交按钮波纹扩散 */
export function createRipple(event, button) {
  if (prefersReducedMotion() || !button) return;
  const ripple = document.createElement('span');
  ripple.className = 'ripple-wave';
  const rect = button.getBoundingClientRect();
  const size = 10;
  ripple.style.width = `${size}px`;
  ripple.style.height = `${size}px`;
  ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
  ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
  button.appendChild(ripple);
  gsap.to(ripple, {
    scale: 20,
    opacity: 0,
    duration: motionDuration(0.6),
    ease: 'power2.out',
    onComplete: () => ripple.remove(),
  });
}

/** 表单验证失败左右微抖 */
export function shakeElement(el) {
  if (prefersReducedMotion() || !el) return;
  gsap.fromTo(
    el,
    { x: 0 },
    {
      x: -8,
      duration: 0.06,
      repeat: 5,
      yoyo: true,
      ease: 'power1.inOut',
      onComplete: () => gsap.set(el, { x: 0 }),
    }
  );
}
