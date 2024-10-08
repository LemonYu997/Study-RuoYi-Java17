package com.lemon.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import com.lemon.common.core.constant.Constants;
import com.lemon.common.core.constant.GlobalConstants;
import com.lemon.common.core.domain.R;
import com.lemon.common.core.utils.SpringUtils;
import com.lemon.common.core.utils.StringUtils;
import com.lemon.common.core.utils.reflect.ReflectUtils;
import com.lemon.common.ratelimiter.annotation.RateLimiter;
import com.lemon.common.ratelimiter.enums.LimitType;
import com.lemon.common.redis.utils.RedisUtils;
import com.lemon.common.web.config.properties.CaptchaProperties;
import com.lemon.common.web.enums.CaptchaType;
import com.lemon.web.domain.vo.CaptchaVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 验证码操作处理
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class CaptchaController {
    private final CaptchaProperties captchaProperties;
//    private final MailProperties mailProperties;

    /**
     * 生成验证码
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @GetMapping("/auth/code")
    public R<CaptchaVo> getCode() {
        CaptchaVo vo = new CaptchaVo();
        Boolean captchaEnabled = captchaProperties.getEnable();
        if (!captchaEnabled) {
            vo.setCaptchaEnabled(false);
            return R.ok(vo);
        }
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + uuid;
        // 生成验证码
        CaptchaType captchaType = captchaProperties.getType();
        // 判断是数字验证码还是字符验证码
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? captchaProperties.getNumberLength() : captchaProperties.getCharLength();
        CodeGenerator codeGenerator = ReflectUtils.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringUtils.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        // 如果是数学验证码，使用SpEL表达式处理验证码结果
        String code = captcha.getCode();
        if (isMath) {
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(StringUtils.remove(code, "="));
            code = exp.getValue(String.class);
        }
        // 验证码入缓存，时效 2 分钟
        RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        vo.setUuid(uuid);
        vo.setImg(captcha.getImageBase64());
        return R.ok(vo);
    }
}
