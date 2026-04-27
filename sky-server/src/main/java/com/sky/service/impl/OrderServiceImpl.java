package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.GaodeProperties;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.GaodeMapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private GaodeMapUtil gaodeMapUtil;

    @Autowired
    private GaodeProperties gaodeProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 模拟支付成功，直接修改订单状态
        paySuccess(ordersPaymentDTO.getOrderNumber());

        // 构造模拟的支付成功返回结果，供前端使用
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr("mockNonceStr");
        vo.setPaySign("mockPaySign");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setPackageStr("prepay_id=mockPrepayId");
        vo.setSignType("RSA");

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //////////////////////////////////////////////
        Map map = new HashMap();
        map.put("type", 1);//消息类型，1表示来单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);
        //通过WebSocket实现来单提醒，向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
        ///////////////////////////////////////////////////
    }

    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {

        //填充用户id
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        //设置分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> records = getOrderVOList(page, false);

        return new PageResult(page.getTotal(), records);
    }

    @Override
    public OrderVO detail(Long orderId) {

        //先查询订单信息
        Orders orders = orderMapper.getById(orderId);

        //再查询订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        //封装入OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Override
    public void userCancelById(Long orderId) throws Exception {

        //根据id查询订单
        Orders order = orderMapper.getById(orderId);

        //校验订单是否存在
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态不为待付款或待接单时不允许用户退款
        if (order.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //订单状态为待接单时取消，需要进行退款（模拟退款，跳过真实微信接口）
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //模拟退款成功，直接修改支付状态为退款
            order.setPayStatus(Orders.REFUND);
        }

        //更新订单状态、取消原因、取消时间并更新到数据库
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    public void repetition(Long orderId) {
        //查询当前用户id
        Long userId = BaseContext.getCurrentId();

        //根据id查询当前订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        //将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            //将订单详情里的菜品信息复制到购物车对象中并补全缺少的信息
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页，并进行分页条件查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //将Orders对象转换为OrderVO对象并返回
        List<OrderVO> orderVOList = getOrderVOList(page, true);
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        //根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatue(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatue(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatue(Orders.DELIVERY_IN_PROGRESS);

        //将查询出的数据封装到OrderStaticticsVO中并返回
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //根据id查询订单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        //只有订单存在且状态为待接单时才可以拒单
        if (orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Integer payStatue = orders.getPayStatus(); //支付状态
        //若用户已支付，则需要退款
        if (payStatue.equals(Orders.PAID)) {
            // ========== 真实微信退款接口（已注释，模拟支付时使用）==========
            // String refund = weChatPayUtil.refund(
            //         orders.getNumber(), //商户订单号
            //         orders.getNumber(), //商户退款单号
            //         new BigDecimal(0.01),//退款金额，单位 元
            //         new BigDecimal(0.01));//原订单金额
            // log.info("申请退款：{}", refund);
            // ==============================================================

            // 模拟退款成功，直接修改支付状态为退款
            log.info("模拟退款成功，订单号：{}", orders.getNumber());

            //支付状态修改为退款
            orders.setPayStatus(Orders.REFUND);
        }

        //根据订单id更新订单状态、拒单原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        //根据id查询订单
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        //若订单不存在则抛出错误
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Integer payStatus = orders.getPayStatus(); //支付状态
        //若用户已支付，则需要退款
        if (payStatus.equals(Orders.PAID)) {
            // ========== 真实微信退款接口（已注释，模拟支付时使用）==========
            // String refund = weChatPayUtil.refund(
            //         orders.getNumber(),
            //         orders.getNumber(),
            //         new BigDecimal(0.01),
            //         new BigDecimal(0.01)
            // );
            // log.info("申请退款：{}", refund);
            // ==============================================================

            // 模拟退款成功，直接修改支付状态为退款
            log.info("模拟退款成功，订单号：{}", orders.getNumber());

            //支付状态修改为退款
            orders.setPayStatus(Orders.REFUND);
        }

        //根据订单id更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long orderId) {
        //根据id查询订单
        Orders orders = orderMapper.getById(orderId);

        //只有订单存在且状态为待派送才可以派送
        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //更新订单状态
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long orderId) {
        //根据id查询订单
        Orders orders = orderMapper.getById(orderId);

        //只有订单存在且状态为派送中才可以完成
        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //更新订单状态、送达时间
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 用户催单
     *
     * @param id
     */
    public void reminder(Long id) {
        // 查询订单是否存在
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //基于WebSocket实现催单
        Map map = new HashMap();
        map.put("type", 2);//2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }


    private List<OrderVO> getOrderVOList(Page<Orders> page, boolean isAdmin) {
        List<OrderVO> orderVOList = new ArrayList<>();

        //查询订单明细，并封装入OrderVO进行响应
        if (page != null && !page.isEmpty()) {
            for (Orders order : page) {
                //封装入OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);

                //查询订单明细
                Long orderId = order.getId(); //订单id
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                //如果是管理端的查询请求，就拼接菜品信息字符串(格式：菜品名字*菜品数量; )
                if (isAdmin) {
                    StringBuffer orderDishes = new StringBuffer();
                    for (OrderDetail orderDetail : orderDetailList) {
                        orderDishes.append(orderDetail.getName()).append('*').append(orderDetail.getNumber()).append("; ");
                        orderVO.setOrderDishes(orderDishes.toString());
                    }
                } else {
                    //如果是用户端的查询请求，就把订单明细封装入OrderVO
                    orderVO.setOrderDetailList(orderDetailList);
                }
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }



    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //校验收货地址是否超出配送范围（5公里）
        //拼接完整收货地址：省+市+区+详细地址
        String deliveryAddress = addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail();
        boolean withinRange = gaodeMapUtil.isWithinDeliveryRange(
                gaodeProperties.getShopAddress(),
                deliveryAddress,
                5000); // 配送范围5公里
        if (!withinRange) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_OUT_OF_RANGE);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail,"id");
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }

}
