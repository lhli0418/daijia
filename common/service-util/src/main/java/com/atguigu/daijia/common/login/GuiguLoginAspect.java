package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.swing.text.Utilities;

/**
 * ClassName: GuiguLoginAspect
 * package: com.atguigu.daijia.common.login
 * Description:
 *
 * @Author lh
 * @Create 2024/10/5 12:13
 * @Version 1.0
 */
@Aspect
@Slf4j
@Component
@Order(100)
public class GuiguLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     *  环绕通知：检测登录状态
     * @param guiguLogin
     * @return
     */
    // 切点表达式： 指定对哪些规则的方法进行增强
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(guiguLogin)")
    public Object process(ProceedingJoinPoint joinPoint, GuiguLogin guiguLogin) throws Throwable{

        // 1.获取request对象
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();

        // 2.从请求头中获取token
        String token = request.getHeader("token");
        // 3.判断token是否为空，为空返回登录提示
        if (!StringUtils.hasText(token)){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        // 4.token不为空，查询redis
        String userId = (String)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        // 5.查询redis对应用户id，将用户id存入ThreadLocal
        // AuthContextHolder封装了ThreadLocal，用于保存当前线程的用户id，这样后面controller方法就能够获取当前用户id了。
        if (StringUtils.hasText(userId)){
            AuthContextHolder.setUserId(Long.parseLong(userId));
        }
        // 6.执行业务方法
        return joinPoint.proceed();
    }
}
