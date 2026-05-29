import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * 提交问卷到 AI 分析
 */
export async function submitQuestionnaireToAI(userId, questionnaireType, answers, totalScore) {
  try {
    const response = await axios.post(`${API_BASE_URL}/ai/analyze-questionnaire`, {
      userId,
      questionnaireType,
      answers,
      totalScore
    });

    if (response.data.success) {
      return response.data.report;
    } else {
      throw new Error(response.data.error || '问卷分析失败');
    }
  } catch (error) {
    console.error('问卷分析请求失败:', error);
    throw error;
  }
}
