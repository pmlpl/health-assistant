// ============================================================
// 页面表单草稿：sessionStorage 持久化，切路由返回后不丢未保存内容
// ============================================================

/**
 * 保存页面草稿
 * @param {string} key - 存储键（如 diary_form_draft）
 * @param {object} payload - 要序列化的数据
 */
export function savePageDraft(key, payload) {
    try {
        sessionStorage.setItem(key, JSON.stringify({ ...payload, savedAt: Date.now() }));
    } catch (e) {
        console.warn('保存页面草稿失败:', e);
    }
}

/**
 * 读取页面草稿
 * @param {string} key
 * @returns {object|null}
 */
export function loadPageDraft(key) {
    try {
        const raw = sessionStorage.getItem(key);
        if (!raw) return null;
        return JSON.parse(raw);
    } catch (e) {
        console.warn('读取页面草稿失败:', e);
        return null;
    }
}

/**
 * 清除页面草稿
 * @param {string} key
 */
export function clearPageDraft(key) {
    try {
        sessionStorage.removeItem(key);
    } catch (e) {
        console.warn('清除页面草稿失败:', e);
    }
}
