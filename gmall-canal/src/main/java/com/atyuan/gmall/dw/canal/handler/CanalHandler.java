package com.atyuan.gmall.dw.canal.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.atyuan.gmall.dw.canal.utils.MyKafkaSender;
import com.atyuan.gmall.dw.constant.GmallConstant;
import com.google.common.base.CaseFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create by lxyua on 2019/02/13 18:54
 */
public class CanalHandler {
    public static void handle(String tableName, CanalEntry.EventType eventType, List<CanalEntry.RowData> rowChangeList) {
        if (eventType == CanalEntry.EventType.INSERT && "order_info".equals(tableName)) {
            for (CanalEntry.RowData rowData : rowChangeList) {
                //变化之后的数据
                List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();

                Map orderMap = new HashMap();
                for (CanalEntry.Column column : afterColumnsList) {

                    //将列名更改格式
                    String propertyName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, column.getName());
                    orderMap.put(propertyName, column.getValue());
                    System.out.println(JSON.toJSONString(orderMap));

                }
                MyKafkaSender.send(GmallConstant.KAFKA_TOPIC_ORDER, JSON.toJSONString(orderMap));
            }
        }
    }
}
