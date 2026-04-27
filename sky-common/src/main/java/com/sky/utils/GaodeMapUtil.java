package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.GaodeProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 高德地图工具类
 */
@Component
@Slf4j
public class GaodeMapUtil {

    @Autowired
    private GaodeProperties gaodeProperties;

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String DISTANCE_URL = "https://restapi.amap.com/v3/distance";

    /**
     * 地理编码：将地址转换为经纬度坐标
     * @param address 地址
     * @return [longitude, latitude]，如果解析失败返回 null
     */
    public String[] geocode(String address) {
        try {
            String url = GEOCODE_URL + "?key=" + gaodeProperties.getKey()
                    + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8.name());

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            httpClient.close();
            response.close();

            log.info("高德地理编码返回结果：{}", result);

            JSONObject jsonObject = JSON.parseObject(result);
            String status = jsonObject.getString("status");
            if ("1".equals(status)) {
                JSONArray geocodes = jsonObject.getJSONArray("geocodes");
                if (geocodes != null && geocodes.size() > 0) {
                    String location = geocodes.getJSONObject(0).getString("location");
                    // location 格式：longitude,latitude
                    return location.split(",");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("地理编码失败，地址：{}", address, e);
            return null;
        }
    }

    /**
     * 计算两个坐标之间的距离（单位：米）
     * @param originLocation      起点坐标，格式：longitude,latitude
     * @param destinationLocation 终点坐标，格式：longitude,latitude
     * @return 距离（米），如果计算失败返回 null
     */
    public Integer calculateDistance(String originLocation, String destinationLocation) {
        try {
            String url = DISTANCE_URL + "?key=" + gaodeProperties.getKey()
                    + "&origins=" + originLocation
                    + "&destination=" + destinationLocation
                    + "&type=1"; // type=1 使用驾车距离

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            httpClient.close();
            response.close();

            log.info("高德距离计算返回结果：{}", result);

            JSONObject jsonObject = JSON.parseObject(result);
            String status = jsonObject.getString("status");
            if ("1".equals(status)) {
                JSONArray results = jsonObject.getJSONArray("results");
                if (results != null && results.size() > 0) {
                    return results.getJSONObject(0).getInteger("distance");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("距离计算失败", e);
            return null;
        }
    }

    /**
     * 判断两个地址之间的距离是否在配送范围内
     * @param shopAddress     商家地址
     * @param deliveryAddress 收货地址
     * @param maxDistance     最大配送距离（米）
     * @return true=在配送范围内，false=超出配送范围
     */
    public boolean isWithinDeliveryRange(String shopAddress, String deliveryAddress, Integer maxDistance) {
        String[] shopLocation = geocode(shopAddress);
        if (shopLocation == null) {
            log.error("商家地址解析失败：{}", shopAddress);
            return false;
        }

        String[] deliveryLocation = geocode(deliveryAddress);
        if (deliveryLocation == null) {
            log.error("收货地址解析失败：{}", deliveryAddress);
            return false;
        }

        String shopLoc = shopLocation[0] + "," + shopLocation[1];
        String deliveryLoc = deliveryLocation[0] + "," + deliveryLocation[1];

        Integer distance = calculateDistance(shopLoc, deliveryLoc);
        if (distance == null) {
            log.error("距离计算失败");
            return false;
        }

        log.info("配送距离：{}米，最大配送距离：{}米", distance, maxDistance);
        return distance <= maxDistance;
    }
}
