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
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 基于用户的协同过滤推荐服务 (User-Based Collaborative Filtering)
 * 
 * 核心原理：
 * 1. 找到与目标用户兴趣相似的其他用户（邻居）
 * 2. 推荐邻居喜欢但目标用户还没看过的书
 * 
 * 相似度计算：Jaccard 相似系数
 * similarity(A, B) = |A∩B| / |A∪B|
 * 
 * @author AI Assistant
 */
@Service
public class UserBasedCollaborativeFilteringService {
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private BooksBorrowService booksBorrowService;
    
    @Resource
    private BooksService booksService;
    
    /**
     * 基于用户的协同过滤推荐主方法
     * 
     * @param userId 目标用户 ID
     * @param limit 推荐数量限制
     * @return 推荐的图书列表
     */
    public List<RecommendedBookDTO> recommend(Long userId, int limit) {
        try {
            // 1. 获取目标用户的借阅历史
            List<Long> targetUserBorrowedBooks = getUserBorrowedBooks(userId);
            if (targetUserBorrowedBooks.isEmpty()) {
                return new ArrayList<>(); // 新用户没有借阅历史
            }
            
            // 2. 获取所有其他用户的借阅数据
            Map<Long, Set<Long>> userBookMatrix = buildUserBookMatrix(userId);
            
            // 3. 计算与其他用户的相似度
            Map<Long, Double> userSimilarities = calculateUserSimilarities(
                userId, 
                new HashSet<>(targetUserBorrowedBooks),  // 转换为 Set
                userBookMatrix
            );
            
            // 4. 选择 Top-K 个最近邻居
            List<Map.Entry<Long, Double>> topNeighbors = getTopKUsers(userSimilarities, 10);
            
            // 5. 收集邻居喜欢但目标用户没看过的书
            Map<Long, Double> candidateBookScores = collectCandidateBooks(
                new HashSet<>(targetUserBorrowedBooks),  // 转换为 Set
                topNeighbors, 
                userBookMatrix
            );
            
            // 6. 按分数排序并返回推荐结果
            return generateRecommendations(candidateBookScores, limit);
            
        } catch (Exception e) {
            System.err.println("User-Based CF 推荐失败：" + e.getMessage());
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
     * 构建用户 - 图书矩阵（排除目标用户）
     * Key: 用户 ID, Value: 该用户借阅过的图书 ID 集合
     */
    private Map<Long, Set<Long>> buildUserBookMatrix(Long excludeUserId) {
        Map<Long, Set<Long>> matrix = new HashMap<>();
        
        // 获取所有用户的借阅记录
        List<Users> allUsers = usersService.list();
        
        for (Users user : allUsers) {
            if (user.getUserId().equals(excludeUserId)) {
                continue; // 跳过目标用户自己
            }
            
            Long cardNumber = user.getCardNumber();
            LambdaQueryWrapper<BooksBorrow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BooksBorrow::getCardNumber, cardNumber);
            List<BooksBorrow> borrows = booksBorrowService.list(queryWrapper);
            
            if (!borrows.isEmpty()) {
                Set<Long> bookIds = borrows.stream()
                    .map(BooksBorrow::getBookNumber)
                    .collect(Collectors.toSet());
                matrix.put(user.getUserId(), bookIds);
            }
        }
        
        return matrix;
    }
    
    /**
     * 计算目标用户与其他用户的相似度（使用 Jaccard 相似系数）
     */
    private Map<Long, Double> calculateUserSimilarities(
        Long targetUserId,
        Set<Long> targetUserBooks,
        Map<Long, Set<Long>> userBookMatrix
    ) {
        Map<Long, Double> similarities = new HashMap<>();
        Set<Long> targetBookSet = new HashSet<>(targetUserBooks);
        
        for (Map.Entry<Long, Set<Long>> entry : userBookMatrix.entrySet()) {
            Long otherUserId = entry.getKey();
            Set<Long> otherUserBooks = entry.getValue();
            
            // 计算 Jaccard 相似度
            double similarity = calculateJaccardSimilarity(targetBookSet, otherUserBooks);
            
            if (similarity > 0) {
                similarities.put(otherUserId, similarity);
            }
        }
        
        return similarities;
    }
    
    /**
     * 计算 Jaccard 相似系数
     * similarity = |A∩B| / |A∪B|
     */
    private double calculateJaccardSimilarity(Set<Long> setA, Set<Long> setB) {
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }
        
        // 计算交集
        Set<Long> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        
        // 计算并集
        Set<Long> union = new HashSet<>(setA);
        union.addAll(setB);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 获取 Top-K 个最相似的用户
     */
    private List<Map.Entry<Long, Double>> getTopKUsers(Map<Long, Double> userSimilarities, int k) {
        List<Map.Entry<Long, Double>> sortedUsers = new ArrayList<>(userSimilarities.entrySet());
        
        // 按相似度降序排序
        sortedUsers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 返回 Top-K
        return sortedUsers.subList(0, Math.min(k, sortedUsers.size()));
    }
    
    /**
     * 从邻居用户收集候选图书
     */
    private Map<Long, Double> collectCandidateBooks(
        Set<Long> targetUserBooks,
        List<Map.Entry<Long, Double>> topNeighbors,
        Map<Long, Set<Long>> userBookMatrix
    ) {
        Map<Long, Double> bookScores = new HashMap<>();
        
        for (Map.Entry<Long, Double> neighbor : topNeighbors) {
            Long neighborId = neighbor.getKey();
            Double similarity = neighbor.getValue();
            Set<Long> neighborBooks = userBookMatrix.get(neighborId);
            
            // 只推荐目标用户没看过的书
            for (Long bookId : neighborBooks) {
                if (!targetUserBooks.contains(bookId)) {
                    // 累加分数：相似度越高，推荐权重越大
                    bookScores.put(bookId, bookScores.getOrDefault(bookId, 0.0) + similarity);
                }
            }
        }
        
        return bookScores;
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
