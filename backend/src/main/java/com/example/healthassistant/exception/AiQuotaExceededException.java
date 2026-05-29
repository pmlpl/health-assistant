package com.example.healthassistant.exception;

/**
 * 平台试用配额用尽，引导用户自备 API Key。
 */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(String message) {
        super(message);
    }
}
