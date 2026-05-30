<template>
  <div class="chart-container bh-card">
    <h3 class="chart-title">{{ title }}</h3>
    <v-chart class="chart" :option="option" autoresize />
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  option: {
    type: Object,
    required: true,
  },
});
</script>

<style scoped>
.chart-container {
  padding: var(--space-5);
  background: var(--color-card);
  border-radius: 0;
}

.chart-title {
  font-family: var(--font-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
  margin-bottom: var(--space-5);
}

.chart {
  height: 350px;
}
</style>
