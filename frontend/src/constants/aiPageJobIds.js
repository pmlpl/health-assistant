/** 饮食日记 / 健身记录页面的 AI 任务标识 */
export const AI_PAGE_JOB_IDS = {
    DIARY_SMART_ANALYZE: 'diary_smart_analyze',
    DIARY_NUTRITION_ANALYZE: 'diary_nutrition_analyze',
    DIARY_IMAGE_RECOGNIZE: 'diary_image_recognize',
    FITNESS_WORKOUT_ANALYZE: 'fitness_workout_analyze',
};

export const AI_PAGE_JOB_LABELS = {
    [AI_PAGE_JOB_IDS.DIARY_SMART_ANALYZE]: '饮食日记 · 智能分析',
    [AI_PAGE_JOB_IDS.DIARY_NUTRITION_ANALYZE]: '饮食日记 · 营养建议',
    [AI_PAGE_JOB_IDS.DIARY_IMAGE_RECOGNIZE]: '饮食日记 · 拍照识别',
    [AI_PAGE_JOB_IDS.FITNESS_WORKOUT_ANALYZE]: '健身记录 · AI 分析',
};
