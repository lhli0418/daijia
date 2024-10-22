package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.repository.OrderServiceLocationRepository;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.RedactOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderServiceLocationRepository orderServiceLocationRepository;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private MongoTemplate mongoTemplate;
    // 司机开启接单，更新司机位置信息
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        /**
         *  Redis GEO 主要用于存储地理位置信息，并对存储的信息进行相关操作，该功能在 Redis 3.2 版本新增。
         *  后续用在，乘客下单后寻找5公里范围内开启接单服务的司机，通过Redis GEO进行计算
         */
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                                updateDriverLocationForm.getLatitude().doubleValue());
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION,point,updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    // 司机关闭接单，删除司机位置信息
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION,driverId.toString());
        return true;
    }


    /**
     * 搜索附近满足条件的司机
     * @param searchNearByDriverForm 司机经纬度 订单里程
     * @return
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索五公里以内的司机
        // 定义经纬度
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),searchNearByDriverForm.getLatitude().doubleValue());
        // 定义范围 五公里（系统配置）
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS,RedisGeoCommands.DistanceUnit.KILOMETERS);
        // 定义以point点为中心，distance为距离这么一个范围
        Circle circle = new Circle(point,distance);

        // 定义GEO参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                                                    .includeCoordinates() // 返回坐标
                                                                    .includeDistance()    // 返回距离
                                                                    .sortAscending();     // 排序：升序

        // 1.GEORADIUS获取附近范围的信息
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);
        // 2.收集信息，存入list
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = Objects.requireNonNull(result).getContent();
        // 3.返回计算后的信息
        List<NearByDriverVo> list = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : content) {
            // 获取司机id
            Long driverId = Long.parseLong(item.getContent().getName());
            // 获取司机距离
            BigDecimal currentDistance = BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);
            // 获取司机个性化设置
            DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();
            // 接单里程判断，acceptDistance==0：不限制，
            if (driverSet.getAcceptDistance().doubleValue() != 0 && driverSet.getAcceptDistance().subtract(currentDistance).doubleValue() < 0) {
                continue;
            }
            // 订单里程判断，orderDistance==0：不限制
            if (driverSet.getOrderDistance().doubleValue() != 0 && driverSet.getOrderDistance().subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                continue;
            }
            // 满足条件的附近司机信息
            NearByDriverVo nearByDriverVo = new NearByDriverVo();
            nearByDriverVo.setDriverId(driverId);
            nearByDriverVo.setDistance(currentDistance);
            list.add(nearByDriverVo);
        }
        return list;
    }

    /**
     * 司机赶往代驾起始点：更新订单地址到缓存
     * @param updateOrderLocationForm
     * @return
     */
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {

        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());


        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(orderLocationVo));

        return true;
    }

    /**
     * 司机赶往代驾起始点：获取订单经纬度位置
     * @param orderId
     * @return
     */
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;

        return JSONObject.parseObject((String)redisTemplate.opsForValue().get(key), OrderLocationVo.class);

    }

    /**
     * 开始代驾服务：保存代驾服务订单位置
     * @param orderLocationServiceFormList
     * @return
     */
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        List<OrderServiceLocation> orderServiceLocationList = new ArrayList<>();

        orderLocationServiceFormList.forEach(orderLocationServiceForm -> {

            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            BeanUtils.copyProperties(orderLocationServiceForm,orderServiceLocation);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date());

            orderServiceLocationList.add(orderServiceLocation);
        });

        orderServiceLocationRepository.saveAll(orderServiceLocationList);
        return true;
    }

    /**
     * 代驾服务：获取订单服务最后一个位置信息
     * @param orderId
     * @return
     */
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {

        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("creatTime")));
        query.skip(0).limit(1);
        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);

        // 封装返回对象
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        assert orderServiceLocation != null;
        BeanUtils.copyProperties(orderServiceLocation,orderServiceLastLocationVo);
        return orderServiceLastLocationVo;
    }
}
