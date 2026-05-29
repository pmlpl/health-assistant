<template>
  <div v-if="isVisible" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>手动输入营养成分</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      
      <div class="modal-body">
        <div class="food-info" v-if="foodItem">
          <h4>{{ foodItem.name }}</h4>
          <p class="food-quantity">{{ foodItem.quantity }}{{ foodItem.unit }}</p>
        </div>
        
        <div class="nutrition-form">
          <div class="form-group">
            <label>热量 (kcal)</label>
            <input 
              v-model.number="nutritionData.calories" 
              type="number" 
              placeholder="请输入热量"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>蛋白质 (g)</label>
            <input 
              v-model.number="nutritionData.protein" 
              type="number" 
              placeholder="请输入蛋白质含量"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>碳水化合物 (g)</label>
            <input 
              v-model.number="nutritionData.carbs" 
              type="number" 
              placeholder="请输入碳水化合物含量"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>脂肪 (g)</label>
            <input 
              v-model.number="nutritionData.fat" 
              type="number" 
              placeholder="请输入脂肪含量"
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>膳食纤维 (g)</label>
            <input 
              v-model.number="nutritionData.fiber" 
              type="number" 
              placeholder="请输入膳食纤维含量"
              class="form-input"
            />
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="handleClose">取消</button>
        <button class="btn btn-primary" @click="handleSave">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isVisible: {
    type: Boolean,
    default: false
  },
  foodItem: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'save'])

const nutritionData = ref({
  calories: null,
  protein: null,
  carbs: null,
  fat: null,
  fiber: null
})

// 当组件显示时，重置数据
watch(() => props.isVisible, (newVal) => {
  if (newVal) {
    nutritionData.value = {
      calories: null,
      protein: null,
      carbs: null,
      fat: null,
      fiber: null
    }
  }
})

const handleClose = () => {
  emit('close')
}

const handleOverlayClick = () => {
  handleClose()
}

const handleSave = () => {
  emit('save', { ...nutritionData.value })
  handleClose()
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: var(--color-card);
  border-radius: 0;
  border: var(--border-width-thick) solid var(--color-border);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5);
  border-bottom: var(--border-width) solid var(--color-border);
}

.modal-header h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  color: var(--color-foreground);
  text-transform: uppercase;
}

.food-info {
  background: var(--color-muted);
  padding: var(--space-4);
  border-radius: 0;
  border: var(--border-width) solid var(--color-border);
  margin-bottom: var(--space-5);
}

.food-info h4 {
  margin: 0 0 var(--space-1) 0;
  color: var(--color-foreground);
}

.food-quantity {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: var(--font-size-sm);
}

.form-group label {
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: var(--space-1);
  font-size: var(--font-size-sm);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.form-input {
  padding: var(--space-3);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  font-size: var(--font-size-base);
  background: var(--color-card);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary-blue);
  box-shadow: var(--shadow-sm);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-secondary {
  background: #f0f0f0;
  color: #666;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5a6fd6;
}
</style>