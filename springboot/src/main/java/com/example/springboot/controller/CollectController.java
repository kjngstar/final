package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Collect;
import com.example.springboot.service.ICollectService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/collect")
public class CollectController {

    @Resource
    private ICollectService collectService;

    @PostMapping
    public Result save(@RequestBody Collect collect) {
        Account currentUser = TokenUtils.getCurrentUser();
        try {
            collect.setUserId(currentUser.getId());
            collectService.saveOrUpdate(collect);
        }catch (Exception e){
            LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Collect::getUserId,currentUser.getId());
            wrapper.eq(Collect::getItemId,collect.getItemId());
            collectService.remove(wrapper);
            return Result.error("605","已取消收藏");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId,currentUser.getId());
        wrapper.eq(Collect::getItemId,id);
        collectService.remove(wrapper);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(collectService.removeByIds(ids));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Collect::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Collect::getUserId, keyword);
        }

        return Result.success(collectService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

