package com.book.backend.service.analysis;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Users;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.BooksService;
import com.book.backend.service.UsersService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像分析服务
 * 
 * 分析用户阅读行为和偏好，生成多维度的用户画像：
 * 1. 阅读频率分析
 * 2. 图书分类偏好
 * 3. 作者偏好
 * 4. 阅读时间分布
 * 5. 借阅行为特征
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class UserProfileAnalysisService {
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private BooksBorrowService booksBorrowService;
    
    @Resource
    private BooksService booksService;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 缓存 Key
    private static final String USER_PROFILE_CACHE_KEY = "user:profile:";
    private static final long CACHE_EXPIRE_HOURS = 24L; // 缓存 24 小时
    
    /**
     * 获取用户完整的画像信息
     * 
     * @param userId 用户 ID
     * @return 用户画像数据
     */
    public UserProfileDTO getUserProfile(Long userId) {
        try {
            // 1. 尝试从缓存读取
            String cacheKey = USER_PROFILE_CACHE_KEY + userId;
            UserProfileDTO cachedProfile = (UserProfileDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cachedProfile != null) {
                log.info("[用户画像] 从缓存获取用户 {} 的画像", userId);
                return cachedProfile;
            }
            
            log.info("[用户画像] 从数据库计算用户 {} 的画像", userId);
            
            // 2. 从数据库计算用户画像
            UserProfileDTO profile = calculateUserProfile(userId);
            
            // 3. 写入缓存
            if (profile != null) {
                redisTemplate.opsForValue().set(cacheKey, profile, CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
            }
            
            return profile;
            
        } catch (Exception e) {
            log.error("获取用户画像失败：{}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 计算用户画像
     */
    private UserProfileDTO calculateUserProfile(Long userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            return null;
        }
        
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserId(userId);
        profile.setUsername(user.getUsername());
        profile.setCardNumber(user.getCardNumber());
        
        // 1. 获取用户所有借阅记录
        List<BooksBorrow> borrows = getUserBorrowHistory(user.getCardNumber());
        if (borrows.isEmpty()) {
            profile.setTotalBorrows(0);
            profile.setReadingLevel("新用户");
            return profile;
        }
        
        // 2. 基础统计
        profile.setTotalBorrows(borrows.size());
        profile.setCurrentlyBorrowed(countCurrentlyBorrowed(borrows));
        
        // 3. 阅读等级评估
        profile.setReadingLevel(evaluateReadingLevel(borrows.size()));
        
        // 4. 获取借阅的图书详情（优化：批量查询，避免 N+1 问题）
        List<Long> bookNumbers = borrows.stream()
            .map(BooksBorrow::getBookNumber)
            .distinct()
            .collect(Collectors.toList());
        
        List<Books> borrowedBooks = bookNumbers.isEmpty() ? new ArrayList<>() :
            booksService.list(new LambdaQueryWrapper<Books>().in(Books::getBookNumber, bookNumbers))
                .stream()
                .collect(Collectors.toMap(
                    Books::getBookNumber,
                    book -> book,
                    (existing, replacement) -> existing
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        
        // 5. 分类偏好分析
        analyzeTypePreference(profile, borrowedBooks);
        
        // 6. 作者偏好分析
        analyzeAuthorPreference(profile, borrowedBooks);
        
        // 7. 阅读频率分析
        analyzeReadingFrequency(profile, borrows);
        
        // 8. 活跃时间段分析
        analyzeActiveTimePeriod(profile, borrows);
        
        // 9. 借阅行为特征
        analyzeBorrowBehavior(profile, borrows);
        
        return profile;
    }
    
    /**
     * 获取用户借阅历史
     */
    private List<BooksBorrow> getUserBorrowHistory(Long cardNumber) {
        LambdaQueryWrapper<BooksBorrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BooksBorrow::getCardNumber, cardNumber)
                   .orderByDesc(BooksBorrow::getCreateTime);
        return booksBorrowService.list(queryWrapper);
    }
    
    /**
     * 统计当前借阅数量
     */
    private int countCurrentlyBorrowed(List<BooksBorrow> borrows) {
        return (int) borrows.stream()
            .filter(borrow -> borrow.getReturnDate() == null)
            .count();
    }
    
    /**
     * 评估阅读等级
     */
    private String evaluateReadingLevel(int totalBorrows) {
        if (totalBorrows == 0) {
            return "新用户";
        } else if (totalBorrows <= 5) {
            return "初级读者";
        } else if (totalBorrows <= 20) {
            return "中级读者";
        } else if (totalBorrows <= 50) {
            return "高级读者";
        } else {
            return "资深读者";
        }
    }
    
    /**
     * 分析分类偏好
     */
    private void analyzeTypePreference(UserProfileDTO profile, List<Books> books) {
        Map<String, Long> typeCount = books.stream()
            .filter(book -> book.getBookType() != null && !book.getBookType().isEmpty())
            .collect(Collectors.groupingBy(
                Books::getBookType,
                Collectors.counting()
            ));
        
        if (typeCount.isEmpty()) {
            return;
        }
        
        // 按数量排序
        List<Map.Entry<String, Long>> sortedTypes = new ArrayList<>(typeCount.entrySet());
        sortedTypes.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        // Top 3 偏好分类
        List<String> topTypes = sortedTypes.stream()
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        profile.setFavoriteTypes(topTypes);
        profile.setTypeDistribution(typeCount);
    }
    
    /**
     * 分析作者偏好
     */
    private void analyzeAuthorPreference(UserProfileDTO profile, List<Books> books) {
        Map<String, Long> authorCount = books.stream()
            .filter(book -> book.getBookAuthor() != null && !book.getBookAuthor().isEmpty())
            .collect(Collectors.groupingBy(
                Books::getBookAuthor,
                Collectors.counting()
            ));
        
        if (authorCount.isEmpty()) {
            return;
        }
        
        // Top 3 偏好作者
        List<String> topAuthors = authorCount.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        profile.setFavoriteAuthors(topAuthors);
    }
    
    /**
     * 分析阅读频率
     */
    private void analyzeReadingFrequency(UserProfileDTO profile, List<BooksBorrow> borrows) {
        if (borrows.isEmpty()) {
            return;
        }
        
        // 计算平均每月借阅量
        LocalDateTime earliest = borrows.get(borrows.size() - 1).getBorrowDate();
        LocalDateTime latest = borrows.get(0).getBorrowDate();
        
        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(earliest, latest);
        if (monthsBetween > 0) {
            double avgPerMonth = (double) borrows.size() / monthsBetween;
            profile.setAvgBorrowsPerMonth(Math.round(avgPerMonth * 100.0) / 100.0);
            
            // 阅读频率描述
            if (avgPerMonth >= 5) {
                profile.setFrequencyDescription("阅读狂热者");
            } else if (avgPerMonth >= 3) {
                profile.setFrequencyDescription("高频读者");
            } else if (avgPerMonth >= 1) {
                profile.setFrequencyDescription("中频读者");
            } else {
                profile.setFrequencyDescription("低频读者");
            }
        } else {
            profile.setAvgBorrowsPerMonth((double) borrows.size());
            profile.setFrequencyDescription("新书友");
        }
    }
    
    /**
     * 分析活跃时间段
     */
    private void analyzeActiveTimePeriod(UserProfileDTO profile, List<BooksBorrow> borrows) {
        Map<String, Long> hourDistribution = borrows.stream()
            .collect(Collectors.groupingBy(
                borrow -> String.valueOf(borrow.getBorrowDate().getHour()),
                Collectors.counting()
            ));
        
        if (hourDistribution.isEmpty()) {
            return;
        }
        
        // 找出最活跃的时段
        String peakHour = hourDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("未知");
        
        profile.setPeakBorrowHour(peakHour);
        profile.setBorrowTimeDistribution(hourDistribution);
    }
    
    /**
     * 分析借阅行为特征
     */
    private void analyzeBorrowBehavior(UserProfileDTO profile, List<BooksBorrow> borrows) {
        // 计算平均借阅时长
        List<Long> borrowDurations = borrows.stream()
            .filter(borrow -> borrow.getReturnDate() != null)
            .map(borrow -> java.time.Duration.between(
                borrow.getBorrowDate(), 
                borrow.getReturnDate()
            ).toDays())
            .collect(Collectors.toList());
        
        if (!borrowDurations.isEmpty()) {
            long avgDuration = Math.round(borrowDurations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));
            profile.setAvgBorrowDays(avgDuration);
        }
        
        // 是否按时归还
        long overdueCount = borrows.stream()
            .filter(borrow -> borrow.getReturnDate() != null)
            .filter(borrow -> borrow.getReturnDate().isAfter(borrow.getCloseDate()))
            .count();
        
        profile.setOverdueRate(Math.round((double) overdueCount / borrows.size() * 100.0) / 100.0);
        
        if (profile.getOverdueRate() == 0) {
            profile.setCreditRating("优秀");
        } else if (profile.getOverdueRate() < 0.1) {
            profile.setCreditRating("良好");
        } else if (profile.getOverdueRate() < 0.3) {
            profile.setCreditRating("一般");
        } else {
            profile.setCreditRating("较差");
        }
    }
    
    /**
     * 用户画像数据传输对象
     */
    @Data
    public static class UserProfileDTO {
        /** 用户 ID */
        private Long userId;
        
        /** 用户名 */
        private String username;
        
        /** 借阅证号 */
        private Long cardNumber;
        
        /** 总借阅数 */
        private Integer totalBorrows;
        
        /** 当前借阅数 */
        private Integer currentlyBorrowed;
        
        /** 阅读等级 */
        private String readingLevel;
        
        /** 偏好的图书分类（Top 3） */
        private List<String> favoriteTypes;
        
        /** 偏好的作者（Top 3） */
        private List<String> favoriteAuthors;
        
        /** 分类分布统计 */
        private Map<String, Long> typeDistribution;
        
        /** 平均每月借阅量 */
        private Double avgBorrowsPerMonth;
        
        /** 阅读频率描述 */
        private String frequencyDescription;
        
        /** 最活跃的借阅时段 */
        private String peakBorrowHour;
        
        /** 借阅时间分布 */
        private Map<String, Long> borrowTimeDistribution;
        
        /** 平均借阅天数 */
        private Long avgBorrowDays;
        
        /** 逾期率 */
        private Double overdueRate;
        
        /** 信用评级 */
        private String creditRating;
        
        /** 画像更新时间 */
        private String updateTime;
        
        public UserProfileDTO() {
            this.updateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
