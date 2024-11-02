package com.atguigu.daijia.driver.mapper;

import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface DriverAccountMapper extends BaseMapper<DriverAccount> {

    /**
     * 系统奖励打入司机账户
     * @param driverId
     * @param amount
     * @return
     */
    Integer add(Long driverId, BigDecimal amount);
}
