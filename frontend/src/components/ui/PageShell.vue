<script setup>

import { ref, onMounted } from 'vue';

import { useGsapContext } from '../../composables/useGsapContext';

import { motionDuration } from '../../utils/motion';



/** 统一页面容器：标题区 + 内容槽 + GSAP 入场 */

const props = defineProps({

  title: { type: String, default: '' },

  subtitle: { type: String, default: '' },

  animate: { type: Boolean, default: true },

});



const shellRef = ref(null);

const { createContext, gsap } = useGsapContext(shellRef);



onMounted(() => {

  if (!props.animate) return;

  createContext(() => {

    gsap.from('.page-shell__header > *', {

      y: 20,

      autoAlpha: 0,

      stagger: 0.08,

      duration: motionDuration(0.5),

      ease: 'power2.out',

    });

    gsap.from('.page-shell__body > *', {

      y: 24,

      autoAlpha: 0,

      stagger: 0.1,

      duration: motionDuration(0.55),

      ease: 'power2.out',

      delay: 0.08,

    });

  });

});

</script>



<template>

  <div ref="shellRef" class="page-shell" :class="{ 'page-shell--animate': animate }">

    <header v-if="title || $slots.header" class="page-shell__header">

      <slot name="header">

        <h1 class="page-shell__title">{{ title }}</h1>

        <p v-if="subtitle" class="page-shell__subtitle">{{ subtitle }}</p>

      </slot>

    </header>

    <div class="page-shell__body">

      <slot />

    </div>

  </div>

</template>



<style scoped>

.page-shell {

  width: 100%;

  max-width: var(--content-max-width);

  margin: 0 auto;

  padding: var(--space-6) 0 var(--space-10);

}



.page-shell__header {

  margin-bottom: var(--space-6);

}



.page-shell__title {

  font-size: var(--font-size-3xl);

  font-weight: 700;

  color: var(--color-text);

  letter-spacing: -0.02em;

  line-height: 1.2;

}



.page-shell__subtitle {

  margin-top: var(--space-2);

  font-size: var(--font-size-base);

  color: var(--color-text-secondary);

}



.page-shell__body {

  display: flex;

  flex-direction: column;

  gap: var(--space-6);

}



@media (max-width: 768px) {

  .page-shell {

    padding: var(--space-4) var(--space-4) var(--space-8);

  }



  .page-shell__title {

    font-size: var(--font-size-2xl);

  }

}

</style>

