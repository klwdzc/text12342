package com.book.backend.service.recommendation;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Users;
import com.book.backend.pojo.dto.RecommendedBookDTO;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.BooksService;
import com.book.backend.service.UsersService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 基于物品的协同过滤推荐服务 (Item-Based Collaborative Filtering)
 * 
 * 核心原理：
 * 1. 计算图书之间的相似度（基于共同被借阅的模式）
 * 2. 推荐与用户历史喜欢的图书相似的其他图书
 * 
 * 相似度计算：余弦相似度或改进的 Jaccard
 * 
 * @author AI Assistant
 */
@Service
public class ItemBasedCollaborativeFilteringService {
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private BooksBorrowService booksBorrowService;
    
    @Resource
    private BooksService booksService;
    
    // 缓存图书相似度矩阵
    private final Map<String, Map<Long, Double>> bookSimilarityCache = new ConcurrentHashMap<>();
    
    /**
     * 基于物品的协同过滤推荐主方法
     * 
     * @param userId 目标用户 ID
     * @param limit 推荐数量限制
     * @return 推荐的图书列表
     */
    public List<RecommendedBookDTO> recommend(Long userId, int limit) {
        try {
            // 1. 获取目标用户的借阅历史
            List<Long> userBorrowedBooks = getUserBorrowedBooks(userId);
            if (userBorrowedBooks.isEmpty()) {
                return new ArrayList<>(); // 新用户没有借阅历史
            }
            
            // 2. 获取所有图书
            List<Books> allBooks = booksService.list();
            
            // 3. 为每本历史图书计算与其他图书的相似度
            Map<Long, Double> candidateScores = calculateCandidateScores(
                userBorrowedBooks, 
                allBooks
            );
            
            // 4. 排除用户已经借阅过的图书
            for (Long bookId : userBorrowedBooks) {
                candidateScores.remove(bookId);
            }
            
            // 5. 生成推荐结果
            return generateRecommendations(candidateScores, limit);
            
        } catch (Exception e) {
            System.err.println("Item-Based CF 推荐失败：" + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取用户借阅过的图书 ID 列表
     */
    private List<Long> getUserBorrowedBooks(Long userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        
        Long cardNumber = user.getCardNumber();
        LambdaQueryWrapper<BooksBorrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BooksBorrow::getCardNumber, cardNumber);
        List<BooksBorrow> borrows = booksBorrowService.list(queryWrapper);
        
        return borrows.stream()
            .map(BooksBorrow::getBookNumber)
            .collect(Collectors.toList());
    }
    
    /**
     * 计算候选图书的得分
     */
    private Map<Long, Double> calculateCandidateScores(
        List<Long> userHistory,
        List<Books> allBooks
    ) {
        Map<Long, Double> scores = new HashMap<>();
        
        for (Books book : allBooks) {
            Long targetBookId = book.getBookNumber();
            
            // 计算这本书与用户历史图书的总相似度
            double totalSimilarity = 0.0;
            
            for (Long historyBookId : userHistory) {
                double similarity = getBookSimilarity(historyBookId, targetBookId);
                totalSimilarity += similarity;
            }
            
            if (totalSimilarity > 0) {
                scores.put(targetBookId, totalSimilarity);
            }
        }
        
        return scores;
    }
    
    /**
     * 获取两本图书的相似度（带缓存）
     */
    private double getBookSimilarity(Long book1Id, Long book2Id) {
        // 构建缓存 key（确保对称性）
        String cacheKey = Math.min(book1Id, book2Id) + "_" + Math.max(book1Id, book2Id);
        
        // 检查缓存
        Map<Long, Double> similarities = bookSimilarityCache.get(cacheKey);
        if (similarities != null) {
            Double cached = similarities.get(book2Id.equals(book1Id) ? book1Id : book2Id);
            if (cached != null) {
                return cached;
            }
        }
        
        // 计算相似度
        double similarity = calculateBookSimilarity(book1Id, book2Id);
        
        // 写入缓存
        if (similarities == null) {
            similarities = new ConcurrentHashMap<>();
            bookSimilarityCache.put(cacheKey, similarities);
        }
        similarities.put(book2Id, similarity);
        
        return similarity;
    }
    
    /**
     * 计算两本图书的相似度（基于借阅模式）
     * 使用改进的 Jaccard 相似度
     */
    private double calculateBookSimilarity(Long book1Id, Long book2Id) {
        try {
            // 获取借阅过 book1 的用户集合
            Set<Long> usersWhoBorrowedBook1 = getUsersWhoBorrowedBook(book1Id);
            
            // 获取借阅过 book2 的用户集合
            Set<Long> usersWhoBorrowedBook2 = getUsersWhoBorrowedBook(book2Id);
            
            if (usersWhoBorrowedBook1.isEmpty() || usersWhoBorrowedBook2.isEmpty()) {
                return 0.0;
            }
            
            // 计算交集（同时借阅过两本书的用户）
            Set<Long> intersection = new HashSet<>(usersWhoBorrowedBook1);
            intersection.retainAll(usersWhoBorrowedBook2);
            
            // 计算并集
            Set<Long> union = new HashSet<>(usersWhoBorrowedBook1);
            union.addAll(usersWhoBorrowedBook2);
            
            if (union.isEmpty()) {
                return 0.0;
            }
            
            // Jaccard 相似度
            return (double) intersection.size() / union.size();
            
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 获取借阅过某本书的所有用户 ID
     */
    private Set<Long> getUsersWhoBorrowedBook(Long bookId) {
        Set<Long> userIds = new HashSet<>();
        
        // 查询所有借阅过这本书的记录
        LambdaQueryWrapper<BooksBorrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BooksBorrow::getBookNumber, bookId);
        List<BooksBorrow> borrows = booksBorrowService.list(queryWrapper);
        
        for (BooksBorrow borrow : borrows) {
            Long cardNumber = borrow.getCardNumber();
            
            // 根据借阅证号查找用户 ID
            LambdaQueryWrapper<Users> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(Users::getCardNumber, cardNumber);
            Users user = usersService.getOne(userQuery);
            
            if (user != null) {
                userIds.add(user.getUserId());
            }
        }
        
        return userIds;
    }
    
    /**
     * 生成最终的推荐列表
     */
    private List<RecommendedBookDTO> generateRecommendations(Map<Long, Double> bookScores, int limit) {
        // 按分数降序排序
        List<Map.Entry<Long, Double>> sortedBooks = new ArrayList<>(bookScores.entrySet());
        sortedBooks.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 构建推荐结果
        List<RecommendedBookDTO> recommendations = new ArrayList<>();
        int count = 0;
        
        for (Map.Entry<Long, Double> entry : sortedBooks) {
            if (count >= limit) {
                break;
            }
            
            Long bookId = entry.getKey();
            Double score = normalizeScore(entry.getValue());
            
            Books book = booksService.getById(bookId);
            if (book != null) {
                recommendations.add(new RecommendedBookDTO(book, score));
                count++;
            }
        }
        
        return recommendations;
    }
    
    /**
     * 归一化分数到 [0, 1] 范围
     */
    private Double normalizeScore(Double rawScore) {
        // 简单归一化：假设最大可能分数为 1.0
        return Math.min(1.0, rawScore);
    }
}
