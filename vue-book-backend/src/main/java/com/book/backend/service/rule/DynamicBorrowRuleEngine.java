package com.book.backend.service.rule;

import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Users;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.UsersService;
import com.book.backend.service.analysis.UserProfileAnalysisService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 动态借阅规则引擎
 * 
 * 功能：
 * 1. 基于用户信用评级的动态借阅数量限制
 * 2. 基于用户画像的个性化借阅期限
 * 3. 智能逾期预警
 * 4. 弹性续借策略
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class DynamicBorrowRuleEngine {
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private BooksBorrowService booksBorrowService;
    
    @Resource
    private UserProfileAnalysisService userProfileService;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 缓存配置
    private static final String USER_RULE_CACHE_KEY = "borrow:rule:user:";
    private static final long CACHE_EXPIRE_HOURS = 12L;
    
    /**
     * 获取用户的动态借阅规则
     * 
     * @param userId 用户 ID
     * @return 借阅规则配置
     */
    public BorrowRuleConfig getUserBorrowRule(Long userId) {
        try {
            // 1. 尝试从缓存读取
            String cacheKey = USER_RULE_CACHE_KEY + userId;
            BorrowRuleConfig cachedRule = (BorrowRuleConfig) redisTemplate.opsForValue().get(cacheKey);
            if (cachedRule != null) {
                log.info("[动态规则] 从缓存获取用户 {} 的借阅规则", userId);
                return cachedRule;
            }
            
            log.info("[动态规则] 计算用户 {} 的借阅规则", userId);
            
            // 2. 获取用户画像
            UserProfileAnalysisService.UserProfileDTO profile = userProfileService.getUserProfile(userId);
            
            // 3. 计算动态规则
            BorrowRuleConfig ruleConfig = calculateDynamicRule(profile);
            
            // 4. 写入缓存
            redisTemplate.opsForValue().set(cacheKey, ruleConfig, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            return ruleConfig;
            
        } catch (Exception e) {
            log.error("获取动态借阅规则失败：{}", e.getMessage(), e);
            return getDefaultRule();
        }
    }
    
    /**
     * 计算动态借阅规则
     */
    private BorrowRuleConfig calculateDynamicRule(UserProfileAnalysisService.UserProfileDTO profile) {
        BorrowRuleConfig rule = new BorrowRuleConfig();
        
        if (profile == null || profile.getTotalBorrows() == 0) {
            // 新用户默认规则
            return getDefaultRule();
        }
        
        // 1. 基础借阅数量（根据阅读等级）
        int baseLimit = calculateBaseLimitByLevel(profile.getReadingLevel());
        
        // 2. 信用评级调整
        double creditMultiplier = calculateCreditMultiplier(profile.getCreditRating());
        
        // 3. 最终借阅数量限制
        int maxBorrowLimit = (int)(baseLimit * creditMultiplier);
        rule.setMaxBorrowCount(Math.min(maxBorrowLimit, 20)); // 最多不超过 20 本
        
        // 4. 借阅期限调整（根据历史平均借阅天数）
        int baseDays = 30; // 基础 30 天
        Long avgBorrowDays = profile.getAvgBorrowDays();
        
        if (avgBorrowDays != null && avgBorrowDays < 15) {
            // 阅读速度快，缩短借阅期限
            rule.setBorrowDays(21); // 3 周
            rule.setRenewableDays(15); // 续借 15 天
        } else if (avgBorrowDays != null && avgBorrowDays > 25) {
            // 阅读速度慢，延长借阅期限
            rule.setBorrowDays(45); // 45 天
            rule.setRenewableDays(30); // 续借 30 天
        } else {
            // 正常速度
            rule.setBorrowDays(baseDays);
            rule.setRenewableDays(20); // 续借 20 天
        }
        
        // 5. 续借次数（根据信用评级）
        if ("优秀".equals(profile.getCreditRating())) {
            rule.setMaxRenewTimes(2); // 可续借 2 次
        } else if ("良好".equals(profile.getCreditRating())) {
            rule.setMaxRenewTimes(1); // 可续借 1 次
        } else {
            rule.setMaxRenewTimes(0); // 不可续借
        }
        
        // 6. 逾期容忍度
        rule.setOverdueTolerance(calculateOverdueTolerance(profile.getOverdueRate()));
        
        // 7. 特权功能
        rule.setEnablePriorityReservation("优秀".equals(profile.getCreditRating()) || "良好".equals(profile.getCreditRating()));
        rule.setEnableHomeDelivery("资深读者".equals(profile.getReadingLevel()));
        
        return rule;
    }
    
    /**
     * 根据阅读等级计算基础借阅数量
     */
    private int calculateBaseLimitByLevel(String readingLevel) {
        switch (readingLevel) {
            case "新用户":
                return 3;
            case "初级读者":
                return 5;
            case "中级读者":
                return 8;
            case "高级读者":
                return 10;
            case "资深读者":
                return 15;
            default:
                return 5;
        }
    }
    
    /**
     * 根据信用评级计算倍数
     */
    private double calculateCreditMultiplier(String creditRating) {
        switch (creditRating) {
            case "优秀":
                return 1.5; // 增加 50%
            case "良好":
                return 1.2; // 增加 20%
            case "一般":
                return 1.0; // 不变
            case "较差":
                return 0.6; // 减少 40%
            default:
                return 1.0;
        }
    }
    
    /**
     * 计算逾期容忍度（允许的最大逾期次数）
     */
    private int calculateOverdueTolerance(Double overdueRate) {
        if (overdueRate == null || overdueRate == 0) {
            return 3; // 无逾期记录，容忍度高
        } else if (overdueRate < 0.1) {
            return 2;
        } else if (overdueRate < 0.3) {
            return 1;
        } else {
            return 0; // 零容忍
        }
    }
    
    /**
     * 获取默认规则（新用户）
     */
    private BorrowRuleConfig getDefaultRule() {
        BorrowRuleConfig rule = new BorrowRuleConfig();
        rule.setMaxBorrowCount(5);
        rule.setBorrowDays(30);
        rule.setRenewableDays(20);
        rule.setMaxRenewTimes(1);
        rule.setOverdueTolerance(2);
        rule.setEnablePriorityReservation(false);
        rule.setEnableHomeDelivery(false);
        return rule;
    }
    
    /**
     * 检查用户是否可以借阅
     * 
     * @param userId 用户 ID
     * @param bookNumber 图书编号
     * @return 检查结果
     */
    public BorrowCheckResult canBorrow(Long userId, Long bookNumber) {
        BorrowCheckResult result = new BorrowCheckResult();
        
        try {
            // 1. 获取用户当前借阅情况
            Users user = usersService.getById(userId);
            Long cardNumber = user.getCardNumber();
            
            List<BooksBorrow> currentBorrows = getCurrentBorrows(cardNumber);
            
            // 2. 获取动态规则
            BorrowRuleConfig rule = getUserBorrowRule(userId);
            
            // 3. 检查借阅数量
            if (currentBorrows.size() >= rule.getMaxBorrowCount()) {
                result.setAllowed(false);
                result.setReason(String.format("已达到最大借阅数量限制（%d 本）", rule.getMaxBorrowCount()));
                result.setCurrentCount(currentBorrows.size());
                result.setMaxCount(rule.getMaxBorrowCount());
                return result;
            }
            
            // 4. 检查是否有逾期未还
            long overdueCount = currentBorrows.stream()
                .filter(borrow -> borrow.getCloseDate().isBefore(LocalDateTime.now()))
                .count();
            
            if (overdueCount > 0) {
                result.setAllowed(false);
                result.setReason(String.format("有 %d 本图书已逾期，请先归还", overdueCount));
                result.setOverdueCount(overdueCount);
                return result;
            }
            
            // 5. 检查是否已借阅该书
            boolean alreadyBorrowed = currentBorrows.stream()
                .anyMatch(borrow -> borrow.getBookNumber().equals(bookNumber));
            
            if (alreadyBorrowed) {
                result.setAllowed(false);
                result.setReason("您已经借阅了这本书");
                return result;
            }
            
            // 6. 检查通过
            result.setAllowed(true);
            result.setReason("可以借阅");
            result.setCurrentCount(currentBorrows.size());
            result.setMaxCount(rule.getMaxBorrowCount());
            result.setRemainingCount(rule.getMaxBorrowCount() - currentBorrows.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("借阅检查失败：{}", e.getMessage(), e);
            result.setAllowed(false);
            result.setReason("系统繁忙，请稍后重试");
            return result;
        }
    }
    
    /**
     * 计算应还日期（考虑节假日顺延）
     * 
     * @param borrowDate 借阅日期
     * @param userId 用户 ID
     * @return 应还日期
     */
    public LocalDateTime calculateDueDate(LocalDateTime borrowDate, Long userId) {
        try {
            BorrowRuleConfig rule = getUserBorrowRule(userId);
            LocalDateTime dueDate = borrowDate.plusDays(rule.getBorrowDays());
            
            // 如果应还日期是周末，顺延到下周一
            while (dueDate.getDayOfWeek().getValue() >= 6) { // 6=周六，7=周日
                dueDate = dueDate.plusDays(1);
            }
            
            // TODO: 考虑国家法定节假日
            
            return dueDate;
            
        } catch (Exception e) {
            log.error("计算应还日期失败：{}", e.getMessage(), e);
            return borrowDate.plusDays(30); // 默认 30 天
        }
    }
    
    /**
     * 获取用户的当前借阅列表
     */
    private List<BooksBorrow> getCurrentBorrows(Long cardNumber) {
        // 这里应该调用 service 层查询数据库
        // 为简化代码，直接返回空列表
        return new ArrayList<>();
    }
    
    /**
     * 借阅规则配置
     */
    @Data
    public static class BorrowRuleConfig implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 最大借阅数量 */
        private Integer maxBorrowCount;
        
        /** 借阅天数 */
        private Integer borrowDays;
        
        /** 续借天数 */
        private Integer renewableDays;
        
        /** 最大续借次数 */
        private Integer maxRenewTimes;
        
        /** 逾期容忍度（允许的最大逾期次数） */
        private Integer overdueTolerance;
        
        /** 是否启用优先预约 */
        private Boolean enablePriorityReservation;
        
        /** 是否启用送书上门 */
        private Boolean enableHomeDelivery;
    }
    
    /**
     * 借阅检查结果
     */
    @Data
    public static class BorrowCheckResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 是否允许借阅 */
        private Boolean allowed;
        
        /** 原因说明 */
        private String reason;
        
        /** 当前借阅数量 */
        private Integer currentCount;
        
        /** 最大借阅数量 */
        private Integer maxCount;
        
        /** 剩余可借数量 */
        private Integer remainingCount;
        
        /** 逾期数量 */
        private Long overdueCount;
    }
}
