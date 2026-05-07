package com.example.springboot.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springboot.common.Result;
import com.example.springboot.config.interceptor.AuthAccess;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.Type;
import com.example.springboot.service.IAdminService;
import com.example.springboot.service.IGoodsService;
import com.example.springboot.service.ITypeService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private IAdminService adminService;

    @Resource
    private ITypeService typeService;

    @Resource
    private IGoodsService goodsService;

    // 💡 你的 DeepSeek API 配置
    private final String API_KEY = "key";
    private final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * AI 聊天接口：全信息读表增强版（含分类、名称、详情摘要）
     */
    @AuthAccess
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, String> body) {
        String userMessage = body.get("message");
        if (StrUtil.isBlank(userMessage)) {
            return Result.error("400", "内容不能为空");
        }

        // 1. 💡 动态提取知识库：分类 + 服务名称 + 详情描述
        // A. 获取所有分类信息
        List<Type> allTypes = typeService.list();

        // B. 获取所有已上架服务，包含 content 字段用于语义匹配
        LambdaQueryWrapper<Goods> goodsWrapper = new LambdaQueryWrapper<>();
        goodsWrapper.select(Goods::getName, Goods::getTypeId, Goods::getContent)
                .eq(Goods::getStatus, "已上架");
        List<Goods> onlineGoods = goodsService.list(goodsWrapper);

        // 2. 💡 构建结构化的上下文手册
        StringBuilder platformContext = new StringBuilder();
        platformContext.append("【平台实时业务手册（含详情摘要）】\n");
        for (Type type : allTypes) {
            platformContext.append("分类：").append(type.getName()).append("\n");

            // 筛选该分类下的服务项目
            List<Goods> typeGoods = onlineGoods.stream()
                    .filter(g -> g.getTypeId().equals(type.getId()))
                    .collect(Collectors.toList());

            for (Goods g : typeGoods) {
                // 💡 清洗 HTML 标签并截断描述文本，防止 Token 溢出
                String rawContent = g.getContent();
                String cleanContent = "";
                if (rawContent != null) {
                    // 利用正则表达式剔除所有 <> 标签
                    cleanContent = rawContent.replaceAll("<[^>]*>", "");
                    // 同时也去除常见的 HTML 转义符，如 &nbsp;
                    cleanContent = cleanContent.replaceAll("&nbsp;", " ");
                }
                // 后面继续执行截断逻辑
                if (cleanContent.length() > 50) {
                    cleanContent = cleanContent.substring(0, 50) + "...";
                }
                if (cleanContent != null && cleanContent.length() > 50) {
                    cleanContent = cleanContent.substring(0, 50) + "...";
                } else if (cleanContent == null) {
                    cleanContent = "暂无详细说明";
                }

                platformContext.append("  - 项目名：").append(g.getName())
                        .append(" (详情：").append(cleanContent).append(")\n");
            }
        }

        // 3. 构建 DeepSeek API 请求载荷
        JSONObject payload = new JSONObject();
        payload.set("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        // 💡 注入增强后的系统指令
        String systemInstruction = "你是一个专业、亲切的技能交易平台智能助手。你的任务是根据用户的需求，从下方的【平台实时业务手册】中寻找最匹配的服务。\n"
                + "要求：\n"
                + "1. 结合“项目名”和“详情”摘要来准确判断服务是否满足用户需求。\n"
                + "2. 如果手册中有匹配项，请详细介绍该服务并引导用户前往对应分类查看。\n"
                + "3. 严禁编造手册中不存在的服务，保持回复简洁专业。\n\n"
                + platformContext.toString();

        messages.add(new JSONObject().set("role", "system").set("content", systemInstruction));
        messages.add(new JSONObject().set("role", "user").set("content", userMessage));

        payload.set("messages", messages);
        payload.set("stream", false);

        try {
            // 4. 发送同步请求，增加超时时间以应对更大的上下文处理
            String response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(payload))
                    .timeout(35000)
                    .execute().body();

            JSONObject resObj = JSONUtil.parseObj(response);

            if (resObj.containsKey("error")) {
                return Result.error("500", "AI 平台异常: " + resObj.getJSONObject("error").getStr("message"));
            }

            String aiReply = resObj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            return Result.success(aiReply);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "AI 服务通讯失败，请稍后重试");
        }
    }

    /**
     * 获取官方客服信息 (保持不变)
     */
    @AuthAccess
    @GetMapping("/getAdmin")
    public Result getAdmin() {
        List<Admin> list = adminService.list();
        if (list.isEmpty()) {
            return Result.error("500", "未配置管理员");
        }
        Admin admin = list.get(0);
        JSONObject obj = JSONUtil.parseObj(admin);
        obj.set("role", "ADMIN");
        return Result.success(obj);
    }
}