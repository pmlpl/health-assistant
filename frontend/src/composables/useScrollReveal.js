import { onMounted, onUnmounted } from 'vue';
import { gsap, ScrollTrigger } from '../plugins/gsap';
import { prefersReducedMotion, motionDuration } from '../utils/motion';

/**
 * 单元素 ScrollTrigger 入场（gsap-scrolltrigger：once + scoped cleanup）
 */
export function useScrollReveal(targetRef, options = {}) {
  let ctx = null;

  onMounted(() => {
    const el = targetRef.value;
    if (!el || prefersReducedMotion()) return;

    ctx = gsap.context(() => {
      gsap.from(el, {
        y: options.y ?? 24,
        autoAlpha: 0,
        duration: motionDuration(options.duration ?? 0.6),
        ease: options.ease ?? 'power2.out',
        scrollTrigger: {
          trigger: el,
          start: options.start ?? 'top 88%',
          once: true,
          ...options.scrollTrigger,
        },
      });
    }, el);
  });

  onUnmounted(() => {
    ctx?.revert();
    ctx = null;
  });
}
