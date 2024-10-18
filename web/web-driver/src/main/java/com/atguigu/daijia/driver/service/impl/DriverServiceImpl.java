package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverFaceRecognition;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public String login(String code) {
        // 1.用code进行远程调用,获取用户id对应响应码
        Result<Long> loginResult = driverInfoFeignClient.login(code);
        Integer codeResult = loginResult.getCode();

        // 2.判断如果返回失败了,返回错误提示
        // 200正常
        if (codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 3.获取司机id
        Long driverId = loginResult.getData();

        // 4.判断返回id是否为空,如果为空,返回错位提示
        if (driverId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 5.生成token字符串 UUID / jwt工具包
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 6.把用户id放入Redis,设置过期时间
        // key:token value:driverId
//        redisTemplate.opsForValue().set(token,driverId.toString(),30,
//                TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 7.返回token
        return token;

    }


    /**
     * 根据司机id查询用户信息
     * @param driverId
     * @return
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        Result<DriverLoginVo> result = driverInfoFeignClient.getDriverLoginInfo(driverId);
        return result.getData();
    }

    /**
     * 获取司机认证信息
     * @param driverId
     * @return
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {

        Result<DriverAuthInfoVo> driverAuthInfo = driverInfoFeignClient.getDriverAuthInfo(driverId);
        return driverAuthInfo.getData();
    }

    /**
     *  更新司机认证信息
     * @param updateDriverAuthInfoForm
     * @return
     */
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        return driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm).getData();
    }

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm
     * @return
     */
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm).getData();
    }

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId
     * @return
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    /**
     * 司机人脸验证
     * @param driverFaceModelForm
     * @return
     */
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    /**
     * 开启接单服务
     * @param driverId
     * @return
     */
    @Override
    public Boolean startService(Long driverId) {
        // 判断是否已认证
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        if(driverLoginVo.getAuthStatus() != 2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        // 判读是否进行人脸识别
        Boolean isFaceRecognition = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        if (!isFaceRecognition) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        // 更新司机接单状态
        driverInfoFeignClient.updateServiceStatus(driverId,1);

        // 删除司机位置
        locationFeignClient.removeDriverLocation(driverId);

        // 清空司机新订单临时队列
        newOrderFeignClient.clearNewOrderQueueData(driverId);

        return true;
    }
}
