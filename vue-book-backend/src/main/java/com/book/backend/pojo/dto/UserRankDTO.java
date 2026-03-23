package com.book.backend.pojo.dto;

import lombok.Data;

/**
 * 用户借阅排行 DTO
 */
@Data
public class UserRankDTO {
    
    /**
     * 借阅证号
     */
    private Long cardNumber;
    
    /**
     * 读者姓名
     */
    private String cardName;
    
    /**
     * 借阅数量
     */
    private Integer borrowCount;
    
    /**
     * 用户所属学院
     */
    private String college;
}
