package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Constants;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IAdminService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private UserMapper userMapper; // 💡 注入 UserMapper 以操作影子账号



    @Override
    public Account login(Account account) {
        //查询
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, account.getUsername());
        wrapper.eq(Admin::getPassword, account.getPassword());
        Admin one = adminMapper.selectOne(wrapper);
        if (one != null) {
            //找用户
            String role = "ROLE_ADMIN";
            BeanUtils.copyProperties(one,account);
            String token = TokenUtils.createToken( one.getId() + "-" + role, account.getPassword());
            account.setToken(token);
            account.setRole(role);
            account.setPassword(null);
            return account;
        }else {
            throw new ServiceException(Constants.CODE_605,"用户名或密码错误");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(Account account) {
        // 1. 检查管理员表是否已存在该用户名
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, account.getUsername());
        Admin one = adminMapper.selectOne(wrapper);

        if (one != null) {
            throw new ServiceException(Constants.CODE_605, "用户已存在");
        }

        // 2. 在 sys_user 表创建影子账号
        User shadowUser = new User();
        shadowUser.setUsername(account.getUsername());
        shadowUser.setPassword(account.getPassword());
        shadowUser.setNickname(account.getNickname());
        shadowUser.setAvatarUrl(account.getAvatarUrl());
        shadowUser.setRole("ADMIN"); // 标记为管理员角色

        // 插入 User 表，MyBatis-Plus 会自动将自增 ID 回填到 shadowUser 对象
        userMapper.insert(shadowUser);

        // 3. 在 sys_admin 表创建管理员账号，并强制使用 User 表生成的 ID
        Admin newAdmin = new Admin();
        BeanUtils.copyProperties(account, newAdmin);
        newAdmin.setId(shadowUser.getId()); // 💡 关键：同步 ID
        newAdmin.setRole("ROLE_ADMIN");

        adminMapper.insert(newAdmin);
    }

    @Override
    public void updatePassword(Account account) {
        LambdaUpdateWrapper<Admin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Admin::getUsername, account.getUsername());
        wrapper.eq(Admin::getPassword, account.getPassword());
        wrapper.set(Admin::getPassword, account.getNewPassword());
        // 执行更新操作
        int updateCount = adminMapper.update(null, wrapper);
        // 检查更新结果
        if (updateCount == 0) {
            throw new ServiceException(Constants.CODE_605, "原密码输入错误，请检查后再试！");
        }
    }


}
