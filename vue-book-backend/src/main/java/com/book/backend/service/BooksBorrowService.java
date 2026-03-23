package com.book.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Violation;
import com.book.backend.pojo.dto.BorrowTypeDTO;
import com.book.backend.pojo.dto.CurrentBorrowDTO;
import com.book.backend.pojo.dto.RecentBorrowDTO;
import com.book.backend.pojo.dto.ViolationDTO;

import java.util.List;

/**
 * @author 程序员小白条
 * @description 针对表【t_books_borrow】的数据库操作Service
 * @createDate 2023-02-05 18:53:07
 */
public interface BooksBorrowService extends IService<BooksBorrow> {
    /**
     * 借阅信息查询 根据用户id，条件及其内容
     *
     * @param basePage 用于接受分页传参和用户id
     * @return R<Page < BooksBorrow>>
     */
    R<Page<BooksBorrow>> getBookBorrowPage(BasePage basePage);

    /**
     * 获取图书逾期信息
     *
     * @param bookNumber 图书编号
     * @return R<Violation>
     */
    R<ViolationDTO> queryExpireInformationByBookNumber(Long bookNumber);

    /**
     * 归还图书
     *
     * @param violation 违章表
     * @return R
     */
    R<String> returnBook(Violation violation);
    /**
     * 获取还书报表
     *
     * @param basePage 接受分页构造器和模糊查询的传参
     * @return R<Page < BooksBorrow>>
     */
    R<Page<BooksBorrow>> getReturnStatement(BasePage basePage);

    /**
     * 统计图书分类借阅情况（使用 SQL GROUP BY）
     * @return 分类借阅统计列表
     */
    List<BorrowTypeDTO> getBorrowTypeStatistic();
    
    /**
     * 获取最近借阅记录（用于首页展示）
     * @param limit 返回数量限制
     * @return 最近借阅记录列表
     */
    R<List<RecentBorrowDTO>> getRecentBorrows(Integer limit);
    
    /**
     * 获取用户当前借阅记录（用于首页展示）
     * @param userId 用户 ID
     * @return 当前借阅记录列表
     */
    R<List<CurrentBorrowDTO>> getCurrentBorrows(Integer userId);
}
