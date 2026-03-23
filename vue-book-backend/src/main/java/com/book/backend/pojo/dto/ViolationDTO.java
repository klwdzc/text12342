package com.book.backend.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author 程序员小白条
 *  DTO用于将管理员昵称传输
 */
@Data
public class ViolationDTO implements Serializable {
    /**
     * 违章表唯一标识
     */
    private Integer violationId;
    /**
     * 借阅证编号
     */
    private Long cardNumber;
    /**
     * 图书编号
     */
    private Long bookNumber;
    /**
     * 借阅日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime borrowDate;
    /**
     * 截止日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime closeDate;
    /**
     * 归还日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime returnDate;
    /**
     * 违章信息
     */
    private String violationMessage;
    /**
     * 违章信息管理员的id
     */
    private Integer violationAdminId;
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
    private String bookName;
    private String bookAuthor;
    private String bookType;
    private String bookLibrary;
    private String bookLocation;
    private String bookStatus;
    /**
     * 应还日期（前端展示字段）
     */
    private LocalDateTime shouldReturnDate;

    public Long getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Long cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getBookNumber() {
        return bookNumber;
    }

    public void setBookNumber(Long bookNumber) {
        this.bookNumber = bookNumber;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDateTime closeDate) {
        this.closeDate = closeDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public String getViolationMessage() {
        return violationMessage;
    }

    public void setViolationMessage(String violationMessage) {
        this.violationMessage = violationMessage;
    }

    public Integer getViolationAdminId() {
        return violationAdminId;
    }

    public void setViolationAdminId(Integer violationAdminId) {
        this.violationAdminId = violationAdminId;
    }

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
}
