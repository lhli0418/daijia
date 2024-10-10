package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Value("${tencent.cloud.map}")
    private String key; // 腾讯地图服务

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        Map<String,String> map = new HashMap<>();
        // 起始位置
        map.put("from",calculateDrivingLineForm.getStartPointLatitude() + "," +
                calculateDrivingLineForm.getStartPointLongitude());
        map.put("to",calculateDrivingLineForm.getEndPointLatitude() + "," +
                calculateDrivingLineForm.getEndPointLongitude());
        // 腾讯云开发key
        map.put("key",key);

        // 使用RestTemplate调用get
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        // 判断调用是否成功
        if (result.getIntValue("status") != 0){
            // 失败
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        // 返回一条最佳路线
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        // 创建vo对象
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        // 时间
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        // 距离 km /1000 向上取证
        drivingLineVo.setDistance(route.getBigDecimal("distance").
                divideToIntegralValue(new BigDecimal(1000)).
                setScale(2, RoundingMode.HALF_UP));
        // 路线
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));

        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();
        return drivingLineVo;
    }
}
