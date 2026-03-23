package com.book.backend.manager;

import cn.hutool.core.date.StopWatch;
import com.aliyun.broadscope.bailian.sdk.AccessTokenClient;
import com.aliyun.broadscope.bailian.sdk.ApplicationClient;
import com.aliyun.broadscope.bailian.sdk.models.BaiLianConfig;
import com.aliyun.broadscope.bailian.sdk.models.CompletionsRequest;
import com.aliyun.broadscope.bailian.sdk.models.CompletionsResponse;
import com.aliyun.broadscope.bailian.sdk.utils.UUIDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 阿里AI模型 通义千问-Plus 工具类
 *
 * @author <a href="https://github.com/luoye6">小白条
 * @from <a href="https://luoye6.github.io/"> 个人博客
 */
@Service
@Slf4j
public class AlibabaAIModel {
    
    // 从配置文件读取密钥信息
    @Value("${aliyun.ai.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aliyun.ai.access-key-secret:}")
    private String accessKeySecret;
    
    @Value("${aliyun.ai.agent-key:}")
    private String agentKey;
    
    @Value("${aliyun.ai.app-id:}")
    private String appId;
    /**
     * 阿里云百炼也支持调用侧自己维护上下文对话历史, 同时传入sessionId和history，会优先采用调用侧传入的上下文历史
     * CompletionsRequest.ChatQaPair chatQaPair = new CompletionsRequest.ChatQaPair("我想去北京", "北京的天气很不错");
     * CompletionsRequest.ChatQaPair chatQaPair2 = new CompletionsRequest.ChatQaPair("北京有哪些景点", "北京有故宫、长城等");
     * history.add(chatQaPair);
     * history.add(chatQaPair2);
     * request.setHistory(history);
     */
    public static final List<CompletionsRequest.ChatQaPair> history = new ArrayList<>();

    public String doChatOnce(String prompt) {
        log.info("进入 AI 对话");
        AccessTokenClient accessTokenClient = new AccessTokenClient(accessKeyId, accessKeySecret, agentKey);
        String token = accessTokenClient.getToken();
    
        BaiLianConfig config = new BaiLianConfig()
                .setApiKey(token);
        CompletionsRequest request = new CompletionsRequest()
                .setAppId(appId)
                .setPrompt(prompt);
    
        ApplicationClient client = new ApplicationClient(config);
        CompletionsResponse response = client.completions(request);
        log.info("AI 对话结束");
        return response.getData().getText();
    }
    
    public String doChatWithHistory(String prompt, List<String[]> recentHistory) {
        log.info("进入 AI 对话");
        AccessTokenClient accessTokenClient = new AccessTokenClient(accessKeyId, accessKeySecret, agentKey);
        String token = accessTokenClient.getToken();

        BaiLianConfig config = new BaiLianConfig()
                .setApiKey(token);
        // 通过sessionId 判断是否为同一个用户
        String sessionId = UUIDGenerator.generate();
        // 将该用户最近的五条历史记录加入到绘画中
        recentHistory.forEach(item -> {
            CompletionsRequest.ChatQaPair chatQaPair = new CompletionsRequest.ChatQaPair(item[0], item[1]);
            history.add(chatQaPair);
        });

        CompletionsRequest request = new CompletionsRequest()
                .setAppId(appId)
                .setPrompt(prompt)
                .setSessionId(sessionId)
                .setHistory(history);
        ApplicationClient client = new ApplicationClient(config);
        CompletionsResponse response = client.completions(request);
        log.info("AI对话结束");
        return response.getData().getText();
    }
}
