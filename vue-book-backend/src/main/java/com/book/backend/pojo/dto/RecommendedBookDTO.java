package com.book.backend.pojo.dto;

import com.book.backend.pojo.Books;
import lombok.Data;

/**
 * 推荐图书数据传输对象
 * 
 * @author AI Assistant
 */
@Data
public class RecommendedBookDTO {
    
    /**
     * 图书信息
     */
    private Books book;
    
    /**
     * 推荐分数（相似度）
     */
    private Double score;
    
    /**
     * 推荐理由
     */
    private String reason;
    
    public RecommendedBookDTO() {
    }
    
    public RecommendedBookDTO(Books book, Double score) {
        this.book = book;
        this.score = score;
        this.reason = generateReason(book, score);
    }
    
    /**
     * 生成推荐理由
     */
    private String generateReason(Books book, Double score) {
        if (score >= 0.8) {
            return String.format("与您喜欢的图书高度相似（匹配度：%.1f%%）", score * 100);
        } else if (score >= 0.6) {
            return String.format("与您喜欢的图书较为相似（匹配度：%.1f%%）", score * 100);
        } else {
            return String.format("根据您的阅读兴趣推荐（匹配度：%.1f%%）", score * 100);
        }
    }
}
