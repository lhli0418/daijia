package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dom4j.io.SAXReader;

public interface DriverInfoService extends IService<DriverInfo> {

    Long login(String code);

    DriverLoginVo getDriverLoginInfo(Long driverId);

    //获取司机认证信息
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    DriverSet getDriverSet(Long driverId);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean updateServiceStatus(Long driverId, Integer status);

    DriverInfoVo getDriverInfo(Long driverId);

    String getDriverOpenId(Long driverId);
}
