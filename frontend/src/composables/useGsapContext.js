import { onUnmounted, shallowRef } from 'vue';
import { gsap } from '../plugins/gsap';
import { prefersReducedMotion } from '../utils/motion';

/**
 * Vue 3 + GSAP 生命周期封装（gsap-frameworks：context + revert）
 */
export function useGsapContext(scopeRef) {
  const ctx = shallowRef(null);

  /** 在 onMounted 中调用，scope 限定选择器范围 */
  const createContext = (setupFn) => {
    if (!scopeRef?.value || prefersReducedMotion()) {
      setupFn?.();
      return;
    }
    ctx.value = gsap.context(setupFn, scopeRef.value);
  };

  onUnmounted(() => {
    ctx.value?.revert();
    ctx.value = null;
  });

  return { gsap, createContext, ctx };
}
