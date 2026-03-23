package com.book.backend.mapper;

import com.book.backend.pojo.Users;
import com.book.backend.pojo.dto.UserRankDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 程序员小白条
* @description 针对表【t_users】的数据库操作 Mapper
* @createDate 2023-02-02 16:20:02
* @Entity com.book.backend.pojo.Users
*/
@Mapper
public interface UsersMapper extends BaseMapper<Users> {

    /**
     * 获取用户借阅排行榜（前 10 名）
     * @return 用户借阅排行列表
     */
    @Select("SELECT u.card_number AS cardNumber, " +
            "u.card_name AS cardName, " +
            "u.college, " +
            "COUNT(bb.borrow_id) AS borrowCount " +
            "FROM t_users u " +
            "LEFT JOIN t_books_borrow bb ON u.card_number = bb.card_number " +
            "GROUP BY u.card_number, u.card_name, u.college " +
            "ORDER BY borrowCount DESC " +
            "LIMIT 10")
    List<UserRankDTO> getUserRankList();

}




