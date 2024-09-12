package com.lemon.common.mybatis.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.lemon.common.core.factory.YmlPropertySourceFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis-plus配置类(下方注释有插件介绍)
 */
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("${mybatis-plus.mapperPackage}")
@PropertySource(value = "classpath:common-mybatis.yml", factory = YmlPropertySourceFactory.class)
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件
        // 数据权限处理
        // 分页插件
        // 乐观锁插件
        return interceptor;
    }

    /**
     * PaginationInnerInterceptor 分页插件，自动识别数据库类型
     * https://baomidou.com/pages/97710a/
     * OptimisticLockerInnerInterceptor 乐观锁插件
     * https://baomidou.com/pages/0d93c0/
     * MetaObjectHandler 元对象字段填充控制器
     * https://baomidou.com/pages/4c6bcf/
     * ISqlInjector sql注入器
     * https://baomidou.com/pages/42ea4a/
     * BlockAttackInnerInterceptor 如果是对全表的删除或更新操作，就会终止该操作
     * https://baomidou.com/pages/f9a237/
     * IllegalSQLInnerInterceptor sql性能规范插件(垃圾SQL拦截)
     * IdentifierGenerator 自定义主键策略
     * https://baomidou.com/pages/568eb2/
     * TenantLineInnerInterceptor 多租户插件
     * https://baomidou.com/pages/aef2f2/
     * DynamicTableNameInnerInterceptor 动态表名插件
     * https://baomidou.com/pages/2a45ff/
     */

}
