package com.book.backend.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.pojo.*;
import com.book.backend.pojo.dto.CommentDTO;
import com.book.backend.pojo.dto.CurrentBorrowDTO;
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
@RequestMapping("/user")
public class UserFunctionController {
    @Resource
    private BooksService booksService;
    @Resource
    private BookRuleService bookRuleService;
    @Resource
    private NoticeService noticeService;
    @Resource
    private UsersService usersService;
    @Resource
    private BooksBorrowService booksBorrowService;
    @Resource
    private ViolationService violationService;

    @Resource
    private CommentService commentService;
    @Resource
    private AiIntelligentService aiIntelligentService;

    /**
     * 图书查询 分页和条件查询 (模糊查询)
     *
     * @param basePage 用于接受分页传参
     * @return R<Page < Books>>
     */
    @PostMapping("/search_book_page")
    @Operation(summary = "图书查询 分页和条件查询")
    public R<Page<Books>> searchBookPage(@RequestBody BasePage basePage) {
        return booksService.searchBookPage(basePage);
    }

    /**
     * 读者规则查询
     *
     * @return R<List < BookRule>>
     */
    @GetMapping("get_rulelist")
    @Operation(summary = "读者规则查询")
    public R<List<BookRule>> getRuleList() {
        return bookRuleService.getRuleList();
    }

    /**
     * 查询公告信息
     *
     * @return R<List < Notice>>
     */
    @GetMapping("get_noticelist")
    @Operation(summary = "查询公告信息")
    public R<List<Notice>> getNoticeList() {
        return noticeService.getNoticeList();
    }

    /**
     * Rest接受参数 查询个人用户userId
     *
     * @param userId 用户id
     * @return R<Users>
     */
    @GetMapping("get_information/{userId}")
    @Operation(summary = "查询个人用户")
    public R<Users> getUserByUserId(@PathVariable("userId") Integer userId) {
        return usersService.getUserByUserId(userId);
    }


    /**
     * 根据借阅证号查询用户信息
     *
     * @param cardNumber 借阅证号
     * @return R<Users>
     */
    @GetMapping("get_user_by_card_number/{cardNumber}")
    @Operation(summary = "根据借阅证号查询用户")
    public R<Users> getUserByCardNumber(@PathVariable("cardNumber") Long cardNumber) {
        return usersService.getUserByCardNumber(cardNumber);
    }


    /**
     * 修改密码
     *
     * @return R
     */
    @PostMapping("update_password")
    @Operation(summary = "修改密码")
    public R<String> updatePassword(@RequestBody Users users) {
        return usersService.updatePassword(users);
    }

    /**
     * 借阅信息查询 根据用户id，条件及其内容
     *
     * @param basePage 用于接受分页传参和用户id
     * @return R<Page < BooksBorrow>>
     */
    @PostMapping("get_bookborrow")
    @Operation(summary = "借阅信息查询")
    public R<Page<BooksBorrow>> getBookBorrowPage(@RequestBody BasePage basePage) {
        return booksBorrowService.getBookBorrowPage(basePage);
    }

    /**
     * 查询违章信息(借阅证)
     *
     * @param basePage 获取前端的分页参数，条件和内容，借阅证
     * @return R<Page < ViolationDTO>>
     */
    @PostMapping("get_violation")
    @Operation(summary = "查询违章信息")
    public R<Page<ViolationDTO>> getViolationListByPage(@RequestBody BasePage basePage) {
        return violationService.getViolationListByPage(basePage);
    }

    /**
     * 获取弹幕列表
     *
     * @return R<Comment>
     */
    @GetMapping("get_commentlist")
    @Operation(summary = "获取弹幕列表")
    public R<List<CommentDTO>> getCommentList() {
        return commentService.getCommentList();

    }

    /**
     * 添加弹幕
     *
     * @return R
     */
    @PostMapping("add_comment")
    @Operation(summary = "添加弹幕")
    public R<String> addComment(@RequestBody CommentDTO commentDTO) {
        return commentService.addComment(commentDTO);
    }

    /**
     * 调用AI模型，获取数据库中有的，并且推荐图书给用户
     * @param aiIntelligent AI实体类
     * @return R<String>
     */
    @PostMapping("ai_intelligent")
    @Operation(summary = "推荐图书")
    public R<String> aiRecommend(@RequestBody AiIntelligent aiIntelligent){
        return aiIntelligentService.getGenResult(aiIntelligent);
    }

    /**
     * 根据用户 ID 获取该用户和 AI 聊天的最近的五条消息
     * @param userId 用户 id
     * @return R<List<AiIntelligent>>
     */
    @GetMapping("ai_list_information/{userId}")
    @Operation(summary = "获取该用户和 AI 聊天的最近的五条消息")
    public R<List<AiIntelligent>> getAiInformationByUserId(@PathVariable("userId") Long userId){
        return aiIntelligentService.getAiInformationByUserId(userId);
    }

    /**
     * 获取用户首页统计数据
     *
     * @param userId 用户 ID（从 Token 中获取）
     * @return R<Map<String, Object>> 包含当前借阅、历史借阅、即将逾期、违章记录
     */
    @GetMapping("get_dashboard_statistics")
    @Operation(summary = "获取用户首页统计数据")
    public R<Map<String, Object>> getDashboardStatistics(@RequestParam("userId") Integer userId) {
        return usersService.getDashboardStatistics(userId);
    }
    
    /**
     * 获取用户当前借阅记录（用于首页展示）
     *
     * @param userId 用户 ID
     * @return R<List<CurrentBorrowDTO>> 当前借阅记录列表
     */
    @GetMapping("get_current_borrows")
    @Operation(summary = "获取用户当前借阅记录")
    public R<List<CurrentBorrowDTO>> getCurrentBorrows(@RequestParam("userId") Integer userId) {
        return booksBorrowService.getCurrentBorrows(userId);
    }
}
