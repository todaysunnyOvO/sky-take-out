package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * Cancel orders that are still unpaid 15 minutes after creation.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("Process timeout unpaid orders: {}", LocalDateTime.now());

        LocalDateTime orderTime = LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, orderTime);
        if (CollectionUtils.isEmpty(ordersList)) {
            return;
        }

        for (Orders ordersDB : ordersList) {
            Orders orders = new Orders();
            orders.setId(ordersDB.getId());
            orders.setStatus(Orders.CANCELLED);
            orders.setCancelReason("Order timeout, automatically canceled");
            orders.setCancelTime(LocalDateTime.now());
            orderMapper.update(orders);
        }
    }

    /**
     * Complete orders that have stayed in delivery for more than one hour.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("Process long-running delivery orders: {}", LocalDateTime.now());

        LocalDateTime orderTime = LocalDateTime.now().minusMinutes(60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, orderTime);
        if (CollectionUtils.isEmpty(ordersList)) {
            return;
        }

        for (Orders ordersDB : ordersList) {
            Orders orders = new Orders();
            orders.setId(ordersDB.getId());
            orders.setStatus(Orders.COMPLETED);
            orders.setDeliveryTime(LocalDateTime.now());
            orderMapper.update(orders);
        }
    }
}
