package com.book.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.mapper.ViolationMapper;
import com.book.backend.pojo.BookAdmins;
import com.book.backend.pojo.BooksBorrow;
import com.book.backend.pojo.Users;
import com.book.backend.pojo.Violation;
import com.book.backend.pojo.dto.BorrowData;
import com.book.backend.pojo.dto.ViolationDTO;
import com.book.backend.service.AdminsService;
import com.book.backend.service.BookAdminsService;
import com.book.backend.service.BooksBorrowService;
import com.book.backend.service.UsersService;
import com.book.backend.service.ViolationService;
import com.book.backend.utils.BorrowDateUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 程序员小白条
 * @description 针对表【t_violation】的数据库操作Service实现
 * @createDate 2023-02-06 16:31:20
 */
@Service
public class ViolationServiceImpl extends ServiceImpl<ViolationMapper, Violation>
        implements ViolationService {
    private BookAdminsService bookAdminsService;
    
    @Resource
    private AdminsService adminsService;
    
    @Resource
    private UsersService usersService;
    
    @Resource
    @Lazy
    private BooksBorrowService booksBorrowService;

    @Autowired
    public ViolationServiceImpl(@Lazy BookAdminsService bookAdminsService) {
        this.bookAdminsService = bookAdminsService;
    }

    /**
     * 1.先根据借阅证查询是否该用户在违章表中，如果没有直接返回
     * 2.先判断BasePage中传入的condition和query是否有空值
     * 3.如果有空值，根据借阅证查询所有的违章信息,进行分页查询
     * 3.1利用对象拷贝BeanUtils将分页构造器拷贝到DTO的分页构造器(忽略records)
     * 3.2根据图书管理员的id去获取用户名，然后封装到每一个DTO,返回DTOList
     * 3.3利用DTO分页构造器的setRecords重新赋值DTOList,将DTO分页构造器放入通用类的数据中,设置响应状态码和请求信息，返回前端
     * 4.如果没有空值,创建条件构造器，并根据借阅证编号、条件、内容查询
     * 5.获取违章数据，判断是否为空，如果为空，设置响应状态码404,并提示前端查询不到数据
     * 6.如果不为空,先放入原来的分页构造器
     * 6.1利用对象拷贝BeanUtils将分页构造器拷贝到DTO的分页构造器(忽略records)
     * 6.2根据图书管理员的id去获取用户名，然后封装到每一个DTO,返回DTOList
     * 6.3利用DTO分页构造器的setRecords重新赋值DTOList,将DTO分页构造器放入通用类的数据中,设置响应状态码和请求信息，返回前端
     */
    @Override
    public R<Page<ViolationDTO>> getViolationListByPage(BasePage basePage) {

        // 优先使用 cardNumber，如果没有则尝试从 userId 获取
        String cardNumber = basePage.getCardNumber();
        if (StringUtils.isBlank(cardNumber)) {
            // 尝试从 userId 获取借阅证号
            Integer userId = basePage.getUserId();
            if (userId != null) {
                LambdaQueryWrapper<Users> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(Users::getUserId, userId);
                Users user = usersService.getOne(userWrapper);
                if (user != null) {
                    cardNumber = String.valueOf(user.getCardNumber());
                }
            }
        }
        
        if (StringUtils.isBlank(cardNumber)) {
            return R.error("未找到用户 ID 或借阅证号");
        }
        
        QueryWrapper<Violation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("card_number", cardNumber);
        List<Violation> list = this.list(queryWrapper);
        if (list.isEmpty()) {
            return R.error("没有该用户的违章信息");
        }
        // 页码
        int pageNum = basePage.getPageNum();
        // 页数
        int pageSize = basePage.getPageSize();
        // 两个分页构造器
        Page<Violation> pageInfo = new Page<>(pageNum, pageSize);
        Page<ViolationDTO> dtoPage = new Page<>(pageNum, pageSize);
        // 创建返回结果类
        R<Page<ViolationDTO>> result = new R<>();
        // 有违章记录
        String condition = basePage.getCondition();
        String query = basePage.getQuery();
        if (StringUtils.isBlank(condition) || StringUtils.isBlank(query)) {
            LambdaQueryWrapper<Violation> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(Violation::getCardNumber, cardNumber).orderByAsc(Violation::getCreateTime);
            this.page(pageInfo, queryWrapper1);
            BeanUtils.copyProperties(pageInfo, dtoPage, "records");
            List<Violation> records = pageInfo.getRecords();
            List<ViolationDTO> listDTO = records.stream().map((item) -> {
                ViolationDTO violationDTO = new ViolationDTO();
                // 对象拷贝
                BeanUtils.copyProperties(item, violationDTO);
                // 获取图书管理员的id
                Integer violationAdminId = item.getViolationAdminId();
                // 根据id去查询用户名
                LambdaQueryWrapper<BookAdmins> queryWrapper2 = new LambdaQueryWrapper<>();
                queryWrapper2.eq(BookAdmins::getBookAdminId, violationAdminId);
                BookAdmins bookAdmins = bookAdminsService.getOne(queryWrapper2);
                if (bookAdmins != null) {
                    // 获取用户名
                    String username = bookAdmins.getUsername();
                    violationDTO.setViolationAdmin(username);
                }
                return violationDTO;
            }).collect(Collectors.toList());
            dtoPage.setRecords(listDTO);
            result.setData(dtoPage);
            result.setMsg("获取违章记录成功");
            result.setStatus(200);
            return result;
        }
        // 模糊查询
        queryWrapper.like(condition, query);
        Page<Violation> page = this.page(pageInfo, queryWrapper);
        if (page.getTotal() == 0) {
            return R.error("查询不到该用户的违章记录");
        }
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Violation> records2 = pageInfo.getRecords();
        List<ViolationDTO> violationDTOList = records2.stream().map((item) -> {
            ViolationDTO violationDTO2 = new ViolationDTO();
            BeanUtils.copyProperties(item, violationDTO2);
            // 获取图书管理员的id
            Integer violationAdminId = item.getViolationAdminId();
            // 根据id获取用户名
            LambdaQueryWrapper<BookAdmins> queryWrapper3 = new LambdaQueryWrapper<>();
            queryWrapper3.eq(BookAdmins::getBookAdminId, violationAdminId);
            BookAdmins bookAdmins2 = bookAdminsService.getOne(queryWrapper3);
            if (bookAdmins2 != null) {
                // 获取用户名
                String username = bookAdmins2.getUsername();
                violationDTO2.setViolationAdmin(username);
            }
            return violationDTO2;
        }).collect(Collectors.toList());
        dtoPage.setRecords(violationDTOList);
        result.setData(dtoPage);
        result.setMsg("获取违章信息成功");
        result.setStatus(200);
        return result;
    }

    /**
     * 1.分别获取5个时间节点，计算四个间隔之间的借阅量，也就是一周的借阅量
     * 2.时间格式化 然后封装到BorrowDate的日期数组中 再分别封装借阅量
     * 3.返回前端
     */
    @Override
    public R<BorrowData> getBorrowDate() {
        LocalDateTime now = LocalDateTime.now();
        String[] dateArray = BorrowDateUtil.getDateArray(now);
        LocalDateTime time1 = now.minusWeeks(1);
        LocalDateTime time2 = now.minusWeeks(2);
        LocalDateTime time3 = now.minusWeeks(3);
        LocalDateTime time4 = now.minusWeeks(4);
        
        // 从借阅记录表统计 (而不是违章表)
        LambdaQueryWrapper<BooksBorrow> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.between(BooksBorrow::getBorrowDate, time1, now);
        
        LambdaQueryWrapper<BooksBorrow> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.between(BooksBorrow::getBorrowDate, time2, time1);
        
        LambdaQueryWrapper<BooksBorrow> queryWrapper3 = new LambdaQueryWrapper<>();
        queryWrapper3.between(BooksBorrow::getBorrowDate, time3, time2);
        
        LambdaQueryWrapper<BooksBorrow> queryWrapper4 = new LambdaQueryWrapper<>();
        queryWrapper4.between(BooksBorrow::getBorrowDate, time4, time3);
        
        long count1 = booksBorrowService.count(queryWrapper1);
        long count2 = booksBorrowService.count(queryWrapper2);
        long count3 = booksBorrowService.count(queryWrapper3);
        long count4 = booksBorrowService.count(queryWrapper4);
        
        Integer[] borrowNumbers = new Integer[4];
        borrowNumbers[3] = (int) count1;
        borrowNumbers[1] = (int) count2;
        borrowNumbers[2] = (int) count3;
        borrowNumbers[0] = (int) count4;
        
        BorrowData borrowData = new BorrowData(dateArray, borrowNumbers);
        return R.success(borrowData, "获取借阅量成功");
    }
}




