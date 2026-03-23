package com.book.backend.pojo.dto;

import com.book.backend.pojo.Violation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author 程序员小白条
 *  DTO用于将管理员昵称传输
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ViolationDTO extends Violation implements Serializable{
//    /**
//     * 违章列表
//     */
//    public Violation violation;
    /**
     * 违章信息处理人的姓名
     */
    public String violationAdmin;
    /**
     * 还剩多少天逾期
     */
    public long expireDays;
    /**
     * 读者姓名
     */
    private String cardName;
    /**
     * 是否逾期
     */
    private Boolean isOverdue;
    /**
     * 逾期天数
     */
    private Long overdueDays;
    /**
     * 罚款金额
     */
    private Double fineAmount;
    /**
     * 图书信息
     */
    /**
     * 应还日期（前端展示字段）
     */
    private LocalDateTime shouldReturnDate;

    /**
     * 备注信息
     */
    private String remark;



    private String bookName;
    private String bookAuthor;
    private String bookType;
    private String bookLibrary;
    private String bookLocation;
    private String bookStatus;


    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public Boolean getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(Boolean overdue) {
        isOverdue = overdue;
    }

    public Long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Long overdueDays) {
        this.overdueDays = overdueDays;
    }

    public Double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(Double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getBookType() {
        return bookType;
    }

    public void setBookType(String bookType) {
        this.bookType = bookType;
    }

    public String getBookLibrary() {
        return bookLibrary;
    }

    public void setBookLibrary(String bookLibrary) {
        this.bookLibrary = bookLibrary;
    }

    public String getBookLocation() {
        return bookLocation;
    }

    public void setBookLocation(String bookLocation) {
        this.bookLocation = bookLocation;
    }

    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }

    public LocalDateTime getShouldReturnDate() {
        return shouldReturnDate;
    }

    public void setShouldReturnDate(LocalDateTime shouldReturnDate) {
        this.shouldReturnDate = shouldReturnDate;
    }
    public String getRemark() {
        return remark;
    }
}