package com.theoyu.thesis.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.auth.constants.RedisKeyConstants;
import com.theoyu.thesis.auth.enums.ResponseCodeEnum;
import com.theoyu.thesis.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.theoyu.thesis.auth.service.VerificationCodeService;
import com.theoyu.thesis.auth.utils.sms.AliyunSmsHelper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5L;
    private static final long VERIFICATION_CODE_SEND_INTERVAL_MINUTES = 3L;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private AliyunSmsHelper aliyunSmsHelper;


    @Override
    public Response<?> sendVerificationCode(SendVerificationCodeReqVO reqVO) {
        // 获取手机号和构造 Redis Key
        String phone = reqVO.getPhone();
        String verificationCodeKey = RedisKeyConstants.buildVerificationCodeKey(phone);
        String sendLimitKey = RedisKeyConstants.buildVerificationCodeSendLimitKey(phone);

        boolean isSend = Boolean.TRUE.equals(redisTemplate.hasKey(sendLimitKey));

        if (isSend) {
            throw new BusinessException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }
        String verificationCode = RandomUtil.randomNumbers(6);

        log.info("==> 手机号: {}, 即将发送的验证码：【{}】", phone, verificationCode);

        //在异步线程池中调用阿里云短信服务发送验证码
        //TODO:阿里云暂时无法申请短信签名，因此这个接口当前仅用于测试，实际使用时需要替换为有效的短信签名和模板代码
//        threadPoolTaskExecutor.submit(() -> {
//            String signName = "阿里云短信测试";
//            String templateCode = "SMS_322255324";
//            String templateParam = String.format("{\"code\":\"%s\"}", verificationCode);
//            aliyunSmsHelper.sendTextMessage(signName, templateCode, phone, templateParam);
//        });

        // 验证码有效期和发送冷却分开管理，避免验证码未过期时阻塞下一次发送。
        redisTemplate.opsForValue().set(verificationCodeKey, verificationCode, VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(sendLimitKey, "1", VERIFICATION_CODE_SEND_INTERVAL_MINUTES, TimeUnit.MINUTES);
        return Response.success();
    }
}
