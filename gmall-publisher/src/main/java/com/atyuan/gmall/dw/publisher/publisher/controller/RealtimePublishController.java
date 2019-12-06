package com.atyuan.gmall.dw.publisher.publisher.controller;

import com.alibaba.fastjson.JSON;
import com.atyuan.gmall.dw.publisher.publisher.bean.Option;
import com.atyuan.gmall.dw.publisher.publisher.bean.Stat;
import com.atyuan.gmall.dw.publisher.publisher.service.RealtimePublishService;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Create by lxyua on 2019/02/13 08:22
 */
@RestController
public class RealtimePublishController {
    @Autowired
    RealtimePublishService realtimePublishService;

    @GetMapping("realtime-total")
    public String realtimeTotal(@RequestParam("date") String date) {
        List<Map> totalList = new ArrayList<>();

        //每日活跃用户
        int dauTotal = realtimePublishService.getDauTotal(date);
        Map map = new HashMap();
        map.put("id", "dau");
        map.put("name", "活跃用户");
        map.put("value", dauTotal);
        totalList.add(map);

        //每日交易额
        Double orderTotalAmount = realtimePublishService.getTotalAmount(date);
        Map orderTotalAmountMap = new HashMap();
        orderTotalAmountMap.put("id", "order_total_amount");
        orderTotalAmountMap.put("name", "交易额");
        orderTotalAmountMap.put("value", orderTotalAmount);
        totalList.add(orderTotalAmountMap);

        return JSON.toJSONString(totalList);
    }

    @GetMapping("realtime-hour")
    public String realtimeHour(@RequestParam("id") String id, @RequestParam("date") String date) {
        if ("dau".equals(id)) {
            Map map = new HashMap();
            Map dauHourTodayMap = realtimePublishService.getDauHour(date);
            map.put("today", dauHourTodayMap);

            Date yesterday = null;
            try {
                yesterday = DateUtils.addDays(new SimpleDateFormat("yyyy-MM-dd").parse(date), -1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Map dauHourYesterday = realtimePublishService.getDauHour(new SimpleDateFormat("yyyy-MM-dd").format(yesterday));
            map.put("yesterday", dauHourYesterday);
            return JSON.toJSONString(map);
        } else if ("order_total_amount".equals(id)) {
            Map orderTotalAmountInfoMap = new HashMap();
            Map orderTotalAmountTodayMap = realtimePublishService.getTotalAmountHour(date);
            orderTotalAmountInfoMap.put("today", orderTotalAmountTodayMap);

            //查询昨天的分时交易额
            Date yesterday = null;
            try {
                yesterday = DateUtils.addDays(new SimpleDateFormat("yyyy-MM-dd").parse(date), -1);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Map orderTotalAmountYesterdayMap = realtimePublishService.getTotalAmountHour(new SimpleDateFormat("yyyy-MM-dd").format(yesterday));
            orderTotalAmountInfoMap.put("yesterday", orderTotalAmountYesterdayMap);
            return JSON.toJSONString(orderTotalAmountYesterdayMap);
        }
        return null;
    }

    @GetMapping("sale_detail")
    public String getSaleDetail(HttpServletRequest request) {
        String keyword = request.getParameter("keyword");
        String date = request.getParameter("date");
        String startpage = request.getParameter("startpage");
        String size = request.getParameter("size");
        Map saleAgeMap = realtimePublishService.getSaleDetail(date, keyword, Integer.parseInt(startpage), Integer.parseInt(size), "user_age");

        Map<String, Long> ageMap = (Map<String, Long>) saleAgeMap.get("age");

        Integer total = (Integer) saleAgeMap.get("total");
        long age_20 = 0L;
        long age20_30 = 0L;
        long age30_ = 0L;

        for (Map.Entry<String, Long> ageEntry : ageMap.entrySet()) {
            int age = Integer.parseInt(ageEntry.getKey());
            Long ageCount = ageEntry.getValue();
            if (age < 20) {
                age_20 += ageCount;
            } else if (age >= 20 && age < 30) {
                age20_30 += ageCount;
            } else {
                age30_ += ageCount;
            }
        }

        double age_20Ratio = Math.round(1000.0D * age_20 / total) / 10D;
        double age20_30Ratio = Math.round(1000.0D * age20_30 / total) / 10D;
        double age30_Ratio = Math.round(1000.0D * age30_ / total) / 10D;
        List<Stat> statList = new ArrayList<>();

        List<Option> ageOptions = new ArrayList();
        ageOptions.add(new Option("20岁以下", age_20Ratio));
        ageOptions.add(new Option("20岁到30岁", age20_30Ratio));
        ageOptions.add(new Option("30岁及30岁以上", age30_Ratio));
        Stat ageStat = new Stat("用户年龄占比", ageOptions);
        statList.add(ageStat);

        Map saleDenderMap = realtimePublishService.getSaleDetail(date, keyword, Integer.parseInt(startpage), Integer.parseInt(size), "user_gender");

        Map<String, Long> genderMap = (Map<String, Long>) saleDenderMap.get("aggs");

        long maleCount = 0L;
        long femaleCount = 0L;

        for (Map.Entry<String, Long> entry : genderMap.entrySet()) {
            String gender = entry.getKey();
            Long genderCount = entry.getValue();
            if (gender.equals("M")) {
                maleCount = genderCount;
            } else {
                femaleCount = genderCount;
            }
        }

        double maleRatio = Math.round(1000.0D * maleCount / total) / 10D;
        double femaleRatio = Math.round(1000.0D * femaleCount / total) / 10D;

        List<Option> genderOptions = new ArrayList();
        genderOptions.add(new Option("男", maleRatio));
        genderOptions.add(new Option("女", femaleRatio));

        Stat genderStat = new Stat("用户性别占比", genderOptions);
        statList.add(genderStat);

        HashMap<Object, Object> saleMap = new HashMap<>();
        saleMap.put("detail", saleAgeMap.get("detail"));
        saleMap.put("total", total);
        saleMap.put("stat", statList);

        return JSON.toJSONString(saleMap);
    }
}