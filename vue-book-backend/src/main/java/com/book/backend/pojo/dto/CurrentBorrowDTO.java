package com.book.backend.pojo.dto;

import lombok.Data;

/**
 * 当前借阅 DTO（用于用户首页展示）
 */
@Data
public class CurrentBorrowDTO {
    
    /**
     * 图书名称
     */
    private String bookName;
    
    /**
     * 应还日期
     */
    private String dueDate;
    
    /**
     * 类型：normal-正常，warning-即将逾期，danger-已逾期
     */
    private String type;
}
