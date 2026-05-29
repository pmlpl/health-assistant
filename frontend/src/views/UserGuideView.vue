<template>
  <div class="guide-page texture-noise">
    <!-- Hero：蓝色 Bauhaus 色块 -->
    <header class="guide-hero bauhaus-block bauhaus-block--blue">
      <div class="guide-hero__inner">
        <SectionLabel label="使用手册" />
        <div class="guide-hero__punct" aria-hidden="true">
          <span class="guide-hero__rule" />
          <span class="guide-hero__square" />
        </div>
        <h1 class="guide-hero__title bh-heading-on-color">
          <span class="guide-hero__line">AI 健康</span>
          <span class="guide-hero__line guide-hero__line--accent">助手</span>
        </h1>
        <p class="guide-hero__subtitle">
          各模块说明、平台试用规则与 API Key 配置 — 一文读懂如何用好你的健康助手
        </p>
        <div class="guide-hero__actions">
          <router-link to="/" class="btn-brand">开始使用 →</router-link>
          <a href="#overview" class="btn-outline guide-hero__ghost">向下浏览</a>
        </div>
      </div>
    </header>

    <!-- 锚点导航：线条分割，无圆角 -->
    <nav class="guide-nav" aria-label="页面目录">
      <a v-for="item in navItems" :key="item.id" :href="item.id">{{ item.label }}</a>
    </nav>

    <hr class="rule-thick" />

    <div class="guide-body texture-lines">
      <section id="overview" class="guide-section">
        <SectionLabel label="01 · 总览" />
        <h2 class="guide-section__title">快速了解产品能力</h2>
        <p class="guide-section__lead">
          本应用使用<strong>大模型 API</strong> 实现咨询、分析、识图与配图。您可以使用<strong>平台代付的试用额度</strong>，
          或在「AI 设置」中填写<strong>自己的 Key</strong>（不占用试用次数，费用由您向厂商支付）。
        </p>
      </section>

      <hr class="rule-thick" />

      <section id="platform" class="guide-section">
        <SectionLabel label="02 · 平台能力" />
        <h2 class="guide-section__title">平台默认提供什么？</h2>
        <div class="guide-table-wrap bh-card">
          <table class="guide-table">
            <thead>
              <tr>
                <th>能力</th>
                <th>平台使用模型</th>
                <th>试用限制</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>AI 对话、营养咨询、日记/健身分析、食谱文案</td>
                <td><strong>DeepSeek</strong>（如 deepseek-v4-flash）</td>
                <td><strong>不限次数</strong></td>
              </tr>
              <tr>
                <td>饮食日记「拍照上传」识食</td>
                <td><strong>豆包</strong> 视觉</td>
                <td><strong>10 次 / 用户</strong></td>
              </tr>
              <tr>
                <td>AI 食谱配图（文生图）</td>
                <td><strong>豆包 Seedream</strong></td>
                <td><strong>10 次 / 用户</strong></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="callout callout--info">
          食谱配图改为豆包<strong>直接生成</strong>美食图片，效果更稳定。
        </div>
      </section>

      <!-- 黄色 Bauhaus 统计色块 -->
      <section class="guide-stats bauhaus-block bauhaus-block--yellow" aria-label="试用额度统计">
        <div class="guide-stats__inner">
          <div v-for="stat in stats" :key="stat.label" class="bh-strip stat-item">
            <span class="stat-value">{{ stat.value }}</span>
            <span>{{ stat.label }}</span>
          </div>
        </div>
      </section>

      <hr class="rule-thick" />

      <section id="trial" class="guide-section">
        <SectionLabel label="03 · 试用额度" />
        <h2 class="guide-section__title">试用额度与自备 Key</h2>
        <div class="callout callout--warn">
          <strong>拍照识食</strong>或<strong>食谱配图</strong>试用各 10 次用完后，请在
          <strong>「API 状态」→ AI 设置</strong> 填写您自己的豆包 Key。
        </div>
        <ul class="guide-list">
          <li><strong>DeepSeek 对话</strong>：平台代付<strong>不限次</strong></li>
          <li><strong>豆包拍照</strong>：10 次 / 用户</li>
          <li><strong>豆包食谱生图</strong>：10 次 / 用户（与拍照分开计数）</li>
          <li>自备 Key 后：对应功能不再扣减平台额度</li>
        </ul>
      </section>

      <hr class="rule-thick" />

      <section id="ai-settings" class="guide-section">
        <SectionLabel label="04 · AI 设置" />
        <h2 class="guide-section__title">AI 设置（右上角「API 状态」）</h2>
        <div class="guide-subsections">
          <article class="bh-card guide-subcard">
            <h3>4.1 文本对话（默认 DeepSeek）</h3>
            <ul>
              <li>新用户默认服务商：<strong>DeepSeek</strong></li>
              <li>可改为通义、豆包、本地 LM Studio 等</li>
              <li>填写对应厂商 API Key 即可覆盖平台代付</li>
            </ul>
          </article>
          <article class="bh-card guide-subcard">
            <h3>4.2 拍照识食</h3>
            <ul>
              <li>推荐视觉服务商：<strong>豆包</strong></li>
              <li>视觉模型：填火山方舟<strong>推理接入点</strong></li>
              <li><strong>无需配置 base_url</strong>，系统已内置官方 endpoint</li>
            </ul>
          </article>
          <article class="bh-card guide-subcard">
            <h3>4.3 食谱配图</h3>
            <p>由后端使用豆包 Seedream 模型自动生成，配置在服务器环境变量中。</p>
          </article>
        </div>
      </section>

      <hr class="rule-thick" />

      <section id="modules" class="guide-section texture-grid">
        <SectionLabel label="05 · 功能模块" />
        <h2 class="guide-section__title">各功能模块一览</h2>
        <div class="module-grid">
          <article
            v-for="(mod, index) in modules"
            :key="mod.title"
            class="module-item"
            :class="{ 'card-featured': index === 0 }"
          >
            <div :class="index === 0 ? 'card-featured__inner bh-strip' : 'bh-card module-card'">
              <h3>{{ mod.icon }} {{ mod.title }}</h3>
              <p v-if="mod.desc">{{ mod.desc }}</p>
              <ul v-if="mod.items">
                <li v-for="(line, i) in mod.items" :key="i">{{ line }}</li>
              </ul>
            </div>
          </article>
        </div>
      </section>

      <hr class="rule-thick" />

      <section id="faq" class="guide-section">
        <SectionLabel label="06 · 常见问题" />
        <h2 class="guide-section__title">FAQ</h2>
        <dl class="faq-list">
          <div v-for="faq in faqs" :key="faq.q" class="faq-item bh-card">
            <span class="faq-item__quote" aria-hidden="true">"</span>
            <dt>{{ faq.q }}</dt>
            <dd>{{ faq.a }}</dd>
          </div>
        </dl>
      </section>
    </div>

    <footer class="guide-cta page-cta">
      <div class="guide-cta__inner">
        <h2>准备好开始了吗？</h2>
        <p>记录第一餐饮食，或向 AI 精灵提一个营养问题。</p>
        <router-link to="/" class="btn-brand">进入首页 →</router-link>
      </div>
    </footer>
  </div>
</template>

<script setup>
import SectionLabel from '../components/common/SectionLabel.vue';

const navItems = [
  { id: '#overview', label: '总览' },
  { id: '#platform', label: '平台能力' },
  { id: '#trial', label: '试用额度' },
  { id: '#ai-settings', label: 'AI 设置' },
  { id: '#modules', label: '功能模块' },
  { id: '#faq', label: '常见问题' },
];

/** 反色统计区数据 */
const stats = [
  { value: '∞', label: 'DeepSeek 对话' },
  { value: '10', label: '豆包拍照 / 用户' },
  { value: '10', label: '食谱生图 / 用户' },
];

const modules = [
  {
    icon: '💬',
    title: 'AI 精灵 / 营养咨询',
    desc: '平台 DeepSeek，不限次。分析、建议、对话均走文本模型。',
  },
  {
    icon: '📔',
    title: '饮食日记',
    items: ['手动记录：无 AI', '文字识食 / AI 分析：DeepSeek', '拍照上传：豆包视觉（10 次试用）'],
  },
  {
    icon: '🏋️',
    title: '健身记录',
    desc: 'AI 建议与分析：DeepSeek。',
  },
  {
    icon: '🍳',
    title: 'AI 食谱',
    desc: '文案：DeepSeek；配图：豆包 Seedream 生图（10 次试用）。',
  },
];

const faqs = [
  { q: '提示「平台试用额度已用完」？', a: '拍照或食谱配图 10 次已用尽。请在 AI 设置填写豆包 Key。' },
  { q: '豆包要填 base_url 吗？', a: '不需要。系统使用火山方舟官方 endpoint。' },
  { q: 'DeepSeek 和豆包 Key 都要配吗？', a: '试用阶段不必。长期或超额后按需分别配置。' },
];
</script>

<style scoped>
.guide-page {
  min-height: 100vh;
  background: var(--color-background);
  color: var(--color-text);
}

/* —— Hero：与 PageHeader 同系蓝底点阵 —— */
.guide-hero {
  padding: var(--space-20) var(--space-6) var(--space-16);
  background-color: var(--color-primary-blue);
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 18px 18px;
}

.guide-hero__inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-4);
}

.guide-hero__punct {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin: var(--space-6) 0 var(--space-8);
}

.guide-hero__rule {
  display: block;
  width: 4rem;
  height: 4px;
  background: #fff;
}

.guide-hero__square {
  display: block;
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  background: var(--color-primary-yellow);
}

.guide-hero__title {
  margin: 0 0 var(--space-6);
  font-family: var(--font-display);
  font-weight: var(--font-weight-black);
  letter-spacing: -0.04em;
  line-height: 0.95;
  text-transform: uppercase;
}

.guide-hero__line {
  display: block;
  font-size: clamp(var(--font-size-5xl), 12vw, var(--font-size-8xl));
}

.guide-hero__line--accent {
  font-style: italic;
  padding-left: var(--space-8);
}

.guide-hero__subtitle {
  margin: 0 0 var(--space-10);
  max-width: 38rem;
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.85);
}

.guide-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.guide-hero__ghost {
  background: #fff;
  color: var(--color-foreground);
}

/* —— 锚点导航 —— */
.guide-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
}

.guide-nav a {
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-foreground);
  text-decoration: none;
  padding: var(--space-3) var(--space-4);
  border: 1px solid transparent;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.guide-nav a:hover {
  border-color: var(--color-foreground);
  text-decoration: none;
}

.guide-nav a:focus-visible {
  outline: 3px solid var(--color-foreground);
  outline-offset: 2px;
}

/* —— 正文区 —— */
.guide-body {
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: var(--space-16) var(--space-6);
}

.guide-section {
  padding: var(--space-12) 0;
}

.guide-section__title {
  margin: 0 0 var(--space-6);
  font-family: var(--font-display);
  font-size: clamp(var(--font-size-2xl), 4vw, var(--font-size-4xl));
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.guide-section__lead {
  margin: 0;
  font-size: var(--font-size-lg);
  line-height: 1.75;
  color: var(--color-text-secondary);
  max-width: 52rem;
}

/* —— 表格 —— */
.guide-table-wrap {
  overflow-x: auto;
  padding: var(--space-6);
  margin: var(--space-6) 0;
}

.guide-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

.guide-table th,
.guide-table td {
  padding: var(--space-4) var(--space-5);
  text-align: left;
  border-bottom: 1px solid var(--color-border-light);
}

.guide-table th {
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
  border-bottom: 2px solid var(--color-foreground);
}

/* —— 提示框：纯线条，无彩色 —— */
.callout {
  padding: var(--space-5) var(--space-6);
  margin: var(--space-6) 0;
  line-height: 1.7;
  border: 2px solid var(--color-foreground);
  background: var(--color-muted);
}

.callout--info,
.callout--warn {
  border-width: 2px;
  border-color: var(--color-foreground);
  background: var(--color-muted);
  color: var(--color-text);
}

.guide-list {
  margin: var(--space-6) 0 0;
  padding-left: var(--space-6);
  line-height: 1.85;
  color: var(--color-text-secondary);
}

.guide-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

/* —— 黄色统计色块 —— */
.guide-stats {
  margin: var(--space-12) calc(var(--space-6) * -1);
  padding: var(--space-14) var(--space-6);
}

.guide-stats .stat-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  text-align: center;
}

.guide-stats .stat-value {
  font-family: var(--font-display);
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-black);
  color: var(--color-primary-red);
}

.guide-stats__inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-6);
}

/* —— 子卡片 —— */
.guide-subsections {
  display: grid;
  gap: var(--space-5);
}

.guide-subcard {
  padding: var(--space-6) var(--space-8);
}

.guide-subcard h3 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.guide-subcard ul {
  margin: 0;
  padding-left: var(--space-5);
  color: var(--color-text-secondary);
  line-height: 1.75;
}

.guide-subcard p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.75;
}

/* —— 功能模块网格 —— */
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-6);
  margin-top: var(--space-8);
}

.module-card {
  padding: var(--space-8);
  height: 100%;
}

.module-card h3,
.card-featured__inner h3 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.module-card p,
.card-featured__inner p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.module-card ul,
.card-featured__inner ul {
  margin: var(--space-3) 0 0;
  padding-left: var(--space-5);
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.card-featured__inner {
  padding: var(--space-8);
  height: 100%;
}

/* hover 时 featured 卡片内文字 */
.card-featured:hover .card-featured__inner p,
.card-featured:hover .card-featured__inner li {
  color: inherit;
  opacity: 0.85;
}

/* —— FAQ 编辑风引语 —— */
.faq-list {
  display: grid;
  gap: var(--space-5);
  margin: var(--space-8) 0 0;
}

.faq-item {
  position: relative;
  padding: var(--space-8) var(--space-8) var(--space-6);
  border-top: 2px solid var(--color-foreground);
  transition: border-width var(--transition-fast);
}

.faq-item:hover {
  border-top-width: 4px;
}

.faq-item__quote {
  position: absolute;
  top: var(--space-4);
  left: var(--space-6);
  font-family: var(--font-display);
  font-size: var(--font-size-6xl);
  line-height: 1;
  opacity: 0.12;
  transition: opacity var(--transition-fast);
}

.faq-item:hover .faq-item__quote {
  opacity: 0.22;
}

.faq-item dt {
  font-family: var(--font-display);
  font-size: var(--font-size-xl);
  font-style: italic;
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-3);
  padding-top: var(--space-6);
}

.faq-item dd {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.75;
}

.faq-item:hover dd {
  color: inherit;
  opacity: 0.85;
}

/* —— 底部 CTA —— */
.guide-cta {
  padding: var(--space-20) var(--space-6);
}

.guide-cta__inner {
  max-width: 36rem;
  margin: 0 auto;
}

.guide-cta h2 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.02em;
}

.guide-cta p {
  margin: 0 0 var(--space-8);
  font-size: var(--font-size-lg);
  color: var(--color-foreground);
  line-height: 1.65;
}

@media (max-width: 640px) {
  .guide-hero__line--accent {
    padding-left: 0;
  }

  .guide-stats {
    margin-left: calc(var(--space-4) * -1);
    margin-right: calc(var(--space-4) * -1);
  }
}
</style>
