package com.example.healthassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户 API Key AES-GCM 加解密。
 */
@Service
public class AiSettingsEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AiSettingsEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] keyBytes;

    public AiSettingsEncryptionService(
            @Value("${ai.settings.encryption-secret:}") String secret,
            @Value("${jwt.secret:dev-only-change-me-before-production}") String jwtFallback) {
        String material = (secret != null && secret.length() >= 16) ? secret : jwtFallback + "-ai-settings";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.keyBytes = digest.digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化 AI 设置加密", e);
        }
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("加密 API Key 失败", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密 API Key 失败", e);
        }
    }

    /** 解密失败时返回 null，避免因密钥轮换导致整个设置接口 500 */
    public String decryptSafe(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return decrypt(encoded);
        } catch (Exception e) {
            log.warn("API Key 解密失败（可能更换了 ai.settings.encryption-secret）: {}", e.getMessage());
            return null;
        }
    }

    public static String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return key == null ? null : "****";
        }
        return key.substring(0, 4) + "…" + key.substring(key.length() - 4);
    }
}
