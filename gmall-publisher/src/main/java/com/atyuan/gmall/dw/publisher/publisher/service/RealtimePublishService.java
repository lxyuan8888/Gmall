package com.atyuan.gmall.dw.publisher.publisher.service;

import java.util.Map;

/**
 * Create by lxyua on 2019/02/13 08:24
 */
public interface RealtimePublishService {

    //day07 获取每日日活
    public int getDauTotal(String date);

    //day07 获取每日分时日活
    public Map getDauHour(String date);

    //day08 获取每日交易总金额
   public Double getTotalAmount(String date);

    //day08 获取每日分时交易金额
    public Map getTotalAmountHour(String date);

    //day09
    Map getSaleDetail(String date, String keyword, int startPage, int size, String aggsField);
}
