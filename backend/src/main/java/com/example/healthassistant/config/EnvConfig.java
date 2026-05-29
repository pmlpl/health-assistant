package com.example.healthassistant.config;

import java.io.*;
import java.util.Properties;

/**
 * 环境变量配置类
 * 用于从 .env 文件加载环境变量
 */
public class EnvConfig {
    
    private static final Properties envProperties = new Properties();
    private static boolean loaded = false;
    
    static {
        loadEnvFile();
    }
    
    /**
     * 加载 .env 文件
     */
    private static void loadEnvFile() {
        if (loaded) {
            return;
        }
        
        try {
            // 尝试从 classpath 加载 .env 文件
            InputStream inputStream = EnvConfig.class.getClassLoader().getResourceAsStream(".env");
            
            if (inputStream == null) {
                // 尝试从项目根目录加载
                File envFile = new File("backend/.env");
                if (envFile.exists()) {
                    inputStream = new FileInputStream(envFile);
                }
            }
            
            if (inputStream != null) {
                envProperties.load(inputStream);
                loaded = true;
            } else {
                // 未找到 .env 文件，将使用系统环境变量
            }
        } catch (IOException e) {
            // 加载失败，使用系统环境变量
        }
    }
    
    /**
     * 获取指定环境变量
     * @param key 变量名
     * @return 变量值
     */
    public static String get(String key) {
        // 先从 .env 文件加载
        String value = envProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // 其次从系统环境变量获取
        return System.getenv(key);
    }
    
    /**
     * 获取通义千问 API Key
     * @return API Key
     */
    public static String getDashscopeApiKey() {
        return get("DASHSCOPE_API_KEY");
    }
    
    /**
     * 获取豆包 AI API Key
     * @return API Key
     */
    public static String getDoubaoApiKey() {
        return get("DOUBAO_API_KEY");
    }
    
    /**
     * 检查是否已加载 .env 文件
     * @return true 如果已加载
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
