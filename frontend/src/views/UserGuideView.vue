<template>
  <div class="guide-page">
    <header class="guide-header">
      <h1>AI 健康助手 · 使用手册</h1>
      <p class="subtitle">各模块说明、平台试用规则与 API Key 配置</p>
      <router-link to="/" class="back-link">← 返回首页</router-link>
    </header>

    <nav class="guide-nav">
      <a href="#overview">总览</a>
      <a href="#platform">平台提供的能力</a>
      <a href="#trial">试用额度</a>
      <a href="#ai-settings">AI 设置</a>
      <a href="#modules">功能模块</a>
      <a href="#faq">常见问题</a>
    </nav>

    <section id="overview" class="guide-section">
      <h2>1. 总览</h2>
      <p>
        本应用使用<strong>大模型 API</strong> 实现咨询、分析、识图与配图。您可以使用<strong>平台代付的试用额度</strong>，
        或在「AI 设置」中填写<strong>自己的 Key</strong>（不占用试用次数，费用由您向厂商支付）。
      </p>
    </section>

    <section id="platform" class="guide-section">
      <h2>2. 平台默认提供什么？</h2>
      <table class="guide-table">
        <thead>
          <tr><th>能力</th><th>平台使用模型</th><th>试用限制</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>AI 对话、营养咨询、日记/健身分析、食谱文案</td>
            <td><strong>DeepSeek</strong>（如 deepseek-v4-flash）</td>
            <td><strong>不限次数</strong></td>
          </tr>
          <tr>
            <td>饮食日记「拍照上传」识食</td>
            <td><strong>豆包</strong> 视觉（如 doubao-seed-2-0-lite）</td>
            <td><strong>10 次 / 用户</strong></td>
          </tr>
          <tr>
            <td>AI 食谱配图（文生图）</td>
            <td><strong>豆包 Seedream 5.0 Lite</strong>（doubao-seedream-5-0-lite）</td>
            <td><strong>10 次 / 用户</strong></td>
          </tr>
        </tbody>
      </table>
      <div class="callout info">
        已移除 TheMealDB / Pexels 搜图方案。食谱配图改为豆包<strong>直接生成</strong>美食图片，效果更稳定。
      </div>
    </section>

    <section id="trial" class="guide-section">
      <h2>3. 试用额度与自备 Key</h2>
      <div class="callout warn">
        <strong>拍照识食</strong>或<strong>食谱配图</strong>试用各 10 次用完后，平台豆包 Key 将不可用。
        请在右上角 <strong>「API 状态」→ AI 设置</strong> 填写您自己的豆包 Key，或查看本手册配置说明。
      </div>
      <ul>
        <li><strong>DeepSeek 对话</strong>：平台代付<strong>不限次</strong>（仍建议长期使用时自备 Key）</li>
        <li><strong>豆包拍照</strong>：10 次 / 用户</li>
        <li><strong>豆包食谱生图</strong>：10 次 / 用户（与拍照分开计数）</li>
        <li>自备 Key 后：对应功能不再扣减平台额度</li>
      </ul>
    </section>

    <section id="ai-settings" class="guide-section">
      <h2>4. AI 设置（右上角「API 状态」）</h2>
      <h3>4.1 文本对话（默认 DeepSeek）</h3>
      <ul>
        <li>新用户默认服务商：<strong>DeepSeek</strong></li>
        <li>可改为通义、豆包、本地 LM Studio 等</li>
        <li>填写对应厂商 API Key 即可覆盖平台代付</li>
      </ul>
      <h3>4.2 拍照识食（独立配置）</h3>
      <ul>
        <li>推荐视觉服务商：<strong>豆包</strong></li>
        <li>视觉模型：填火山方舟<strong>推理接入点</strong>，如 <code>ep-20260529143011-9jk6q</code></li>
        <li>拍照 API Key：火山方舟 ARK Key（可与食谱生图共用同一 Key）</li>
        <li><strong>无需配置 base_url</strong>，系统已内置官方 endpoint</li>
      </ul>
      <h3>4.3 食谱配图</h3>
      <p>由后端使用豆包 Seedream 模型（如 <code>doubao-seedream-5-0-lite-260128</code>）自动生成，配置在服务器 <code>DOUBAO_IMAGE_MODEL</code>。若使用 <code>ep-</code> 接入点，须为控制台「图片生成」类模型。</p>
    </section>

    <section id="modules" class="guide-section">
      <h2>5. 各功能模块</h2>
      <article class="module-card">
        <h3>💬 AI 精灵 / 营养咨询</h3>
        <p>平台 <strong>DeepSeek</strong>，不限次。分析、建议、对话均走文本模型。</p>
      </article>
      <article class="module-card">
        <h3>📔 饮食日记</h3>
        <ul>
          <li>手动记录：无 AI</li>
          <li>文字识食 / AI 分析：<strong>DeepSeek</strong></li>
          <li>拍照上传：<strong>豆包视觉</strong>（10 次试用）</li>
        </ul>
      </article>
      <article class="module-card">
        <h3>🏋️ 健身记录</h3>
        <p>AI 建议与分析：<strong>DeepSeek</strong>。</p>
      </article>
      <article class="module-card">
        <h3>🍳 AI 食谱</h3>
        <p>文案：<strong>DeepSeek</strong>；配图：<strong>豆包 Seedream 生图</strong>（10 次试用，非搜图）。</p>
      </article>
    </section>

    <section id="faq" class="guide-section">
      <h2>6. 常见问题</h2>
      <dl>
        <dt>提示「平台试用额度已用完」？</dt>
        <dd>拍照或食谱配图 10 次已用尽。请在 AI 设置填写豆包 Key，或等待管理员调整配额。</dd>
        <dt>豆包要填 base_url 吗？</dt>
        <dd>不需要。系统使用 <code>https://ark.cn-beijing.volces.com/api/v3</code>。</dd>
        <dt>DeepSeek 和豆包 Key 都要配吗？</dt>
        <dd>试用阶段不必，平台已代付。长期或超额后：对话配 DeepSeek Key，识图/食谱配图配豆包 Key。</dd>
        <dt>Seedance 能识图吗？</dt>
        <dd>不能。Seedance 是视频生成；识图用 Seed 2.0 Lite，食谱配图用 Seedream。</dd>
      </dl>
    </section>

    <footer class="guide-footer">
      <router-link to="/" class="btn-primary">开始使用</router-link>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.guide-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  line-height: 1.65;
  color: #333;
}
.guide-header {
  margin-bottom: 24px;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 16px;
}
.guide-header h1 { margin: 0 0 8px; font-size: 1.75rem; color: #1a1a2e; }
.subtitle { color: #666; margin: 0 0 12px; }
.back-link { color: #007bff; font-size: 14px; }
.guide-nav {
  display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 28px;
  padding: 12px; background: #f8f9fa; border-radius: 8px;
}
.guide-nav a { color: #0056b3; font-size: 14px; text-decoration: none; }
.guide-section { margin-bottom: 32px; }
.guide-section h2 {
  font-size: 1.35rem; color: #16213e;
  border-left: 4px solid #007bff; padding-left: 12px; margin-bottom: 16px;
}
.guide-section h3 { font-size: 1.05rem; margin: 20px 0 8px; }
.module-card {
  background: #fff; border: 1px solid #e9ecef;
  border-radius: 8px; padding: 16px; margin-bottom: 12px;
}
.callout { padding: 12px 16px; border-radius: 8px; margin: 16px 0; }
.callout.warn { background: #fff3cd; border: 1px solid #ffc107; color: #856404; }
.callout.info { background: #e8f4fd; border: 1px solid #b8daff; color: #004085; }
.guide-table { width: 100%; border-collapse: collapse; font-size: 14px; margin: 12px 0; }
.guide-table th, .guide-table td { border: 1px solid #dee2e6; padding: 8px 12px; text-align: left; }
.guide-table th { background: #f8f9fa; }
code { background: #f1f3f5; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
dl dt { font-weight: 600; margin-top: 12px; }
dl dd { margin: 4px 0 0 16px; color: #555; }
.guide-footer { margin-top: 40px; padding-top: 24px; border-top: 1px solid #e9ecef; }
.btn-primary {
  background: #007bff; color: #fff; padding: 10px 24px;
  border-radius: 6px; text-decoration: none; font-weight: 500;
}
</style>
