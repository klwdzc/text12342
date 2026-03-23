package com.book.backend.service.recommendation;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.dto.RecommendedBookDTO;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于内容的推荐算法服务
 * 
 * 核心原理：
 * 1. 提取图书特征（分类、作者、描述等）
 * 2. 计算图书之间的相似度（余弦相似度）
 * 3. 根据用户历史借阅记录推荐相似图书
 * 
 * @author AI Assistant
 */
@Service
public class ContentBasedRecommendationService {
    
    /**
     * 基于内容的推荐主方法
     * 
     * @param userHistoryBooks 用户历史借阅的图书列表
     * @param allBooks 所有可选图书
     * @param limit 推荐数量限制
     * @return 推荐的图书列表（按相似度排序）
     */
    public List<RecommendedBookDTO> recommend(List<Books> userHistoryBooks, List<Books> allBooks, int limit) {
        if (userHistoryBooks == null || userHistoryBooks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 为每本历史借阅的图书计算与其他图书的相似度
        Map<Long, Map<Long, Double>> similarityMatrix = calculateSimilarityMatrix(userHistoryBooks, allBooks);
        
        // 2. 聚合分数：对于候选图书，累加它与所有历史图书的相似度
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Books historyBook : userHistoryBooks) {
            Map<Long, Double> similarities = similarityMatrix.get(historyBook.getBookNumber());
            if (similarities != null) {
                for (Map.Entry<Long, Double> entry : similarities.entrySet()) {
                    Long candidateId = entry.getKey();
                    Double similarity = entry.getValue();
                    
                    // 排除已经借阅过的图书
                    if (!isBorrowed(userHistoryBooks, candidateId)) {
                        candidateScores.put(candidateId, candidateScores.getOrDefault(candidateId, 0.0) + similarity);
                    }
                }
            }
        }
        
        // 3. 按分数排序并返回 Top-N
        List<Map.Entry<Long, Double>> sortedCandidates = new ArrayList<>(candidateScores.entrySet());
        sortedCandidates.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 4. 构建推荐结果
        List<RecommendedBookDTO> recommendations = new ArrayList<>();
        Map<Long, Books> bookMap = new HashMap<>();
        for (Books book : allBooks) {
            bookMap.put(book.getBookNumber(), book);
        }
        
        int count = 0;
        for (Map.Entry<Long, Double> entry : sortedCandidates) {
            if (count >= limit) {
                break;
            }
            
            Long bookId = entry.getKey();
            Double score = entry.getValue();
            Books book = bookMap.get(bookId);
            
            if (book != null) {
                recommendations.add(new RecommendedBookDTO(book, score));
                count++;
            }
        }
        
        return recommendations;
    }
    
    /**
     * 计算相似度矩阵
     * 
     * @param historyBooks 历史图书
     * @param allBooks 所有图书
     * @return 相似度矩阵：历史图书 ID -> (候选图书 ID -> 相似度)
     */
    private Map<Long, Map<Long, Double>> calculateSimilarityMatrix(List<Books> historyBooks, List<Books> allBooks) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        
        for (Books historyBook : historyBooks) {
            Map<Long, Double> similarities = new HashMap<>();
            
            for (Books candidateBook : allBooks) {
                // 跳过自己
                if (historyBook.getBookNumber().equals(candidateBook.getBookNumber())) {
                    continue;
                }
                
                // 计算相似度
                double similarity = calculateCosineSimilarity(historyBook, candidateBook);
                similarities.put(candidateBook.getBookNumber(), similarity);
            }
            
            matrix.put(historyBook.getBookNumber(), similarities);
        }
        
        return matrix;
    }
    
    /**
     * 计算两本图书的余弦相似度
     * 
     * @param book1 图书 1
     * @param book2 图书 2
     * @return 余弦相似度值 [0, 1]
     */
    private double calculateCosineSimilarity(Books book1, Books book2) {
        // 1. 构建特征向量
        Map<String, Double> vector1 = buildFeatureVector(book1);
        Map<String, Double> vector2 = buildFeatureVector(book2);
        
        // 2. 计算余弦相似度
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        // 合并所有特征词
        Set<String> allFeatures = new HashSet<>();
        allFeatures.addAll(vector1.keySet());
        allFeatures.addAll(vector2.keySet());
        
        for (String feature : allFeatures) {
            double v1 = vector1.getOrDefault(feature, 0.0);
            double v2 = vector2.getOrDefault(feature, 0.0);
            
            dotProduct += v1 * v2;
            magnitude1 += v1 * v1;
            magnitude2 += v2 * v2;
        }
        
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        
        // 避免除以零
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    /**
     * 构建图书特征向量
     * 
     * 特征包括：
     * - 图书分类（权重：3.0）- 最重要
     * - 作者（权重：2.0）- 较重要
     * - 书名关键词（权重：1.5）
     * - 描述关键词（权重：1.0）
     * 
     * @param book 图书对象
     * @return 特征向量 Map<特征词，权重>
     */
    private Map<String, Double> buildFeatureVector(Books book) {
        Map<String, Double> features = new HashMap<>();
        
        // 1. 图书分类特征（权重最高）
        if (book.getBookType() != null && !book.getBookType().isEmpty()) {
            String[] types = book.getBookType().split("[,，;；]");
            for (String type : types) {
                String normalizedType = type.trim().toLowerCase();
                if (!normalizedType.isEmpty()) {
                    features.put("type_" + normalizedType, 3.0);
                }
            }
        }
        
        // 2. 作者特征
        if (book.getBookAuthor() != null && !book.getBookAuthor().isEmpty()) {
            String normalizedAuthor = book.getBookAuthor().trim().toLowerCase();
            features.put("author_" + normalizedAuthor, 2.0);
        }
        
        // 3. 书名关键词特征
        if (book.getBookName() != null && !book.getBookName().isEmpty()) {
            extractKeywords(book.getBookName()).forEach(keyword -> 
                features.put("title_" + keyword, 1.5)
            );
        }
        
        // 4. 描述关键词特征
        if (book.getBookDescription() != null && !book.getBookDescription().isEmpty()) {
            extractKeywords(book.getBookDescription()).forEach(keyword -> 
                features.put("desc_" + keyword, 1.0)
            );
        }
        
        return features;
    }
    
    /**
     * 从文本中提取关键词
     * 
     * 简单实现：分词 + 去除停用词
     * TODO: 可以集成中文分词库（如 HanLP）来提升效果
     * 
     * @param text 输入文本
     * @return 关键词列表
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 简单的中文分词：按字符分割（简化版）
        // 实际项目中建议使用专业分词库
        List<String> keywords = new ArrayList<>();
        
        // 提取 2-4 字的词组
        for (int i = 0; i < text.length() - 1; i++) {
            for (int len = 2; len <= 4 && i + len <= text.length(); len++) {
                String word = text.substring(i, i + len).trim().toLowerCase();
                if (isValidWord(word)) {
                    keywords.add(word);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * 判断是否是有效的词
     */
    private boolean isValidWord(String word) {
        // 简单的过滤规则
        if (word.length() < 2) {
            return false;
        }
        
        // 排除常见停用词（可以扩展）
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", 
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", 
            "你", "会", "着", "没有", "看", "好", "自己", "这"
        ));
        
        return !stopWords.contains(word);
    }
    
    /**
     * 判断图书是否已被借阅
     */
    private boolean isBorrowed(List<Books> historyBooks, Long bookId) {
        for (Books book : historyBooks) {
            if (book.getBookNumber().equals(bookId)) {
                return true;
            }
        }
        return false;
    }
}
