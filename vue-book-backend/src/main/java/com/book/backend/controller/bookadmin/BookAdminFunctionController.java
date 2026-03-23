package com.book.backend.controller.bookadmin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Notice;
import com.book.backend.pojo.Violation;
import com.book.backend.pojo.dto.BooksBorrowDTO;
import com.book.backend.pojo.dto.RecentBorrowDTO;
import com.book.backend.pojo.dto.ViolationDTO;
import com.book.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author 程序员小白条
 */
@RestController
@RequestMapping("/bookadmin")
public class BookAdminFunctionController {
    @Resource
    private BookAdminsService bookAdminsService;

    @Resource
    private BooksService booksService;
    @Resource
    private BooksBorrowService booksBorrowService;

    @Resource
    private NoticeService noticeService;


    /**
     * 借阅图书根据借阅证号和图书编号
     *
     * @return R
     */
    @PostMapping("borrow_book")
    @Operation(summary = "根据借阅证号和图书编号借阅图书")
    public R<String> borrowBookByCardNumberAndBookNumber(@RequestBody BooksBorrowDTO booksBorrowDTO) {
        return booksService.borrowBookByCardNumberAndBookNumber(booksBorrowDTO);
    }

    /**
     * 查看图书是否有逾期(查看是否借出)
     *
     * @param bookNumber 图书编号
     * @return R
     */
    @GetMapping("query_book/{bookNumber}")
    @Operation(summary = "查看图书是否有逾期")
    public R<String> queryBookExpireByBookNumber(@PathVariable("bookNumber") Long bookNumber) {
        return booksService.queryBookExpireByBookNumber(bookNumber);
    }

    /**
     * 获取图书逾期信息
     *
     * @param bookNumber 图书编号
     * @return R<Violation>
     */
    @GetMapping("query_expire/{bookNumber}")
    @Operation(summary = "获取图书逾期信息")
    public R<ViolationDTO> queryExpireInformationByBookNumber(@PathVariable("bookNumber") Long bookNumber) {
        return booksBorrowService.queryExpireInformationByBookNumber(bookNumber);
    }

    /**
     * 归还图书
     *
     * @param violation 违章表
     * @return R
     */
    @PostMapping("return_book")
    @Operation(summary = "归还图书")
    public R<String> returnBook(@RequestBody Violation violation) {
        return booksBorrowService.returnBook(violation);
    }

    /**
     * 获取还书报表
     *
     * @param basePage 接受分页构造器和模糊查询的传参
     * @return R<Page < BooksBorrow>>
     */
    @PostMapping("get_return_statement")
    @Operation(summary = "获取还书报表")
    public R<Page<BooksBorrow>> getReturnStatement(@RequestBody BasePage basePage) {
        return booksBorrowService.getReturnStatement(basePage);
    }

    /**
     * 获取借书报表
     *
     * @param basePage 接受分页构造器和模糊查询的传参
     * @return R<Page < ViolationDTO>>
     */
    @PostMapping("get_borrow_statement")
    @Operation(summary = "获取借书报表")
    public R<Page<ViolationDTO>> getBorrowStatement(@RequestBody BasePage basePage) {
        return bookAdminsService.getBorrowStatement(basePage);
    }

    /**
     * 获取公告列表
     *
     * @return R<Notice>
     */
    @PostMapping("get_noticelist")
    @Operation(summary = "获取公告列表")
    public R<Page<Notice>> getNoticeList(@RequestBody BasePage basePage) {
        return noticeService.getNoticeList(basePage);
    }

    /**
     * 添加公告
     *
     * @param notice 公告
     * @return R<String>
     */
    @PostMapping("add_notice")
    @Operation(summary = "添加公告")
    public R<String> addNotice(@RequestBody Notice notice) {
        return noticeService.addNotice(notice);
    }

    /**
     * 删除公告根据指定的id
     *
     * @param noticeId 公告id
     * @return R
     */
    @GetMapping("delete_notice/{noticeId}")
    @Operation(summary = "删除公告根据指定的 id")
    public R<String> deleteNoticeById(@PathVariable("noticeId") Integer noticeId) {
        return noticeService.deleteNoticeById(noticeId);
    }

    /**
     * 根据指定id获取公告
     *
     * @param noticeId 公告id
     * @return R<Notice>
     */
    @GetMapping("get_notice/{noticeId}")
    @Operation(summary = "根据指定 id 获取公告")
    public R<Notice> getNoticeByNoticeId(@PathVariable("noticeId") Integer noticeId) {
        return noticeService.getNoticeByNoticeId(noticeId);
    }

    /**
     * 更新公告根据公告 id
     *
     * @param noticeId 公告 id
     * @param notice   公告
     * @return R
     */
    @PutMapping("update_notice/{noticeId}")
    @Operation(summary = "更新公告根据公告 id")
    public R<String> updateNoticeByNoticeId(@PathVariable("noticeId") Integer noticeId, @RequestBody Notice notice) {
        return noticeService.updateNoticeByNoticeId(noticeId, notice);
    }

    /**
     * 获取图书管理员首页统计数据
     *
     * @param bookAdminId 图书管理员 ID（从 Token 中获取）
     * @return R<Map<String, Object>> 包含今日借阅、今日归还、待处理借阅、逾期图书
     */
    @GetMapping("get_dashboard_statistics")
    @Operation(summary = "获取图书管理员首页统计数据")
    public R<Map<String, Object>> getDashboardStatistics(@RequestParam("bookAdminId") Integer bookAdminId) {
        return bookAdminsService.getDashboardStatistics(bookAdminId);
    }
    
    /**
     * 获取最近借阅记录（用于首页展示）
     *
     * @param limit 返回数量限制
     * @return R<List<RecentBorrowDTO>> 最近借阅记录列表
     */
    @GetMapping("get_recent_borrows")
    @Operation(summary = "获取最近借阅记录")
    public R<List<RecentBorrowDTO>> getRecentBorrows(@RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return booksBorrowService.getRecentBorrows(limit);
    }
}
