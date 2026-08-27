package com.litemes.base.module.user.init;

import com.litemes.base.module.user.entity.SysUser;
import com.litemes.base.module.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 初始管理员初始化：首次启动且不存在 admin 账号时自动创建（admin / 123456）。
 * 属于应用自身的数据初始化行为（JPA ddl-auto=update 模式下建表后落数据），不依赖外部 SQL 脚本。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String ADMIN_USERNAME = "admin";

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameAndDelFlag(ADMIN_USERNAME, DEL_FLAG_NORMAL)) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRealName("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus("ENABLED");
        userRepository.save(admin);
        log.warn("已创建初始管理员账号：admin / 123456，请尽快修改密码");
    }
}
