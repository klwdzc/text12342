package com.book.backend.service.chat;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 智能对话上下文管理服务
 * 
 * 功能：
 * 1. 多轮对话上下文维护
 * 2. 用户意图识别
 * 3. 对话状态跟踪
 * 4. 智能回复生成
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class IntelligentChatService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 缓存配置
    private static final String CHAT_CONTEXT_KEY = "chat:context:";
    private static final long CONTEXT_EXPIRE_MINUTES = 30L; // 上下文过期时间 30 分钟
    
    /**
     * 处理用户消息并生成智能回复
     * 
     * @param userId 用户 ID
     * @param message 用户消息
     * @return 智能回复
     */
    public ChatResponse processMessage(Long userId, String message) {
        try {
            log.info("[智能客服] 用户 {} 消息：{}", userId, message);
            
            // 1. 获取对话上下文
            ChatContext context = getChatContext(userId);
            
            // 2. 识别用户意图
            Intent intent = recognizeIntent(message, context);
            
            // 3. 更新对话状态
            updateChatState(context, message, intent);
            
            // 4. 生成回复
            ChatResponse response = generateResponse(intent, context);
            
            // 5. 更新上下文（添加最新对话）
            context.addDialogue(message, response.getReply());
            saveChatContext(userId, context);
            
            log.info("[智能客服] 回复：{}", response.getReply());
            
            return response;
            
        } catch (Exception e) {
            log.error("智能客服处理失败：{}", e.getMessage(), e);
            return createDefaultResponse();
        }
    }
    
    /**
     * 识别用户意图
     */
    private Intent recognizeIntent(String message, ChatContext context) {
        Intent intent = new Intent();
        intent.setOriginalMessage(message);
        
        String lowerMessage = message.toLowerCase();
        
        // 1. 图书相关意图
        if (containsKeywords(lowerMessage, Arrays.asList("推荐", "介绍", "有什么好书"))) {
            intent.setType(IntentType.BOOK_RECOMMENDATION);
            intent.setConfidence(0.9);
            
            // 提取偏好
            if (containsKeywords(lowerMessage, Arrays.asList("科幻", "小说"))) {
                intent.setPreference("科幻");
            } else if (containsKeywords(lowerMessage, Arrays.asList("技术", "编程", "计算机"))) {
                intent.setPreference("计算机");
            }
            
        } else if (containsKeywords(lowerMessage, Arrays.asList("借阅", "借书", "还书", "逾期"))) {
            intent.setType(IntentType.BORROW_QUERY);
            intent.setConfidence(0.85);
            
        } else if (containsKeywords(lowerMessage, Arrays.asList("规则", "规定", "能借多久", "可以借"))) {
            intent.setType(IntentType.RULE_QUERY);
            intent.setConfidence(0.8);
            
        } else if (containsKeywords(lowerMessage, Arrays.asList("密码", "登录", "账号"))) {
            intent.setType(IntentType.ACCOUNT_ISSUE);
            intent.setConfidence(0.75);
            
        } else if (containsKeywords(lowerMessage, Arrays.asList("谢谢", "感谢"))) {
            intent.setType(IntentType.THANKS);
            intent.setConfidence(0.95);
            
        } else if (containsKeywords(lowerMessage, Arrays.asList("你好", "您好", "在吗"))) {
            intent.setType(IntentType.GREETING);
            intent.setConfidence(0.95);
            
        } else {
            // 默认意图：通用聊天
            intent.setType(IntentType.CHAT);
            intent.setConfidence(0.6);
        }
        
        return intent;
    }
    
    /**
     * 生成回复
     */
    private ChatResponse generateResponse(Intent intent, ChatContext context) {
        ChatResponse response = new ChatResponse();
        response.setIntent(intent.getType());
        response.setConfidence(intent.getConfidence());
        
        switch (intent.getType()) {
            case BOOK_RECOMMENDATION:
                response.setReply(generateBookRecommendation(intent, context));
                break;
                
            case BORROW_QUERY:
                response.setReply(generateBorrowQueryResponse(intent, context));
                break;
                
            case RULE_QUERY:
                response.setReply(generateRuleQueryResponse(intent, context));
                break;
                
            case ACCOUNT_ISSUE:
                response.setReply(generateAccountIssueResponse(intent, context));
                break;
                
            case THANKS:
                response.setReply("不客气！很高兴能帮助您。如果还有其他问题，随时问我哦！😊");
                break;
                
            case GREETING:
                response.setReply("您好！我是图书馆智能助手，很高兴为您服务！请问有什么可以帮助您的？📚");
                break;
                
            case CHAT:
            default:
                response.setReply(generateChatResponse(intent, context));
                break;
        }
        
        // 添加建议操作
        response.setSuggestions(generateSuggestions(intent.getType()));
        
        return response;
    }
    
    /**
     * 生成图书推荐回复
     */
    private String generateBookRecommendation(Intent intent, ChatContext context) {
        StringBuilder reply = new StringBuilder();
        
        if (intent.getPreference() != null) {
            reply.append(String.format("根据您的兴趣，我为您推荐%s类图书：\n\n", intent.getPreference()));
            reply.append("1. 《深入理解》系列 - 经典之作，适合深入学习\n");
            reply.append("2. 《实战指南》系列 - 理论与实践相结合\n");
            reply.append("3. 《从入门到精通》系列 - 系统性学习首选\n\n");
            reply.append("您想了解哪一本的详细信息呢？或者告诉我您具体的需求，我可以更精准地推荐！");
        } else {
            reply.append("我很乐意为您推荐好书！📚\n\n");
            reply.append("为了给您更精准的推荐，请告诉我：\n");
            reply.append("• 您喜欢什么类型的书？（如：科幻、历史、技术）\n");
            reply.append("• 您是想学习新知识还是休闲阅读？\n");
            reply.append("• 有没有特别喜欢的作者？");
        }
        
        return reply.toString();
    }
    
    /**
     * 生成借阅查询回复
     */
    private String generateBorrowQueryResponse(Intent intent, ChatContext context) {
        StringBuilder reply = new StringBuilder();
        
        reply.append("关于借阅信息，您可以：\n\n");
        reply.append("📖 当前借阅：查看您正在借阅的图书\n");
        reply.append("📅 借阅历史：查看您的借阅记录\n");
        reply.append("⏰ 逾期提醒：查看是否有即将逾期的图书\n\n");
        reply.append("请告诉我您想查询哪方面的信息？");
        
        return reply.toString();
    }
    
    /**
     * 生成规则查询回复
     */
    private String generateRuleQueryResponse(Intent intent, ChatContext context) {
        StringBuilder reply = new StringBuilder();
        
        reply.append("📋 图书馆借阅规则：\n\n");
        reply.append("✅ 借阅数量：每位读者最多可借 10 本图书\n");
        reply.append("✅ 借阅期限：每本图书可借阅 30 天\n");
        reply.append("✅ 续借政策：可续借一次，延长 30 天\n");
        reply.append("✅ 逾期处理：逾期将产生违规记录，影响信用评分\n\n");
        reply.append("温馨提示：请按时归还图书，保持良好的借阅记录！");
        
        return reply.toString();
    }
    
    /**
     * 生成账号问题回复
     */
    private String generateAccountIssueResponse(Intent intent, ChatContext context) {
        StringBuilder reply = new StringBuilder();
        
        if (containsKeywords(intent.getOriginalMessage().toLowerCase(), Arrays.asList("密码", "修改"))) {
            reply.append("🔐 密码相关问题：\n\n");
            reply.append("如需重置密码，请联系图书馆管理员。\n");
            reply.append("联系方式：library@example.com\n");
            reply.append("工作时间：周一至周五 9:00-17:00");
        } else if (containsKeywords(intent.getOriginalMessage().toLowerCase(), Arrays.asList("登录"))) {
            reply.append("👤 登录帮助：\n\n");
            reply.append("请使用您的借阅证号和密码登录系统。\n");
            reply.append("如忘记密码，请联系管理员重置。");
        } else {
            reply.append("关于账号问题，我可以帮您解答：\n\n");
            reply.append("• 密码找回/重置\n");
            reply.append("• 登录问题\n");
            reply.append("• 账号信息修改\n\n");
            reply.append("请具体描述您遇到的问题。");
        }
        
        return reply.toString();
    }
    
    /**
     * 生成通用聊天回复
     */
    private String generateChatResponse(Intent intent, ChatContext context) {
        // 简单的上下文感知回复
        if (context.getDialogueCount() > 0) {
            return "嗯嗯，我明白了。作为图书馆智能助手，我主要擅长图书推荐、借阅查询等方面的咨询。如果您有相关的问题，我很乐意帮助您！📚";
        } else {
            return "您好！我是图书馆智能助手，可以为您解答图书相关问题，提供图书推荐服务等。请问有什么可以帮助您的？😊";
        }
    }
    
    /**
     * 生成建议操作列表
     */
    private List<String> generateSuggestions(IntentType intentType) {
        List<String> suggestions = new ArrayList<>();
        
        switch (intentType) {
            case BOOK_RECOMMENDATION:
                suggestions.add("查看热门推荐");
                suggestions.add("按分类浏览");
                suggestions.add("查看新书上架");
                break;
                
            case BORROW_QUERY:
                suggestions.add("查看当前借阅");
                suggestions.add("查看借阅历史");
                suggestions.add("预约图书");
                break;
                
            case RULE_QUERY:
                suggestions.add("借阅流程");
                suggestions.add("续借方法");
                suggestions.add("逾期处理");
                break;
                
            default:
                suggestions.add("图书推荐");
                suggestions.add("借阅查询");
                suggestions.add("常见问题");
                break;
        }
        
        return suggestions;
    }
    
    /**
     * 检查消息是否包含关键词
     */
    private boolean containsKeywords(String message, List<String> keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取对话上下文
     */
    private ChatContext getChatContext(Long userId) {
        String key = CHAT_CONTEXT_KEY + userId;
        ChatContext context = (ChatContext) redisTemplate.opsForValue().get(key);
        
        if (context == null) {
            context = new ChatContext();
            context.setUserId(userId);
        }
        
        return context;
    }
    
    /**
     * 保存对话上下文
     */
    private void saveChatContext(Long userId, ChatContext context) {
        String key = CHAT_CONTEXT_KEY + userId;
        redisTemplate.opsForValue().set(key, context, CONTEXT_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * 更新对话状态
     */
    private void updateChatState(ChatContext context, String message, Intent intent) {
        context.setCurrentIntent(intent.getType());
        context.setLastMessage(message);
        context.setLastUpdateTime(System.currentTimeMillis());
    }
    
    /**
     * 创建默认回复
     */
    private ChatResponse createDefaultResponse() {
        ChatResponse response = new ChatResponse();
        response.setReply("抱歉，我好像没太理解您的意思。😅 您可以问我关于图书推荐、借阅规则、账号管理等方面的问题，我会尽力帮助您！");
        response.setIntent(IntentType.CHAT);
        response.setConfidence(0.5);
        response.setSuggestions(Arrays.asList("图书推荐", "借阅查询", "常见问题"));
        return response;
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 对话上下文数据
     */
    @Data
    public static class ChatContext implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 用户 ID */
        private Long userId;
        
        /** 当前意图 */
        private IntentType currentIntent;
        
        /** 最后一条消息 */
        private String lastMessage;
        
        /** 对话历史（最近 10 条） */
        private List<DialoguePair> dialogueHistory = new ArrayList<>();
        
        /** 最后更新时间 */
        private Long lastUpdateTime = System.currentTimeMillis();
        
        /** 添加对话 */
        public void addDialogue(String userMessage, String botReply) {
            dialogueHistory.add(new DialoguePair(userMessage, botReply));
            
            // 保持最近 10 条
            if (dialogueHistory.size() > 10) {
                dialogueHistory.remove(0);
            }
        }
        
        /** 获取对话数量 */
        public int getDialogueCount() {
            return dialogueHistory.size();
        }
    }
    
    /**
     * 对话对
     */
    @Data
    public static class DialoguePair implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String userMessage;
        private final String botReply;
        
        public DialoguePair(String userMessage, String botReply) {
            this.userMessage = userMessage;
            this.botReply = botReply;
        }
    }
    
    /**
     * 用户意图
     */
    @Data
    public static class Intent implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 意图类型 */
        private IntentType type;
        
        /** 置信度 */
        private Double confidence;
        
        /** 原始消息 */
        private String originalMessage;
        
        /** 用户偏好（用于推荐） */
        private String preference;
    }
    
    /**
     * 意图类型枚举
     */
    public enum IntentType {
        BOOK_RECOMMENDATION,  // 图书推荐
        BORROW_QUERY,         // 借阅查询
        RULE_QUERY,           // 规则查询
        ACCOUNT_ISSUE,        // 账号问题
        THANKS,               // 感谢
        GREETING,             // 问候
        CHAT                  // 通用聊天
    }
    
    /**
     * 聊天回复
     */
    @Data
    public static class ChatResponse implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 回复内容 */
        private String reply;
        
        /** 识别的意图 */
        private IntentType intent;
        
        /** 置信度 */
        private Double confidence;
        
        /** 建议操作 */
        private List<String> suggestions;
    }
}
