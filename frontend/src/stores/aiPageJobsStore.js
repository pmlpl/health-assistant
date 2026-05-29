// 饮食日记 / 健身页 AI 任务全局状态（切路由不丢失）
import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { AI_PAGE_JOB_IDS } from '../constants/aiPageJobIds';

function emptyJob() {
    return {
        status: 'idle',
        result: null,
        error: null,
        startedAt: null,
        finishedAt: null,
        route: null,
    };
}

export const useAiPageJobsStore = defineStore('aiPageJobs', () => {
    const jobs = ref({
        [AI_PAGE_JOB_IDS.DIARY_SMART_ANALYZE]: emptyJob(),
        [AI_PAGE_JOB_IDS.DIARY_NUTRITION_ANALYZE]: emptyJob(),
        [AI_PAGE_JOB_IDS.DIARY_IMAGE_RECOGNIZE]: emptyJob(),
        [AI_PAGE_JOB_IDS.FITNESS_WORKOUT_ANALYZE]: emptyJob(),
    });

    const isRunning = (jobId) => jobs.value[jobId]?.status === 'running';

    const anyRunning = computed(() =>
        Object.values(jobs.value).some((j) => j.status === 'running')
    );

    const runningJobs = computed(() =>
        Object.entries(jobs.value)
            .filter(([, j]) => j.status === 'running')
            .map(([id]) => id)
    );

    function setRunning(jobId, route) {
        jobs.value[jobId] = {
            ...emptyJob(),
            status: 'running',
            startedAt: Date.now(),
            route,
        };
    }

    function setSuccess(jobId, result) {
        const prev = jobs.value[jobId] || emptyJob();
        jobs.value[jobId] = {
            ...prev,
            status: 'success',
            result,
            error: null,
            finishedAt: Date.now(),
        };
    }

    function setError(jobId, error) {
        const prev = jobs.value[jobId] || emptyJob();
        jobs.value[jobId] = {
            ...prev,
            status: 'error',
            error: error?.message || String(error),
            finishedAt: Date.now(),
        };
    }

    function reset(jobId) {
        jobs.value[jobId] = emptyJob();
    }

    function clearAll() {
        Object.keys(jobs.value).forEach((id) => {
            jobs.value[id] = emptyJob();
        });
    }

    return {
        jobs,
        isRunning,
        anyRunning,
        runningJobs,
        setRunning,
        setSuccess,
        setError,
        reset,
        clearAll,
    };
});
