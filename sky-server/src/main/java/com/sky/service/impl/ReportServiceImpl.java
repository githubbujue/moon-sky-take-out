package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的日期
        List<Double> turnoverList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天最早和最晚的时间

        dateList.add(begin);
        //将范围内的日期存到集合中
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期加一
            dateList.add(begin);
        }

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //当天最早的时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //当天最晚的时间

            //查询date对应日期的营业额，即状态为已完成的订单的总金额
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map); //查询当天的营业额
            turnover = turnover == null ? 0.0 : turnover; //防止查询结果为null
            turnoverList.add(turnover);
        }

        //将集合转换为字符串
        String dateListString = StringUtils.join(dateList, ",");
        String turnoverListString = StringUtils.join(turnoverList, ",");

        //构造TurnoverReportVO并返回
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();
        return turnoverReportVO;
    }



    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的日期
        List<Integer> totalUserList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的总用户数
        List<Integer> newUserList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天新增的用户数

        dateList.add(begin);
        //将范围内的容器存到集合中
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期加一
            dateList.add(begin);
        }

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //当天最早的时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //当天最晚的时间

            //查询date对应日期的总人数和当天新增的用户数
            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map); //总用户数量
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map); //新增用户数量

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        //将集合转换为字符串
        String dateListString = StringUtils.join(dateList, ",");
        String totalUserListString = StringUtils.join(totalUserList, ",");
        String newUserListString = StringUtils.join(newUserList, ",");

        //构造UserReportVO并返回
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();
        return userReportVO;
    }

    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的日期

        //将范围内的日期存到集合中
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期加一
            dateList.add(begin);
        }

        return dateList;
    }


    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end); //当前集合用于存放从begin到end范围内每天的日期
        List<Integer> orderCountList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的订单数
        List<Integer> validOrderCountList = new ArrayList<>(); //当前集合用于存放从begin到end范围内每天的有效订单数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //当天最早的时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //当天最晚的时间

            //查询date对应日期的订单数和有效订单数
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(map); //查询订单数
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map); //查询有效订单数

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get(); //计算订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get(); //计算有效订单总数
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount; //计算订单完成率

        //将集合转换为字符串
        String dateListString = StringUtils.join(dateList, ",");
        String orderCountListString = StringUtils.join(orderCountList, ",");
        String validOrderCountListString = StringUtils.join(validOrderCountList, ",");

        //构造OrderReportVO并返回
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(dateListString)
                .orderCountList(orderCountListString)
                .validOrderCountList(validOrderCountListString)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN); //指定时间范围内最早的时间
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX); //指定时间范围内最晚的时间

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime); //查询指定时间范围内排名前10名的商品及其销量
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getSaleCount).collect(Collectors.toList());

        //将集合转换为字符串
        String nameListString = StringUtils.join(nameList, ",");
        String numberListString = StringUtils.join(numberList, ",");

        //构造SalesTop10ReportVO并返回
        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder()
                .nameList(nameListString)
                .numberList(numberListString)
                .build();
        return salesTop10ReportVO;
    }



}
