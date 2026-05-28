import { apiClient } from './healthApi';

/** 提交问卷给 AI 分析（后端无专用接口时使用心理健康咨询接口） */
export async function submitQuestionnaireToAI(userId, questionnaireLabel, answersData, totalScore) {
    const summary = answersData
        .map((item, i) => `${i + 1}. ${item.question}\n   回答：${item.answer}（${item.score}分）`)
        .join('\n');

    const message = `请根据以下「${questionnaireLabel}」问卷结果给出专业、温和的分析与建议（总分 ${totalScore}）：\n\n${summary}`;

    try {
        const response = await apiClient.post('/ai/mental-health', {
            userId,
            message,
        }, { timeout: 600000 });

        const data = response.data;
        if (typeof data === 'string') return data;
        if (data?.response) return data.response;
        if (data?.advice) return data.advice;
        return data?.message || '分析完成，请查看详细建议。';
    } catch (error) {
        console.error('问卷 AI 分析失败:', error);
        throw error;
    }
}

export const questionnaireApi = {
    submitQuestionnaireToAI,
};
