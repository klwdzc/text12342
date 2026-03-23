package com.book.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.backend.common.BasePage;
import com.book.backend.common.R;
import com.book.backend.pojo.BookType;
import com.book.backend.service.BookTypeService;
import com.book.backend.mapper.BookTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author 程序员小白条
* @description 针对表【t_book_type】的数据库操作Service实现
* @createDate 2023-02-04 18:51:24
*/
@Service
public class BookTypeServiceImpl extends ServiceImpl<BookTypeMapper, BookType>
    implements BookTypeService{

    @Override
    public R<List<BookType>> getBookTypeList() {
        List<BookType> list = this.list();
        R<List<BookType>> result = new R<>();
        if (list.isEmpty()) {
            return R.error("获取书籍分类失败");
        }
        result.setData(list);
        result.setMsg("获取书籍分类成功");
        result.setStatus(200);
        return result;
    }
    /**
     * 1.获取页码和页数
     * 2.调用服务的 page 分页 判断是否为空
     * 3.如果不为空，存入数据，200 响应状态吗，请求成功信息
     */
    @Override
    public R<Page<BookType>> getBookTypeListByPage(BasePage basePage) {
        try {
            // 页码
            int pageNum = basePage.getPageNum();
            // 页数
            int pageSize = basePage.getPageSize();
            // 内容
            String query = basePage.getQuery();
            // 条件
            String condition = basePage.getCondition();
                
            Page<BookType> pageInfo = new Page<>(pageNum, pageSize);
            R<Page<BookType>> result = new R<>();
                
            if (StringUtils.isBlank(condition) || StringUtils.isBlank(query)) {
                // 无条件查询所有
                Page<BookType> page = this.page(pageInfo);
                if (page.getTotal() == 0) {
                    result.setData(new Page<>());
                    result.setStatus(200);
                    result.setMsg("书籍分类列表为空");
                    return result;
                }
                result.setData(pageInfo);
                result.setMsg("获取书籍分类列表成功");
                result.setStatus(200);
                return result;
            }
                
            // 将前端传来的驼峰命名转换为数据库下划线命名
            String dbColumn = humpToUnderline(condition);
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BookType> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.like(dbColumn, query);
            Page<BookType> page = this.page(pageInfo, queryWrapper);
                
            if (page.getTotal() == 0) {
                result.setData(new Page<>());
                result.setStatus(200);
                result.setMsg("书籍分类列表为空");
                return result;
            }
            result.setData(pageInfo);
            result.setMsg("获取书籍分类列表成功");
            result.setStatus(200);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("获取书籍分类列表失败：" + e.getMessage());
        }
    }
        
    /**
     * 驼峰命名转下划线命名
     * typeName -> type_name
     */
    private String humpToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
    /**
     * 1.调用服务插入书籍类别
     * 2.判断是否成功
     * 3.成功返回响应状态码和请求信息
     */
    @Override
    public R<String> addBookType(BookType bookType) {
        boolean save = this.save(bookType);
        if (!save) {
            return R.error("添加书籍类别失败");
        }
        return R.success(null,"添加书籍类型成功");
    }
    /**
     * 1.根据typeId查询
     * 2.判断是否为空 不为空返回前端
     */
    @Override
    public R<BookType> getBookTypeByTypeId(Integer typeId) {
        BookType type = this.getById(typeId);
        if (type == null) {
            return R.error("获取书籍类别失败");
        }
        R<BookType> result = new R<>();
        result.setData(type);
        result.setStatus(200);
        result.setMsg("获取书籍类别成功");
        return result;
    }
    /**
     * 1.判断空参数
     * 2.更新书籍 判断是否成功
     * 3.成功->200 失败->错误信息
     */
    @Override
    public R<String> updateBookType(BookType bookType) {
        String typeContent = bookType.getTypeContent();
        String typeName = bookType.getTypeName();
        if (StringUtils.isBlank(typeContent) || StringUtils.isBlank(typeName)) {
            return R.error("更新书籍类别失败");
        }
        boolean update = this.updateById(bookType);
        if (!update) {
            return R.error("更新书籍类别失败");
        }

        return R.success(null,"更新书籍类别成功");
    }
    /**
     * 1.先根据typeId查询是否有此书籍类别
     * 2.调用服务，删除书籍类别，判断是否成功
     * 3.成功->200 失败->错误信息
     */
    @Transactional
    @Override
    public R<String> deleteBookTypeByTypeId(Integer typeId) {
        BookType bookType = this.getById(typeId);
        if (bookType == null) {
            return R.error("删除书籍类别失败");
        }
        boolean remove = this.removeById(typeId);
        if (!remove) {
            return R.error("删除书籍类别失败");
        }
        return R.success(null,"删除书籍类别成功");
    }
}




