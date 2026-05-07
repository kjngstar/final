package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.Type;
import com.example.springboot.service.IGoodsService;
import com.example.springboot.service.ITypeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/type")
public class TypeController {

    @Resource
    private ITypeService typeService;
    @Resource
    private IGoodsService goodsService;


    @PostMapping
    public Result save(@RequestBody Type type) {
        return Result.success(typeService.saveOrUpdate(type));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(typeService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(typeService.removeByIds(ids));
    }

    @GetMapping("/front")
    public Result findFront() {
        LambdaQueryWrapper<Type> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Type::getStatus,true)
                .last("limit 4");
        List<Type> typeList = typeService.list(wrapper);
        for (Type type : typeList) {
            LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Goods::getTypeId,type.getId())
                        .eq(Goods::getStatus,"已上架")
                         .orderByDesc(Goods::getNum)
                        .last("limit 3");
            List<Goods> goodsList = goodsService.list(queryWrapper);
            type.setGoodsList(goodsList);
        }

        return Result.success(typeList);
    }

    @GetMapping("/hot")
    public Result findAllHot() {
        LambdaQueryWrapper<Type> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Type::getStatus,true);
        wrapper.last("limit 4");
        List<Type> List = typeService.list(wrapper);
        return Result.success();
    }

    @GetMapping
    public Result findAll() {
        return Result.success(typeService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(typeService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Type> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Type::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Type::getName, keyword);
        }

        return Result.success(typeService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

