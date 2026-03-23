package com.book.backend.mapper;

import com.book.backend.pojo.Books;
import com.book.backend.pojo.dto.BookHotDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 程序员小白条
* @description 针对表【t_books】的数据库操作 Mapper
* @createDate 2023-02-04 18:07:43
* @Entity com.book.backend.pojo.Books
*/
public interface BooksMapper extends BaseMapper<Books> {

    /**
     * 获取图书热度排行榜（前 10 名）
     * @return 图书热度排行列表
     */
    @Select("SELECT b.book_number AS bookNumber, " +
            "b.book_name AS bookName, " +
            "b.book_author AS bookAuthor, " +
            "b.book_type AS bookType, " +
            "COUNT(bb.borrow_id) AS borrowCount " +
            "FROM t_books b " +
            "LEFT JOIN t_books_borrow bb ON b.book_number = bb.book_number " +
            "GROUP BY b.book_number, b.book_name, b.book_author, b.book_type " +
            "ORDER BY borrowCount DESC " +
            "LIMIT 10")
    List<BookHotDTO> getBookHotList();

}




