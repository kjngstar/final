package com.example.springboot.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Goods;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.IGoodsService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Resource
    private IGoodsService goodsService;

    @Resource
    private ICollectService collectService;


    @PostMapping
    public Result save(@RequestBody Goods goods) {
        if (goods.getId() == null) {
            goods.setUserId(TokenUtils.getCurrentUser().getId());
            goods.setStatus("已上架");
            goods.setNum(0);
        }
        return Result.success(goodsService.saveOrUpdate(goods));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(goodsService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(goodsService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(goodsService.list());
    }

    @GetMapping("/user/{id}")
    public Result findUser(@PathVariable String id) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Goods::getId);
        wrapper.eq(Goods::getUserId,id);
        return Result.success(goodsService.list(wrapper));
    }

    @GetMapping("/front")
    public Result findFront() {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Goods::getNum);
        wrapper.eq(Goods::getStatus,"已上架");
        return Result.success(goodsService.list( wrapper.last("LIMIT 12")));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        Goods goods = goodsService.getById(id);
        goods.setNum(goods.getNum()+1);
        goods.setIsCollected(false);
        // 检查当前用户是否已收藏该服务
        Account currentUser = TokenUtils.getCurrentUser();
        if (ObjectUtil.isNotNull(currentUser)){
            LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Collect::getItemId, id);
            wrapper.eq(Collect::getUserId, currentUser.getId());
            goods.setIsCollected(collectService.getOne(wrapper) != null);
        }
        goodsService.updateById(goods);
        return Result.success(goods);
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Goods::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Goods::getName, keyword);
        }

        return Result.success(goodsService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @GetMapping("/collect/page")
    public Result findCollectPage(@RequestParam(defaultValue = "") String keyword,
                                  @RequestParam Integer pageNum,
                                  @RequestParam Integer pageSize) {

        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, TokenUtils.getCurrentUser().getId());
        List<Integer> ids = collectService.list(wrapper).stream().map(Collect::getItemId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(ids)) return Result.success(collectService.page(new Page<>(),wrapper));
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotEmpty(keyword),Goods::getName,keyword);
        queryWrapper.in(Goods::getId,ids);
        return Result.success(goodsService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @GetMapping("/front/page")
    public Result findFrontPage(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") Integer typeId,
                                @RequestParam(defaultValue = "all") String sortBy,
                                @RequestParam Integer pageNum,
                                @RequestParam Integer pageSize) {

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Goods::getStatus,"已上架");
        if (StrUtil.equals(sortBy,"all")) {
            queryWrapper.orderByDesc(Goods::getId);
        } else if (StrUtil.equals(sortBy,"new")) {
            queryWrapper.orderByDesc(Goods::getDate);
        } else if (StrUtil.equals(sortBy,"price")) {
            queryWrapper.orderByAsc(Goods::getPrice);
        }
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Goods::getName, keyword);
        }
        if (typeId!=0) {
            queryWrapper.eq(Goods::getTypeId, typeId);
        }
        return Result.success(goodsService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

