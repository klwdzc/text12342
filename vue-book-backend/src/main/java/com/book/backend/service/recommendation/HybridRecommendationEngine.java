package com.book.backend.service.recommendation;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.dto.RecommendedBookDTO;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合推荐引擎
 * 
 * 整合三种推荐算法：
 * 1. 基于内容的推荐 (Content-Based) - 权重 40%
 * 2. 基于用户的协同过滤 (User-Based CF) - 权重 30%
 * 3. 基于物品的协同过滤 (Item-Based CF) - 权重 30%
 * 
 * 融合策略：加权平均 + 多样性调整
 * 
 * @author AI Assistant
 */
@Service
public class HybridRecommendationEngine {
    
    @Resource
    private ContentBasedRecommendationService contentBasedService;
    
    @Resource
    private UserBasedCollaborativeFilteringService userBasedCFService;
    
    @Resource
    private ItemBasedCollaborativeFilteringService itemBasedCFService;
    
    // 推荐算法权重配置
    private static final double CONTENT_BASED_WEIGHT = 0.4;
    private static final double USER_BASED_CF_WEIGHT = 0.3;
    private static final double ITEM_BASED_CF_WEIGHT = 0.3;
    
    /**
     * 混合推荐主方法
     * 
     * @param userId 用户 ID
     * @param userHistoryBooks 用户历史借阅的图书列表
     * @param allBooks 所有图书
     * @param limit 推荐数量限制
     * @return 混合推荐的图书列表
     */
    public List<RecommendedBookDTO> recommend(
        Long userId,
        List<Books> userHistoryBooks,
        List<Books> allBooks,
        int limit
    ) {
        try {
            System.out.println("[混合推荐] 开始为用户 " + userId + " 生成推荐...");
            
            // 1. 并行执行三种推荐算法
            List<RecommendedBookDTO> contentBasedRecs = executeWithFallback(
                () -> contentBasedService.recommend(userHistoryBooks, allBooks, limit * 2),
                "基于内容的推荐"
            );
            
            List<RecommendedBookDTO> userBasedCFRecs = executeWithFallback(
                () -> userBasedCFService.recommend(userId, limit * 2),
                "User-Based CF"
            );
            
            List<RecommendedBookDTO> itemBasedCFRecs = executeWithFallback(
                () -> itemBasedCFService.recommend(userId, limit * 2),
                "Item-Based CF"
            );
            
            System.out.println(String.format(
                "[混合推荐] 三种算法结果数：%d, %d, %d",
                contentBasedRecs.size(),
                userBasedCFRecs.size(),
                itemBasedCFRecs.size()
            ));
            
            // 2. 融合推荐结果（加权）
            Map<Long, WeightedRecommendation> weightedMap = new HashMap<>();
            
            // 融合基于内容的推荐
            mergeRecommendations(weightedMap, contentBasedRecs, CONTENT_BASED_WEIGHT);
            
            // 融合 User-Based CF 推荐
            mergeRecommendations(weightedMap, userBasedCFRecs, USER_BASED_CF_WEIGHT);
            
            // 融合 Item-Based CF 推荐
            mergeRecommendations(weightedMap, itemBasedCFRecs, ITEM_BASED_CF_WEIGHT);
            
            // 3. 按加权分数排序
            List<Map.Entry<Long, WeightedRecommendation>> sortedRecs = 
                new ArrayList<>(weightedMap.entrySet());
            
            sortedRecs.sort((a, b) -> Double.compare(
                b.getValue().getFinalScore(), 
                a.getValue().getFinalScore()
            ));
            
            // 4. 应用多样性调整（可选）
            applyDiversityAdjustment(sortedRecs, allBooks);
            
            // 5. 生成最终推荐列表
            List<RecommendedBookDTO> finalRecommendations = new ArrayList<>();
            int count = 0;
            
            for (Map.Entry<Long, WeightedRecommendation> entry : sortedRecs) {
                if (count >= limit) {
                    break;
                }
                
                WeightedRecommendation weightedRec = entry.getValue();
                RecommendedBookDTO finalRec = createFinalRecommendation(weightedRec);
                
                if (finalRec != null) {
                    finalRecommendations.add(finalRec);
                    count++;
                }
            }
            
            System.out.println(String.format(
                "[混合推荐] 最终推荐数量：%d",
                finalRecommendations.size()
            ));
            
            return finalRecommendations;
            
        } catch (Exception e) {
            System.err.println("混合推荐失败：" + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 融合多个推荐列表
     */
    private void mergeRecommendations(
        Map<Long, WeightedRecommendation> weightedMap,
        List<RecommendedBookDTO> recommendations,
        double weight
    ) {
        for (RecommendedBookDTO rec : recommendations) {
            Long bookId = rec.getBook().getBookNumber();
            Double score = rec.getScore();
            
            if (!weightedMap.containsKey(bookId)) {
                weightedMap.put(bookId, new WeightedRecommendation(rec.getBook()));
            }
            
            // 累加加权分数
            weightedMap.get(bookId).addWeightedScore(score * weight);
            
            // 记录使用了哪些算法
            weightedMap.get(bookId).incrementAlgorithmCount();
        }
    }
    
    /**
     * 应用多样性调整
     * 避免推荐结果过于集中在某个分类
     */
    private void applyDiversityAdjustment(
        List<Map.Entry<Long, WeightedRecommendation>> sortedRecs,
        List<Books> allBooks
    ) {
        // 统计各分类的比例
        Map<String, Integer> typeDistribution = new HashMap<>();
        for (Books book : allBooks) {
            String type = book.getBookType();
            typeDistribution.put(type, typeDistribution.getOrDefault(type, 0) + 1);
        }
        
        // 对热门分类进行轻微惩罚（可选优化）
        // TODO: 实现更复杂的多样性调整策略
    }
    
    /**
     * 创建最终的推荐对象
     */
    private RecommendedBookDTO createFinalRecommendation(WeightedRecommendation weightedRec) {
        Books book = weightedRec.getBook();
        Double finalScore = weightedRec.getFinalScore();
        
        RecommendedBookDTO finalRec = new RecommendedBookDTO(book, finalScore);
        
        // 设置增强版的推荐理由
        String enhancedReason = buildEnhancedReason(weightedRec);
        finalRec.setReason(enhancedReason);
        
        return finalRec;
    }
    
    /**
     * 构建增强版的推荐理由
     */
    private String buildEnhancedReason(WeightedRecommendation weightedRec) {
        StringBuilder reason = new StringBuilder();
        
        int algoCount = weightedRec.getAlgorithmCount();
        Double score = weightedRec.getFinalScore() * 100;
        
        if (algoCount >= 3) {
            reason.append(String.format(
                "强烈推荐！三种算法一致认可（匹配度：%.1f%%）",
                score
            ));
        } else if (algoCount == 2) {
            reason.append(String.format(
                "双重推荐！两种算法都认为适合您（匹配度：%.1f%%）",
                score
            ));
        } else {
            reason.append(String.format(
                "根据您的阅读兴趣推荐（匹配度：%.1f%%）",
                score
            ));
        }
        
        return reason.toString();
    }
    
    /**
     * 带降级处理的执行方法
     */
    private List<RecommendedBookDTO> executeWithFallback(
        RecommendationExecutor executor,
        String algorithmName
    ) {
        try {
            return executor.execute();
        } catch (Exception e) {
            System.err.println(algorithmName + " 执行失败：" + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 加权推荐内部类
     */
    private static class WeightedRecommendation {
        private final Books book;
        private double totalWeightedScore = 0.0;
        private int algorithmCount = 0;
        
        public WeightedRecommendation(Books book) {
            this.book = book;
        }
        
        public void addWeightedScore(double score) {
            this.totalWeightedScore += score;
        }
        
        public void incrementAlgorithmCount() {
            this.algorithmCount++;
        }
        
        public Books getBook() {
            return book;
        }
        
        public double getFinalScore() {
            // 归一化到 [0, 1]
            return Math.min(1.0, totalWeightedScore);
        }
        
        public int getAlgorithmCount() {
            return algorithmCount;
        }
    }
    
    /**
     * 推荐执行函数式接口
     */
    @FunctionalInterface
    private interface RecommendationExecutor {
        List<RecommendedBookDTO> execute();
    }
}
