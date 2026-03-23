package com.book.backend.service.impl;

import cn.hutool.core.date.StopWatch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.backend.common.R;
import com.book.backend.common.exception.VueBookException;
import com.book.backend.manager.GuavaRateLimiterManager;
import com.book.backend.manager.SparkClient;
import com.book.backend.manager.model.SparkMessage;
import com.book.backend.manager.model.SparkSyncChatResponse;
import com.book.backend.manager.model.request.SparkRequest;
import com.book.backend.manager.model.response.SparkTextUsage;
import com.book.backend.mapper.AiIntelligentMapper;
import com.book.backend.pojo.AiIntelligent;
import com.book.backend.pojo.Books;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.UserInterfaceInfo;
import com.book.backend.service.AiIntelligentService;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.BooksService;
import com.book.backend.service.UsersService;
import com.book.backend.service.recommendation.ContentBasedRecommendationService;
import com.book.backend.service.recommendation.HybridRecommendationEngine;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xiaobaitiao
 * @description 针对表【t_ai_intelligent】的数据库操作Service实现
 * @createDate 2023-08-27 18:44:26
 */
@Service
public class AiIntelligentServiceImpl extends ServiceImpl<AiIntelligentMapper, AiIntelligent>
        implements AiIntelligentService {

    @Resource
    private BooksService booksService;
    @Resource
    private GuavaRateLimiterManager guavaRateLimiterManager;
    @Resource
    private UserInterfaceInfoServiceImpl userInterfaceInfoService;
    @Resource
    @Lazy
    private AiIntelligentService aiIntelligentService;
    @Resource
    private UsersService usersService;
    @Resource
    private BooksBorrowService booksBorrowService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ContentBasedRecommendationService contentBasedRecommendationService;
    @Resource
    private HybridRecommendationEngine hybridRecommendationEngine;
    
    // Redis 缓存 Key 定义
    private static final String USER_PREFERENCE_CACHE_KEY = "ai:user:preference:";
    private static final String CANDIDATE_BOOKS_CACHE_KEY = "ai:books:candidate:";
    private static final long USER_PREFERENCE_EXPIRE_MINUTES = 30L; // 用户偏好缓存 30 分钟
    private static final long CANDIDATE_BOOKS_EXPIRE_MINUTES = 60L; // 候选图书缓存 60 分钟
    
    /**
     * 客户端实例，线程安全
     */
    SparkClient sparkClient = new SparkClient();
    
    // 从配置文件读取讯飞星火认证信息
    @Value("${spark.ai.appid:4868e724}")
    private String sparkAppid;
    
    @Value("${spark.ai.apiKey:963b7be2ea6fa93481f209ab9e337d4e}")
    private String sparkApiKey;
    
    @Value("${spark.ai.apiSecret:YjVlNmI1Y2IxNjJiZmMyNzdiYjBhZDRm}")
    private String sparkApiSecret;
    
    /**
     * 初始化 Spark AI 配置
     */
    @PostConstruct
    public void initSparkConfig() {
        sparkClient.appid = sparkAppid;
        sparkClient.apiKey = sparkApiKey;
        sparkClient.apiSecret = sparkApiSecret;
        System.out.println("Spark AI 配置已加载 (AppID: " + sparkAppid.substring(0, Math.min(4, sparkAppid.length())) + "***)");
    }
    @Override
    public R<String> getGenResult(AiIntelligent aiIntelligent) {
        // 判断用户输入文本是否过长，超过128字，直接返回，防止资源耗尽
        String message = aiIntelligent.getInputMessage();
        if(StringUtils.isBlank(message)){
            return R.error("文本不能为空");
        }
        if (message.length() > 128) {
            return R.error("文本字数过长");
        }
        Long user_id = aiIntelligent.getUserId();
        // 查看用户接口次数是否足够，如果不够直接返回接口次数不够
        LambdaQueryWrapper<UserInterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInterfaceInfo::getUserId, user_id);
        queryWrapper.eq(UserInterfaceInfo::getInterfaceId, 1);
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.getOne(queryWrapper);
        if (userInterfaceInfo == null) {
            return R.error("该接口已废弃");
        }
        Integer leftNum = userInterfaceInfo.getLeftNum();
        Integer totalNum = userInterfaceInfo.getTotalNum();
        if (leftNum <= 0) {
            return R.error("AI接口次数不足，请明天再来");
        }
        // 限流
        boolean rateLimit = guavaRateLimiterManager.doRateLimit(user_id);
        if (!rateLimit) {
            return R.error("请求次数过多，请稍后重试");
        }
        
        // 【优化 1】获取用户借阅历史，分析偏好
        List<String> preferredTypes = getUserPreferredBookTypes(user_id);
        
        // 【优化 2】根据用户偏好查询相关图书，而不是全表查询
        List<Books> candidateBooks = getCandidateBooksByPreferences(preferredTypes, 50);
        
        // 【优化 3】构建优化的 Prompt
        StringBuilder promptBuilder = buildOptimizedPrompt(message, candidateBooks, preferredTypes);

        // 发送请求给AI，进行对话 由讯飞星火模型切换为阿里AI模型
        // 超时判断 利用ExecutorService
//
        // 调用之前先获取该用户最近的五条历史记录
        R<List<AiIntelligent>> history = aiIntelligentService.getAiInformationByUserId(user_id);
        List<AiIntelligent> historyData = history.getData();
        String response;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages = new ArrayList<>();
        historyData.forEach(item->{
            messages.add(SparkMessage.userContent(item.getInputMessage()));
            messages.add(SparkMessage.assistantContent(item.getAiResult()));
        });
        messages.add(SparkMessage.userContent(promptBuilder.toString()));
        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传，默认为2048。
                // V1.5取值为[1,4096]
                // V2.0取值为[1,8192]
                // V3.0取值为[1,8192]
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                .build();
        int timeout = 25; // 超时时间，单位为秒
        Future<String> future = executor.submit(() -> {
            try {
                // 同步调用
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
                SparkTextUsage textUsage = chatResponse.getTextUsage();
                stopWatch.stop();
                long total = stopWatch.getTotal(TimeUnit.SECONDS);
                System.out.println("本次接口调用耗时:"+total+"秒");
                System.out.println("\n回答：" + chatResponse.getContent());
                System.out.println("\n提问tokens：" + textUsage.getPromptTokens()
                        + "，回答tokens：" + textUsage.getCompletionTokens()
                        + "，总消耗tokens：" + textUsage.getTotalTokens());
                return chatResponse.getContent();
//                return AlibabaAIModel.doChatWithHistory(stringBuilder.toString(),recentHistory);
            } catch (Exception exception) {
                throw new RuntimeException("遇到异常");
            }
        });

        try {
            response = future.get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            return R.error("服务器内部错误，请求超时，请稍后重试");
        }
//        // 关闭ExecutorService
        executor.shutdown();

        // 得到消息
//        System.out.println(response);
        AiIntelligent saveResult = new AiIntelligent();
        saveResult.setInputMessage(aiIntelligent.getInputMessage());
        saveResult.setAiResult(response);
        saveResult.setUserId(user_id);
        boolean save = this.save(saveResult);
        if (!save) {
            throw new VueBookException("获取AI推荐信息失败");
        }
        // 更新调用接口的次数 剩余接口调用次数-1.总共调用次数+1
        userInterfaceInfo.setLeftNum(leftNum - 1);
        userInterfaceInfo.setTotalNum(totalNum + 1);
        boolean update = userInterfaceInfoService.updateById(userInterfaceInfo);
        if (!update) {
            return R.error("调用接口信息失败");
        }
        return R.success(response, "获取AI推荐信息成功");
    }

    @Override
    public R<List<AiIntelligent>> getAiInformationByUserId(Long userId) {
        QueryWrapper<AiIntelligent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != null && userId > 0, "user_id", userId);
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT 5");
        List<AiIntelligent> list = this.list(queryWrapper);
        if (list.size() == 0) {
            return R.success(null, "用户暂时没有和AI的聊天记录");
        }
        Collections.reverse(list);
        return R.success(list, "获取和AI最近的5条聊天记录成功");
    }
    public static int getSleepTimeStrategy(String message){
        int length = message.length();
        if(length<20){
            return 10;
        }else if(length<=30){
            return 12;
        }else if(length<=50){
            return 15;
        }else{
            return 20;
        }
    }
    
    /**
     * 【优化方法 1】获取用户偏好的图书分类（带 Redis 缓存）
     * @param userId 用户 ID
     * @return 偏好的图书分类列表
     */
    private List<String> getUserPreferredBookTypes(Long userId) {
        try {
            // 1. 先从 Redis 缓存中获取
            String cacheKey = USER_PREFERENCE_CACHE_KEY + userId;
            List<String> cachedPreferences = (List<String>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedPreferences != null && !cachedPreferences.isEmpty()) {
                System.out.println("[缓存命中] 用户 " + userId + " 的偏好从 Redis 获取");
                return cachedPreferences;
            }
            
            System.out.println("[缓存未命中] 用户 " + userId + " 的偏好从数据库计算");
            
            // 2. 缓存未命中，从数据库查询
            // 查询用户的借阅历史
            com.book.backend.pojo.Users user = usersService.getById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            
            Long cardNumber = user.getCardNumber();
            
            // 查询该用户的所有借阅记录
            LambdaQueryWrapper<BooksBorrow> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BooksBorrow::getCardNumber, cardNumber);
            List<BooksBorrow> borrows = booksBorrowService.list(borrowWrapper);
            
            if (borrows.isEmpty()) {
                List<String> empty = new ArrayList<>();
                // 新用户也缓存，避免频繁查询
                redisTemplate.opsForValue().set(cacheKey, empty, USER_PREFERENCE_EXPIRE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
                return empty;
            }
            
            // 统计各分类的借阅次数（优化：批量查询图书，避免 N+1 问题）
            List<Long> bookNumbers = borrows.stream()
                .map(BooksBorrow::getBookNumber)
                .distinct()
                .collect(Collectors.toList());
            
            // 一次性查询所有图书
            List<Books> allBooks = booksService.list(
                new LambdaQueryWrapper<Books>().in(Books::getBookNumber, bookNumbers)
            );
            
            // 创建图书编号到图书对象的映射
            Map<Long, Books> bookMap = allBooks.stream()
                .collect(Collectors.toMap(Books::getBookNumber, book -> book));
            
            // 使用映射统计分类
            Map<String, Long> typeCountMap = borrows.stream()
                .map(BooksBorrow::getBookNumber)
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                    Books::getBookType,
                    Collectors.counting()
                ));
            
            // 返回借阅次数最多的前 3 个分类
            List<String> preferredTypes = typeCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            // 3. 写入 Redis 缓存
            if (!preferredTypes.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, preferredTypes, USER_PREFERENCE_EXPIRE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            }
            
            return preferredTypes;
        } catch (Exception e) {
            System.err.println("获取用户偏好失败：" + e.getMessage());
            // Redis 异常时降级，直接查数据库
            return getUserPreferredBookTypesFromDB(userId);
        }
    }
    
    /**
     * 降级方法：Redis 不可用时直接从数据库获取
     */
    private List<String> getUserPreferredBookTypesFromDB(Long userId) {
        try {
            com.book.backend.pojo.Users user = usersService.getById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            
            Long cardNumber = user.getCardNumber();
            LambdaQueryWrapper<BooksBorrow> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BooksBorrow::getCardNumber, cardNumber);
            List<BooksBorrow> borrows = booksBorrowService.list(borrowWrapper);
            
            if (borrows.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 统计各分类的借阅次数（优化：批量查询图书，避免 N+1 问题）
            List<Long> bookNumbers = borrows.stream()
                .map(BooksBorrow::getBookNumber)
                .distinct()
                .collect(Collectors.toList());
            
            // 一次性查询所有图书
            List<Books> allBooks = booksService.list(
                new LambdaQueryWrapper<Books>().in(Books::getBookNumber, bookNumbers)
            );
            
            // 创建图书编号到图书对象的映射
            Map<Long, Books> bookMap = allBooks.stream()
                .collect(Collectors.toMap(Books::getBookNumber, book -> book));
            
            // 使用映射统计分类
            Map<String, Long> typeCountMap = borrows.stream()
                .map(BooksBorrow::getBookNumber)
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                    Books::getBookType,
                    Collectors.counting()
                ));
            
            return typeCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 【优化方法 2】根据用户偏好查询候选图书（带 Redis 缓存）
     * @param preferredTypes 偏好的图书分类
     * @param limit 返回数量限制
     * @return 候选图书列表
     */
    private List<Books> getCandidateBooksByPreferences(List<String> preferredTypes, Integer limit) {
        try {
            if (preferredTypes.isEmpty()) {
                // 如果没有偏好，返回热门图书（借阅次数最多的）
                return booksService.list(new LambdaQueryWrapper<Books>().last("LIMIT " + limit));
            }
            
            // 1. 构建缓存 Key（基于偏好分类的哈希）
            String cacheKey = CANDIDATE_BOOKS_CACHE_KEY + String.join(",", preferredTypes);
            List<Books> cachedBooks = (List<Books>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedBooks != null && !cachedBooks.isEmpty()) {
                System.out.println("[缓存命中] 候选图书从 Redis 获取，分类：" + String.join(", ", preferredTypes));
                return cachedBooks;
            }
            
            System.out.println("[缓存未命中] 候选图书从数据库查询，分类：" + String.join(", ", preferredTypes));
            
            // 2. 缓存未命中，从数据库查询
            // 根据偏好分类查询图书
            LambdaQueryWrapper<Books> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Books::getBookType, preferredTypes)
                       .orderByDesc(Books::getCreateTime)
                       .last("LIMIT " + limit);
            
            List<Books> candidateBooks = booksService.list(queryWrapper);
            
            // 3. 写入 Redis 缓存
            if (!candidateBooks.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, candidateBooks, CANDIDATE_BOOKS_EXPIRE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            }
            
            return candidateBooks;
        } catch (Exception e) {
            System.err.println("获取候选图书失败：" + e.getMessage());
            // Redis 异常时降级
            return getCandidateBooksByPreferencesFromDB(preferredTypes, limit);
        }
    }
    
    /**
     * 降级方法：Redis 不可用时直接从数据库获取候选图书
     */
    private List<Books> getCandidateBooksByPreferencesFromDB(List<String> preferredTypes, Integer limit) {
        try {
            if (preferredTypes.isEmpty()) {
                return booksService.list(new LambdaQueryWrapper<Books>().last("LIMIT " + limit));
            }
            
            LambdaQueryWrapper<Books> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Books::getBookType, preferredTypes)
                       .orderByDesc(Books::getCreateTime)
                       .last("LIMIT " + limit);
            
            return booksService.list(queryWrapper);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 【优化方法 3】构建优化的 Prompt（集成基于内容的推荐）
     * @param userInput 用户输入
     * @param candidateBooks 候选图书
     * @param preferredTypes 偏好分类
     * @return 优化后的 Prompt
     */
    private StringBuilder buildOptimizedPrompt(String userInput, List<Books> candidateBooks, List<String> preferredTypes) {
        StringBuilder prompt = new StringBuilder();
        
        // 角色设定
        prompt.append("你是一位专业的图书馆推荐顾问，擅长根据读者的阅读历史和兴趣偏好推荐合适的图书。\n\n");
        
        // 【新增】基于内容的推荐结果
        Long currentUserId = getCurrentUserIdFromContext();
        if (currentUserId != null) {
            List<com.book.backend.pojo.dto.RecommendedBookDTO> contentBasedRecommendations = 
                getContentBasedRecommendations(currentUserId);
            
            if (!contentBasedRecommendations.isEmpty()) {
                prompt.append("【智能推荐结果】（基于您的阅读历史计算）\n");
                for (int i = 0; i < Math.min(5, contentBasedRecommendations.size()); i++) {
                    com.book.backend.pojo.dto.RecommendedBookDTO rec = contentBasedRecommendations.get(i);
                    prompt.append(String.format("%d. 《%s》作者：%s 分类：%s\n", 
                        i + 1,
                        rec.getBook().getBookName(),
                        rec.getBook().getBookAuthor(),
                        rec.getBook().getBookType()));
                    prompt.append(String.format("   推荐理由：%s（匹配度：%.1f%%）\n\n",
                        rec.getReason(),
                        rec.getScore() * 100));
                }
                prompt.append("\n");
            }
        }
        
        // 用户偏好
        if (!preferredTypes.isEmpty()) {
            prompt.append("【读者偏好】\n");
            prompt.append("该读者偏好的图书类型：").append(String.join(", ", preferredTypes)).append("\n\n");
        }
        
        // 候选图书（只包含相关分类的图书）
        prompt.append("【候选图书列表】\n");
        if (candidateBooks.isEmpty()) {
            prompt.append("数据库暂无图书记录。\n");
        } else {
            Set<String> bookNames = new HashSet<>();
            for (Books book : candidateBooks) {
                if (!bookNames.contains(book.getBookName())) {
                    bookNames.add(book.getBookName());
                    prompt.append("- 《").append(book.getBookName())
                          .append("》作者：").append(book.getBookAuthor())
                          .append(" 分类：").append(book.getBookType())
                          .append(" 描述：").append(book.getBookDescription() != null ? book.getBookDescription() : "无")
                          .append("\n");
                }
            }
        }
        prompt.append("\n");
        
        // 推荐要求
        prompt.append("【推荐要求】\n");
        prompt.append("1. 优先参考【智能推荐结果】中的图书进行推荐\n");
        prompt.append("2. 结合读者的偏好和候选图书列表，推荐 1-3 本最合适的图书\n");
        prompt.append("3. 优先推荐数据库中的图书\n");
        prompt.append("4. 如果数据库中确实没有符合读者兴趣的图书，可以根据你的知识推荐，但需说明\n");
        prompt.append("5. 对每本推荐的图书，说明推荐理由（为什么适合该读者）\n");
        prompt.append("6. 如果用户的问题与图书推荐无关，请礼貌拒绝并引导回图书推荐话题\n\n");
        
        // 用户请求
        prompt.append("【读者请求】\n");
        prompt.append(userInput).append("\n");
        
        return prompt;
    }
    
    /**
     * 获取当前用户 ID（从上下文或缓存）
     */
    private Long getCurrentUserIdFromContext() {
        // TODO: 实现从 Spring Security 或 Session 上下文获取当前用户 ID
        // 暂时返回 null，不影响现有功能
        return null;
    }
    
    /**
     * 获取混合推荐结果（带缓存）
     */
    private List<com.book.backend.pojo.dto.RecommendedBookDTO> getContentBasedRecommendations(Long userId) {
        try {
            String cacheKey = "ai:recommend:hybrid:" + userId;
            List<com.book.backend.pojo.dto.RecommendedBookDTO> cached = 
                (List<com.book.backend.pojo.dto.RecommendedBookDTO>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null && !cached.isEmpty()) {
                System.out.println("[缓存命中] 混合推荐从 Redis 获取，用户：" + userId);
                return cached;
            }
            
            System.out.println("[缓存未命中] 计算混合推荐，用户：" + userId);
            
            // 1. 获取用户历史借阅的图书
            com.book.backend.pojo.Users user = usersService.getById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            
            Long cardNumber = user.getCardNumber();
            LambdaQueryWrapper<BooksBorrow> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BooksBorrow::getCardNumber, cardNumber);
            List<BooksBorrow> borrows = booksBorrowService.list(borrowWrapper);
            
            if (borrows.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 2. 获取历史借阅的图书详情
            List<Books> historyBooks = borrows.stream()
                .map(borrow -> booksService.getById(borrow.getBookNumber()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (historyBooks.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 3. 获取所有图书作为候选集
            List<Books> allBooks = booksService.list();
            
            // 4. 调用混合推荐引擎（整合三种算法）
            List<com.book.backend.pojo.dto.RecommendedBookDTO> recommendations = 
                hybridRecommendationEngine.recommend(userId, historyBooks, allBooks, 10);
            
            // 5. 写入缓存（15 分钟）
            if (!recommendations.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, recommendations, 15L, java.util.concurrent.TimeUnit.MINUTES);
            }
            
            return recommendations;
        } catch (Exception e) {
            System.err.println("获取混合推荐失败：" + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}




