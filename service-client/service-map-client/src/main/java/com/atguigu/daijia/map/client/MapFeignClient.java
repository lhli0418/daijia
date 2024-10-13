package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "service-map")
public interface MapFeignClient {

    /**
     * 计算驾驶线路
     * @param calculateDrivingLineForm
     * @return
     */
    @PostMapping("/map/calculateDrivingLine")
    Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm);

    /**
     * 搜索附近满足条件的司机
     * @param searchNearByDriverForm
     * @return
     */
    @PostMapping("/map/location/searchNearByDriver")
    Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody SearchNearByDriverForm searchNearByDriverForm);
}