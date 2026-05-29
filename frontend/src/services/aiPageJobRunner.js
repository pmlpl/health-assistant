// 页面 AI 任务执行器：请求与路由解耦，不在 onUnmounted 中取消
import { apiClient } from '../api/healthApi';
import { useAiPageJobsStore } from '../stores/aiPageJobsStore';
import { AI_PAGE_JOB_IDS, AI_PAGE_JOB_LABELS } from '../constants/aiPageJobIds';
import router from '../router';
import { ElNotification } from 'element-plus';
import { messageFromError } from '../utils/notify';

const LONG_TIMEOUT = 600000;
const controllers = new Map();

function cancelJob(jobId) {
    const ctrl = controllers.get(jobId);
    if (ctrl) {
        ctrl.abort();
        controllers.delete(jobId);
    }
}

async function runJob(jobId, route, requestFn) {
    const store = useAiPageJobsStore();
    cancelJob(jobId);
    const controller = new AbortController();
    controllers.set(jobId, controller);
    store.setRunning(jobId, route);

    try {
        const result = await requestFn(controller.signal);
        store.setSuccess(jobId, result);
        notifyIfAway(jobId, route, 'success');
        return result;
    } catch (err) {
        if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
            store.reset(jobId);
            return null;
        }
        store.setError(jobId, err);
        notifyIfAway(jobId, route, 'error', err);
        throw err;
    } finally {
        controllers.delete(jobId);
    }
}

function notifyIfAway(jobId, route, type, err) {
    const current = router.currentRoute.value.path;
    if (current === route) {
        return;
    }
    const label = AI_PAGE_JOB_LABELS[jobId] || 'AI 分析';
    if (type === 'success') {
        ElNotification({
            title: '✅ AI 已完成',
            message: `${label} 已完成，点击返回 ${route === '/diary' ? '饮食日记' : '健身记录'} 查看`,
            type: 'success',
            duration: 5000,
            offset: 80,
            onClick: () => router.push(route),
        });
    } else {
        ElNotification({
            title: '❌ AI 失败',
            message: `${label}：${messageFromError(err, '分析失败')}`,
            type: 'error',
            duration: 5000,
            offset: 80,
        });
    }
}

export const aiPageJobRunner = {
    cancel(jobId) {
        cancelJob(jobId);
    },

    cancelAll() {
        controllers.forEach((c) => c.abort());
        controllers.clear();
        useAiPageJobsStore().clearAll();
    },

    runDiarySmartAnalyze(foodDescription) {
        return runJob(AI_PAGE_JOB_IDS.DIARY_SMART_ANALYZE, '/diary', (signal) =>
            apiClient
                .post(
                    '/diet/smart-analyze',
                    { foodDescription },
                    { timeout: LONG_TIMEOUT, signal, headers: { 'Content-Type': 'application/json' } }
                )
                .then((r) => r.data)
        );
    },

    runDiaryNutritionAnalyze(userId, nutritionData) {
        return runJob(AI_PAGE_JOB_IDS.DIARY_NUTRITION_ANALYZE, '/diary', (signal) =>
            apiClient
                .post(`/diet/analyze-nutrition/${userId}`, nutritionData, {
                    timeout: LONG_TIMEOUT,
                    signal,
                    headers: { 'Content-Type': 'application/json' },
                })
                .then((r) => r.data)
        );
    },

    runDiaryImageRecognize(formData) {
        return runJob(AI_PAGE_JOB_IDS.DIARY_IMAGE_RECOGNIZE, '/diary', (signal) =>
            apiClient
                .post('/image/recognize', formData, {
                    timeout: LONG_TIMEOUT,
                    signal,
                    headers: { 'Content-Type': 'multipart/form-data' },
                })
                .then((r) => r.data)
        );
    },

    runFitnessWorkoutAnalyze(userId, fitnessAnalysisData) {
        return runJob(AI_PAGE_JOB_IDS.FITNESS_WORKOUT_ANALYZE, '/fitness', (signal) =>
            apiClient
                .post(`/ai/analyze-fitness-workout/${userId}`, fitnessAnalysisData, {
                    timeout: LONG_TIMEOUT,
                    signal,
                    headers: { 'Content-Type': 'application/json' },
                })
                .then((r) => r.data)
        );
    },
};
