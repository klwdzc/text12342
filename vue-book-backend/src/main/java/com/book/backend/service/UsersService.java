package com.book.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.pojo.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.backend.pojo.dto.UsersDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @author 程序员小白条
 * @description 针对表【t_users】的数据库操作Service
 * @createDate 2023-02-02 16:20:02
 */
public interface UsersService extends IService<Users> {
    /**
     * Rest接受参数 查询个人用户userId
     *
     * @param userId 用户id
     * @return R<Users>
     */
    R<Users> getUserByUserId(Integer userId);
    /**
     * 根据借阅证号查询用户信息
     * @param cardNumber 借阅证号
     * @return R<Users>
     */
    R<Users> getUserByCardNumber(Long cardNumber);


    /**
     * 修改密码
     *
     * @return R
     */
    R<String> updatePassword(Users users);

    /**
     * 借阅用户登录
     *
     * @param users 借阅者用户
     * @return 返回R通用数据
     */
    R login(Users users);

    /**
     * 根据用户id传给用户所需的信息
     * @param users 用户
     * @return R<Users>
     */
    R<Users> getUserData( Users users);


    /**
     * 获取借阅证列表(用户列表)
     *
     * @param basePage 用于接受模糊查询和分页构造的参数
     * @return R<Page < Users>>
     */
    R<Page<Users>> getStatementList( BasePage basePage);
    /**
     * 获取用户信息 根据用户id  用于回显借阅证
     *
     * @param userId 用户id
     * @return R<UsersDTO>
     */
    R<UsersDTO> getStatementByUserId( Integer userId);
    /**
     * 修改借阅证信息(用户信息)
     *
     * @param usersDTO 用户DTO
     * @return R
     */
    R<String> updateStatement( UsersDTO usersDTO);
    /**
     * 删除借阅证信息 根据用户 id
     *
     * @param userId 用户 id
     * @return R
     */
    R<String> deleteStatementByUserId( Integer userId);

    /**
     * 获取用户首页统计数据
     * @param userId 用户 ID
     * @return R<Map<String, Object>> 包含当前借阅、历史借阅、即将逾期、违章记录
     */
    R<Map<String, Object>> getDashboardStatistics(Integer userId);

    /**
     * 获取用户借阅排行榜（前 10 名）
     * @return R<List<UserRankDTO>> 用户借阅排行列表
     */
    R<List<com.book.backend.pojo.dto.UserRankDTO>> getUserRankList();
}
