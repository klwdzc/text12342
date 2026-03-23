package com.book.backend.pojo.dto;

import lombok.Data;

/**
 * 图书热度排行 DTO
 */
@Data
public class BookHotDTO {
    
    /**
     * 图书编号
     */
    private Long bookNumber;
    
    /**
     * 图书名称
     */
    private String bookName;
    
    /**
     * 作者
     */
    private String bookAuthor;
    
    /**
     * 借阅次数
     */
    private Integer borrowCount;
    
    /**
     * 图书分类
     */
    private String bookType;
}
