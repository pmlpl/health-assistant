<template>
  <canvas ref="canvasRef" class="auth-particles" aria-hidden="true" />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { prefersReducedMotion } from '../../utils/motion';

const canvasRef = ref(null);
let rafId = null;
let particles = [];

const initParticles = (width, height) => {
  particles = Array.from({ length: 30 }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    r: 1 + Math.random() * 2,
    vx: (Math.random() - 0.5) * 0.35,
    vy: (Math.random() - 0.5) * 0.35,
    alpha: 0.15 + Math.random() * 0.35,
  }));
};

const draw = () => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const { width, height } = canvas;

  ctx.clearRect(0, 0, width, height);
  particles.forEach((p) => {
    p.x += p.vx;
    p.y += p.vy;
    if (p.x < 0) p.x = width;
    if (p.x > width) p.x = 0;
    if (p.y < 0) p.y = height;
    if (p.y > height) p.y = 0;

    ctx.beginPath();
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
    ctx.fillStyle = `rgba(255,255,255,${p.alpha})`;
    ctx.fill();
  });

  rafId = requestAnimationFrame(draw);
};

const resize = () => {
  const canvas = canvasRef.value;
  if (!canvas?.parentElement) return;
  const rect = canvas.parentElement.getBoundingClientRect();
  canvas.width = rect.width;
  canvas.height = rect.height;
  initParticles(canvas.width, canvas.height);
};

onMounted(() => {
  if (prefersReducedMotion()) return;
  resize();
  draw();
  window.addEventListener('resize', resize);
});

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId);
  window.removeEventListener('resize', resize);
});
</script>

<style scoped>
.auth-particles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
