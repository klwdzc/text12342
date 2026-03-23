package com.book.backend.service.analysis;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.BooksService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图书热度预测服务
 * 
 * 基于历史借阅数据预测图书的热度趋势：
 * 1. 实时热度计算
 * 2. 热度趋势预测
 * 3. 热门图书排行榜
 * 4. 潜力图书发现
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class BookPopularityPredictionService {
    
    @Resource
    private BooksService booksService;
    
    @Resource
    private BooksBorrowService booksBorrowService;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 缓存 Key
    private static final String BOOK_HOTNESS_CACHE_KEY = "book:hotness:";
    private static final String HOT_BOOKS_RANK_KEY = "rank:hot:books";
    private static final long CACHE_EXPIRE_HOURS = 6L; // 6 小时更新一次
    
    /**
     * 获取图书的实时热度分数
     * 
     * @param bookNumber 图书编号
     * @return 热度分数 [0-100]
     */
    public Double getBookHotness(Long bookNumber) {
        try {
            // 1. 尝试从缓存读取
            String cacheKey = BOOK_HOTNESS_CACHE_KEY + bookNumber;
            Double cachedHotness = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedHotness != null) {
                return cachedHotness;
            }
            
            // 2. 计算热度分数
            Double hotness = calculateBookHotness(bookNumber);
            
            // 3. 写入缓存
            if (hotness != null) {
                redisTemplate.opsForValue().set(cacheKey, hotness, CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
            }
            
            return hotness;
            
        } catch (Exception e) {
            log.error("获取图书热度失败：{}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * 计算图书热度分数
     * 
     * 热度计算公式：
     * Hotness = (基础借阅分 * 0.4) + (近期趋势分 * 0.4) + (时间衰减分 * 0.2)
     * 
     * @param bookNumber 图书编号
     * @return 热度分数
     */
    private Double calculateBookHotness(Long bookNumber) {
        // 1. 获取该书的所有借阅记录
        List<BooksBorrow> borrows = getBookBorrowHistory(bookNumber);
        if (borrows.isEmpty()) {
            return 0.0;
        }
        
        // 2. 基础借阅分（总借阅次数）
        double baseScore = Math.min(100, borrows.size() * 5);
        
        // 3. 近期趋势分（最近 30 天的借阅量）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentBorrows = borrows.stream()
            .filter(borrow -> borrow.getBorrowDate().isAfter(thirtyDaysAgo))
            .count();
        double trendScore = Math.min(100, recentBorrows * 10);
        
        // 4. 时间衰减分（最近的借阅权重更高）
        double timeDecayScore = calculateTimeDecayScore(borrows);
        
        // 5. 加权计算最终热度
        double finalHotness = (baseScore * 0.4) + (trendScore * 0.4) + (timeDecayScore * 0.2);
        
        return Math.min(100, Math.max(0, finalHotness));
    }
    
    /**
     * 计算时间衰减分数
     * 使用指数衰减函数：weight = e^(-λt)
     * λ = 0.05, t = 距离现在的天数
     */
    private double calculateTimeDecayScore(List<BooksBorrow> borrows) {
        double totalWeight = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (BooksBorrow borrow : borrows) {
            long daysDiff = java.time.Duration.between(borrow.getBorrowDate(), now).toDays();
            double weight = Math.exp(-0.05 * daysDiff);
            totalWeight += weight;
        }
        
        // 归一化到 0-100
        return Math.min(100, totalWeight * 10);
    }
    
    /**
     * 获取图书借阅历史
     */
    private List<BooksBorrow> getBookBorrowHistory(Long bookNumber) {
        LambdaQueryWrapper<BooksBorrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BooksBorrow::getBookNumber, bookNumber)
                   .orderByDesc(BooksBorrow::getBorrowDate);
        return booksBorrowService.list(queryWrapper);
    }
    
    /**
     * 获取热门图书排行榜
     * 
     * @param limit 返回数量
     * @return 热门图书列表
     */
    public List<HotBookDTO> getHotBooksRanking(int limit) {
        try {
            // 1. 尝试从缓存读取
            List<HotBookDTO> cached = (List<HotBookDTO>) redisTemplate.opsForValue().get(HOT_BOOKS_RANK_KEY);
            if (cached != null && !cached.isEmpty()) {
                return cached.subList(0, Math.min(limit, cached.size()));
            }
            
            // 2. 获取所有图书
            List<Books> allBooks = booksService.list();
            
            // 3. 计算每本书的热度
            List<HotBookDTO> hotBooks = new ArrayList<>();
            for (Books book : allBooks) {
                Double hotness = getBookHotness(book.getBookNumber());
                
                HotBookDTO dto = new HotBookDTO();
                dto.setBookNumber(book.getBookNumber());
                dto.setBookName(book.getBookName());
                dto.setBookAuthor(book.getBookAuthor());
                dto.setBookType(book.getBookType());
                dto.setHotnessScore(hotness);
                dto.setHotnessLevel(getHotnessLevel(hotness));
                
                hotBooks.add(dto);
            }
            
            // 4. 按热度排序
            hotBooks.sort((a, b) -> Double.compare(b.getHotnessScore(), a.getHotnessScore()));
            
            // 5. 写入缓存
            redisTemplate.opsForValue().set(HOT_BOOKS_RANK_KEY, hotBooks, CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
            
            // 6. 返回 Top-N
            return hotBooks.subList(0, Math.min(limit, hotBooks.size()));
            
        } catch (Exception e) {
            log.error("获取热门图书排行失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取热度等级描述
     */
    private String getHotnessLevel(Double hotness) {
        if (hotness >= 80) {
            return "🔥 超热";
        } else if (hotness >= 60) {
            return "📈 热门";
        } else if (hotness >= 40) {
            return "📚 温门";
        } else if (hotness >= 20) {
            return "📖 冷门";
        } else {
            return "❄️ 冰点";
        }
    }
    
    /**
     * 发现潜力图书（热度上升快）
     * 
     * @param limit 返回数量
     * @return 潜力图书列表
     */
    public List<HotBookDTO> findRisingStars(int limit) {
        try {
            // 1. 获取所有图书
            List<Books> allBooks = booksService.list();
            
            // 2. 计算每本书的热度增长率
            List<RisingStarDTO> risingStars = new ArrayList<>();
            
            for (Books book : allBooks) {
                RisingStarDTO star = calculateRisingTrend(book.getBookNumber());
                if (star != null && star.getGrowthRate() > 0) {
                    risingStars.add(new RisingStarDTO(
                        star.getBookNumber(),
                        star.getBookName(),
                        star.getBookAuthor(),
                        star.getCurrentHotness(),
                        star.getGrowthRate()
                    ));
                }
            }
            
            // 3. 按增长率排序
            risingStars.sort((a, b) -> Double.compare(b.getGrowthRate(), a.getGrowthRate()));
            
            // 4. 转换为 HotBookDTO
            List<HotBookDTO> result = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, risingStars.size()); i++) {
                RisingStarDTO star = risingStars.get(i);
                
                HotBookDTO dto = new HotBookDTO();
                dto.setBookNumber(star.getBookNumber());
                dto.setBookName(star.getBookName());
                dto.setBookAuthor(star.getBookAuthor());
                dto.setHotnessScore(star.getCurrentHotness());
                dto.setHotnessLevel("🚀 潜力股 (+" + String.format("%.1f", star.getGrowthRate() * 100) + "%)");
                
                result.add(dto);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("发现潜力图书失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 计算图书的热度增长趋势
     */
    private RisingStarDTO calculateRisingTrend(Long bookNumber) {
        try {
            // 获取借阅历史
            List<BooksBorrow> borrows = getBookBorrowHistory(bookNumber);
            if (borrows.isEmpty()) {
                return null;
            }
            
            // 计算当前热度
            Double currentHotness = getBookHotness(bookNumber);
            
            // 计算前 30 天和后 30 天的借阅量
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sixtyDaysAgo = now.minusDays(60);
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            
            long firstPeriodBorrows = borrows.stream()
                .filter(b -> b.getBorrowDate().isAfter(sixtyDaysAgo) && b.getBorrowDate().isBefore(thirtyDaysAgo))
                .count();
            
            long secondPeriodBorrows = borrows.stream()
                .filter(b -> b.getBorrowDate().isAfter(thirtyDaysAgo))
                .count();
            
            // 计算增长率
            double growthRate = 0;
            if (firstPeriodBorrows > 0) {
                growthRate = (double)(secondPeriodBorrows - firstPeriodBorrows) / firstPeriodBorrows;
            } else if (secondPeriodBorrows > 0) {
                growthRate = 1.0; // 从无到有，增长率 100%
            }
            
            // 获取图书信息
            Books book = booksService.getById(bookNumber);
            if (book == null) {
                return null;
            }
            
            return new RisingStarDTO(
                bookNumber,
                book.getBookName(),
                book.getBookAuthor(),
                currentHotness,
                growthRate
            );
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 热门图书数据传输对象
     */
    @Data
    public static class HotBookDTO {
        /** 图书编号 */
        private Long bookNumber;
        
        /** 图书名称 */
        private String bookName;
        
        /** 作者 */
        private String bookAuthor;
        
        /** 分类 */
        private String bookType;
        
        /** 热度分数 */
        private Double hotnessScore;
        
        /** 热度等级 */
        private String hotnessLevel;
    }
    
    /**
     * 潜力图书数据传输对象
     */
    @Data
    public static class RisingStarDTO {
        /** 图书编号 */
        private final Long bookNumber;
        
        /** 图书名称 */
        private final String bookName;
        
        /** 作者 */
        private final String bookAuthor;
        
        /** 当前热度 */
        private final Double currentHotness;
        
        /** 增长率 */
        private final Double growthRate;
        
        public RisingStarDTO(Long bookNumber, String bookName, String bookAuthor, 
                           Double currentHotness, Double growthRate) {
            this.bookNumber = bookNumber;
            this.bookName = bookName;
            this.bookAuthor = bookAuthor;
            this.currentHotness = currentHotness;
            this.growthRate = growthRate;
        }
    }
}
