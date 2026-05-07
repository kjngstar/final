package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Chat;
import com.example.springboot.service.IChatService;
import com.example.springboot.service.IUserService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chatAdmin")
public class ChatAdminController {

    @Resource
    private IChatService chatService;

    @Resource
    private IUserService userService;

    /**
     * 保存消息：强制从 Token 获取发送者 ID，确保身份绝对准确
     */
    @PostMapping("/save")
    public Result save(@RequestBody Chat chat) {
        Account currentUser = TokenUtils.getCurrentUser();
        // 🔥 核心安全：无论前端传什么，后台强制校准为当前登录人 ID
        chat.setFromUserId(currentUser.getId());

        if (chat.getTime() == null) {
            chat.setTime(DateUtil.now());
        }
        chatService.save(chat);
        return Result.success();
    }

    /**
     * 获取待接入列表：仅限人工申请标记（tp='uertoadmin'）且未读
     */
    @GetMapping("/getApplyList")
    public Result getApplyList() {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chat::getTp, "uertoadmin");
        wrapper.eq(Chat::getIsRead, false);
        wrapper.orderByDesc(Chat::getTime);
        return Result.success(chatService.list(wrapper));
    }

    /**
     * 清除申请状态：管理员打开窗口或点击接入时调用
     */
    @GetMapping("/clearApply")
    public Result clearApply(@RequestParam Integer userId) {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chat::getFromUserId, userId);
        wrapper.eq(Chat::getTp, "uertoadmin");
        wrapper.eq(Chat::getIsRead, false);

        List<Chat> list = chatService.list(wrapper);
        if (!list.isEmpty()) {
            list.forEach(item -> item.setIsRead(true));
            chatService.updateBatchById(list);
        }
        return Result.success();
    }

    /**
     * 获取人工对话历史
     */
    @GetMapping("/history")
    public Result history(@RequestParam Integer userId, @RequestParam Integer adminId) {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        // 💡 只筛选人工客服对话（带有 tp 标记的消息）
        wrapper.isNotNull(Chat::getTp);
        wrapper.and(w ->
                w.and(q -> q.eq(Chat::getFromUserId, userId).eq(Chat::getToUserId, adminId))
                        .or(q -> q.eq(Chat::getFromUserId, adminId).eq(Chat::getToUserId, userId))
        ).orderByAsc(Chat::getTime);
        return Result.success(chatService.list(wrapper));
    }

    /**
     * 加载已服务的联系人列表
     * 💡 受益于影子账号方案：此处直接查询 userService，即可同时获得普通用户和管理员的信息。
     */
    @GetMapping("/getServedUsers")
    public Result getServedUsers() {
        Account account = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Chat::getTp); // 仅统计人工服务产生的联系人
        wrapper.and(w -> w.eq(Chat::getFromUserId, account.getId()).or().eq(Chat::getToUserId, account.getId()));

        List<Chat> chatList = chatService.list(wrapper);
        // 提取所有对方的 ID（可能是用户 ID，也可能是其他管理员 ID）
        Set<Integer> allIds = chatList.stream()
                .map(item -> item.getFromUserId().equals(account.getId()) ? item.getToUserId() : item.getFromUserId())
                .collect(Collectors.toSet());

        if (allIds.isEmpty()) return Result.success(new ArrayList<>());

        // 💡 影子账号确保了所有 ID 都能在 sys_user 表中找到对应头像和昵称
        return Result.success(userService.listByIds(allIds));
    }

    /**
     * 获取 AI 对话历史背景
     * 管理员在人工服务前，查看用户之前与 AI 聊了什么（假设 AI 的 ID 为 0）。
     */
    @GetMapping("/aiHistory")
    public Result aiHistory(@RequestParam Integer userId) {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Chat::getTp); // AI 对话不带 tp 标记
        wrapper.and(w ->
                w.and(q -> q.eq(Chat::getFromUserId, userId).eq(Chat::getToUserId, 0))
                        .or(q -> q.eq(Chat::getFromUserId, 0).eq(Chat::getToUserId, userId))
        ).orderByAsc(Chat::getTime);
        return Result.success(chatService.list(wrapper));
    }
}