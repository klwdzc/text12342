package com.book.backend.mapper;

import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.dto.BorrowTypeDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 程序员小白条
* @description 针对表【t_books_borrow】的数据库操作 Mapper
* @createDate 2023-02-05 18:53:07
* @Entity com.book.backend.pojo.BooksBorrow
*/
public interface BooksBorrowMapper extends BaseMapper<BooksBorrow> {

    /**
     * 统计图书分类借阅情况（使用 SQL GROUP BY）
     * @return 分类借阅统计列表
     */
    @Select("SELECT b.book_type AS bookTypes, COUNT(*) AS borrowNumbers " +
            "FROM t_books_borrow bb " +
            "JOIN t_books b ON bb.book_number = b.book_number " +
            "GROUP BY b.book_type " +
            "ORDER BY borrowNumbers DESC")
    List<BorrowTypeDTO> getBorrowTypeStatistic();

}




