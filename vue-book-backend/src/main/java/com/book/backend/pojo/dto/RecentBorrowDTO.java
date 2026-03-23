package com.book.backend.pojo.dto;

import lombok.Data;

/**
 * 最近借阅 DTO（用于首页展示）
 */
@Data
public class RecentBorrowDTO {
    
    /**
     * 读者姓名
     */
    private String userName;
    
    /**
     * 图书名称
     */
    private String bookName;
    
    /**
     * 借阅时间
     */
    private String time;
}
