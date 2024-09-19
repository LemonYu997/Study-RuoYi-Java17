package com.lemon.common.ratelimiter.aspectj;

import com.lemon.common.core.constant.GlobalConstants;
import com.lemon.common.core.exception.ServiceException;
import com.lemon.common.core.utils.MessageUtils;
import com.lemon.common.core.utils.ServletUtils;
import com.lemon.common.core.utils.SpringUtils;
import com.lemon.common.core.utils.StringUtils;
import com.lemon.common.ratelimiter.annotation.RateLimiter;
import com.lemon.common.ratelimiter.enums.LimitType;
import com.lemon.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateType;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * 限流处理
 */
@Slf4j
@Aspect
public class RateLimiterAspect {
    /**
     * 定义spel表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    /**
     * 定义spel解析模版
     */
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * 方法参数解析器
     */
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    /**
     * 方法前处理
     */
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        // 限流时间和限流次数
        int time = rateLimiter.time();
        int count = rateLimiter.count();
        try {
            String combineKey = getCombineKey(rateLimiter, point);
            RateType rateType = RateType.OVERALL;
            if (rateLimiter.limitType() == LimitType.CLUSTER) {
                rateType = RateType.PER_CLIENT;
            }
            // 是否限流
            long number = RedisUtils.rateLimiter(combineKey, rateType, count, time);
            if (number == -1) {
                // 限流报错
                String message = rateLimiter.message();
                if (StringUtils.startsWith(message, "{") && StringUtils.endsWith(message, "}")) {
                    message = MessageUtils.message(StringUtils.substring(message, 1, message.length() - 1));
                }
                throw new ServiceException(message);
            }
            log.info("限制令牌 => {}, 剩余令牌 => {}, 缓存key => '{}'", count, number, combineKey);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            } else {
                throw new RuntimeException("服务器限流异常，请稍候再试", e);
            }
        }
    }

    /**
     * 组合 key
     */
    private String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        // key用Spring el表达式来动态获取方法上的参数值
        String key = rateLimiter.key();
        // 判断 key 不为空和不是表达式
        if (StringUtils.isNotBlank(key) && StringUtils.containsAny(key, "#")) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method targetMethod = signature.getMethod();
            Object[] args = point.getArgs();
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, targetMethod, args, pnd);
            context.setBeanResolver(new BeanFactoryResolver(SpringUtils.getBeanFactory()));
            Expression expression;
            // 解析 EL 表达式
            if (StringUtils.startsWith(key, parserContext.getExpressionPrefix())
                && StringUtils.endsWith(key, parserContext.getExpressionSuffix())) {
                expression = parser.parseExpression(key, parserContext);
            } else {
                expression = parser.parseExpression(key);
            }
            key = expression.getValue(context, String.class);
        }
        StringBuilder sb = new StringBuilder(GlobalConstants.RATE_LIMIT_KEY);
        sb.append(ServletUtils.getRequest().getRequestURI()).append(":");
        if (rateLimiter.limitType() == LimitType.IP) {
            // 获取请求 IP
            sb.append(ServletUtils.getClientIP()).append(":");
        } else if (rateLimiter.limitType() == LimitType.CLUSTER) {
            // 获取客户端实例id
            sb.append(RedisUtils.getClient().getId()).append(":");
        }

        // 返回拼接好的 key
        return sb.append(key).toString();
    }
}
