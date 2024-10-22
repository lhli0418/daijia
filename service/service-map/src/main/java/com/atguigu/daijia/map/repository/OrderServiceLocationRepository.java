package com.atguigu.daijia.map.repository;

import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * mongoDB repository类
 */
@Repository
public interface OrderServiceLocationRepository extends MongoRepository<OrderServiceLocation, String> {

}
