package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;

    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null || !userId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart query = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(query);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        BigDecimal orderAmount = calculateOrderAmount(shoppingCartList, ordersSubmitDTO.getPackAmount());
        LocalDateTime orderTime = LocalDateTime.now();

        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setAddressBookId(addressBook.getId());
        order.setOrderTime(orderTime);
        order.setPayStatus(Orders.UN_PAID);
        order.setAmount(orderAmount);
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        order.setAddress(buildAddress(addressBook));

        orderMapper.insert(order);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        shoppingCartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        Orders order = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        Long userId = BaseContext.getCurrentId();

        if (order == null || !userId.equals(order.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!Orders.PENDING_PAYMENT.equals(order.getStatus()) || !Orders.UN_PAID.equals(order.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(order.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(ordersPaymentDTO.getPayMethod())
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

        return OrderPaymentVO.builder()
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .nonceStr(RandomStringUtils.randomNumeric(32))
                .packageStr("mock-prepay_id=" + order.getNumber())
                .signType("MOCK")
                .paySign("mock-pay-sign")
                .build();
    }

    private BigDecimal calculateOrderAmount(List<ShoppingCart> shoppingCartList, Integer packAmount) {
        BigDecimal goodsAmount = shoppingCartList.stream()
                .map(cart -> cart.getAmount().multiply(BigDecimal.valueOf(cart.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return goodsAmount.add(BigDecimal.valueOf(packAmount == null ? 0 : packAmount));
    }

    private String buildAddress(AddressBook addressBook) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, addressBook.getProvinceName());
        appendIfPresent(builder, addressBook.getCityName());
        appendIfPresent(builder, addressBook.getDistrictName());
        appendIfPresent(builder, addressBook.getDetail());
        return builder.toString();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isEmpty()) {
            builder.append(value);
        }
    }
}
