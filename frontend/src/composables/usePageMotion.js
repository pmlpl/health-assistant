import { onMounted, onUnmounted, nextTick } from 'vue';
import { gsap, ScrollTrigger } from '../plugins/gsap';
import { prefersReducedMotion, motionDuration } from '../utils/motion';

const DEFAULT_REVEAL = '.card, .motion-reveal, .app-card';

/**
 * 全页动效：路由入场 + 标题 stagger + 卡片 reveal + 数字滚动
 */
export function usePageMotion(scopeRef, options = {}) {
  let ctx = null;
  let initialEnterDone = false;
  const revealSelector = options.revealSelector ?? DEFAULT_REVEAL;
  const skipEnter = options.skipEnter ?? false;
  const shouldRun = () => (options.when ? options.when() : true);

  const clearMotionState = (root) => {
    if (!root) return;
    gsap.set(root.querySelectorAll(revealSelector), { clearProps: 'all' });
    gsap.set(root, { clearProps: 'all' });
  };

  const runCountUp = (root, { immediate = false } = {}) => {
    const selector = immediate ? '[data-count-immediate]' : '[data-count-up]';
    root.querySelectorAll(selector).forEach((el, index) => {
      const raw = el.dataset.countImmediate ?? el.dataset.countUp ?? el.textContent;
      const end = Number(String(raw).replace(/[^\d.-]/g, '')) || 0;
      const suffix = el.dataset.countSuffix ?? '';
      const obj = { val: 0 };

      const tweenVars = {
        val: end,
        duration: motionDuration(immediate ? 0.9 : 1.1),
        delay: immediate ? index * 0.08 : 0,
        ease: 'power2.out',
        onUpdate: () => {
          el.textContent = `${Math.round(obj.val)}${suffix}`;
        },
      };

      if (immediate) {
        gsap.to(obj, tweenVars);
      } else {
        gsap.to(obj, {
          ...tweenVars,
          scrollTrigger: {
            trigger: el,
            start: 'top 92%',
            once: true,
          },
        });
      }
    });
  };

  /** 已在视口内的卡片直接入场，其余走 ScrollTrigger.batch */
  const revealCards = (root) => {
    const reveals = Array.from(root.querySelectorAll(revealSelector));
    if (!reveals.length) return;

    const inView = [];
    const belowFold = [];
    const threshold = window.innerHeight * 0.9;

    reveals.forEach((el) => {
      const top = el.getBoundingClientRect().top;
      if (top < threshold) inView.push(el);
      else belowFold.push(el);
    });

    if (inView.length) {
      gsap.fromTo(
        inView,
        { autoAlpha: 0, y: 26 },
        {
          autoAlpha: 1,
          y: 0,
          stagger: 0.09,
          duration: motionDuration(0.52),
          ease: 'power2.out',
          overwrite: true,
        }
      );
    }

    if (belowFold.length) {
      gsap.set(belowFold, { autoAlpha: 0, y: 26 });
      ScrollTrigger.batch(belowFold, {
        onEnter: (batch) => {
          gsap.to(batch, {
            autoAlpha: 1,
            y: 0,
            stagger: 0.09,
            duration: motionDuration(0.52),
            ease: 'power2.out',
            overwrite: true,
          });
        },
        start: 'top 90%',
        once: true,
      });
    }
  };

  const setup = async () => {
    await nextTick();
    const root = scopeRef.value;
    if (!root) return;

    ctx?.revert();
    ctx = null;

    if (!shouldRun()) {
      clearMotionState(root);
      return;
    }

    if (prefersReducedMotion()) {
      clearMotionState(root);
      return;
    }

    // 清除上次路由可能残留的 visibility/opacity
    gsap.killTweensOf(root);
    gsap.set(root, { autoAlpha: 1, clearProps: 'transform' });

    ctx = gsap.context(() => {
      if (!skipEnter && !initialEnterDone) {
        gsap.from(root, {
          autoAlpha: 0,
          y: options.enterY ?? 14,
          duration: motionDuration(0.42),
          ease: 'power2.out',
        });
        initialEnterDone = true;
      }

      const header = root.querySelector('.page-header');
      if (header) {
        gsap.from(header.children, {
          y: 18,
          autoAlpha: 0,
          stagger: 0.07,
          duration: motionDuration(0.48),
          ease: 'power2.out',
          delay: skipEnter ? 0 : 0.06,
        });
      }

      revealCards(root);
      runCountUp(root, { immediate: true });
      runCountUp(root, { immediate: false });
    }, root);

    ScrollTrigger.refresh();
  };

  onMounted(() => {
    setup();
  });

  onUnmounted(() => {
    ctx?.revert();
    ctx = null;
  });

  return {
    replay: setup,
    refresh: () => ScrollTrigger.refresh(),
  };
}
