<!-- src/components/layout/Footer.vue — Bauhaus 页脚 -->
<template>
  <footer class="footer">
    <div class="footer-content">
      <div class="footer-section">
        <div class="footer-logo bh-logo-shapes" aria-hidden="true">
          <span class="bh-logo-shapes__circle" />
          <span class="bh-logo-shapes__square" />
          <span class="bh-logo-shapes__triangle" />
        </div>
        <h4>AI健康助手</h4>
        <p class="footer-description">智能健康管理，让生活更美好</p>
      </div>
      <div class="footer-links">
        <a href="#" class="footer-link">隐私政策</a>
        <span class="divider" aria-hidden="true">|</span>
        <a href="#" class="footer-link">使用条款</a>
        <span class="divider" aria-hidden="true">|</span>
        <a href="#" class="footer-link">联系我们</a>
      </div>
      <div class="footer-copyright">
        <p>Copyright &copy; 2026 AI健康助手. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  background: var(--color-muted);
  border-top: var(--border-width) solid var(--color-border);
  padding: var(--space-8) var(--space-12);
  width: 100%;
}

.footer-content {
  max-width: var(--content-area-max-width);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-6);
}

.footer-logo {
  justify-content: center;
  margin-bottom: var(--space-2);
}

.footer-section {
  text-align: center;
}

.footer-section h4 {
  margin: 0 0 var(--space-2) 0;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.footer-description {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-muted-foreground);
}

.footer-links {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
  justify-content: center;
}

.footer-link {
  color: var(--color-foreground);
  text-decoration: none;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--color-primary-red);
}

.divider {
  color: var(--color-border-light);
  font-size: var(--font-size-xs);
}

.footer-copyright p {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-muted-foreground);
}

@media (max-width: 768px) {
  .footer {
    padding: var(--space-6) var(--space-5);
  }

  .divider {
    display: none;
  }

  .footer-links {
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
