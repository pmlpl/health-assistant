package com.example.healthassistant;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionContentPart;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// 请确保您已将 API Key 存储在环境变量 ARK_API_KEY 中
// 初始化Ark客户端，从环境变量中读取您的API Key
public class ChatCompletionsVisionExample {
    // 从环境变量中获取您的 API Key。此为默认方式，您可根据需要进行修改
    static String apiKey = System.getenv("ARK_API_KEY");
    // 此为默认路径，您可根据业务所在地域进行配置
    static String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    static ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
    static Dispatcher dispatcher = new Dispatcher();
    static ArkService service = ArkService.builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build();

    public static void main(String[] args) {
        // 检查API密钥是否存在
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误: 请设置环境变量 ARK_API_KEY");
            System.err.println("在Windows中使用: set ARK_API_KEY=your_api_key_here");
            System.err.println("在Linux/Mac中使用: export ARK_API_KEY=your_api_key_here");
            return;
        }

        final List<ChatMessage> messages = new ArrayList<>();
        final List<ChatCompletionContentPart> multiParts = new ArrayList<>();
        
        multiParts.add(ChatCompletionContentPart.builder()
                .type("image_url")
                .imageUrl(new ChatCompletionContentPart.ChatCompletionContentPartImageURL(
                    "https://ark-project.tos-cn-beijing.ivolces.com/images/view.jpeg"
                ))
                .build());
                
        multiParts.add(ChatCompletionContentPart.builder()
                .type("text")
                .text("这是哪里？")
                .build());

        final ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .multiContent(multiParts)
                .build();
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                // 指定您创建的方舟推理接入点 ID
                .model("doubao-seed-1-8-251228")
                .messages(messages)
                .build();

        try {
            // 根据项目中 DoubaoFoodRecognitionService 的实现，使用 createResponse 方法
            // service.createResponse(chatCompletionRequest);
                    
            // 或者如果 SDK 支持 createChatCompletion，使用这个方法
            service.createChatCompletion(chatCompletionRequest)
                   .getChoices()
                   .forEach(choice -> 
                       System.out.println(choice.getMessage().getContent()));
                    
        } catch (Exception e) {
            
            // 提供调试信息
            System.err.println("请检查:");
            System.err.println("1. API密钥是否正确设置: " + (apiKey != null ? "已设置" : "未设置"));
            System.err.println("2. 网络连接是否正常");
            System.err.println("3. 模型名称是否正确: doubao-seed-1-8-251228");
            System.err.println("4. SDK版本是否兼容");
        } finally {
            service.shutdownExecutor();
        }
    }
}