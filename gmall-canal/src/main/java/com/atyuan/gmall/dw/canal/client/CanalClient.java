package com.atyuan.gmall.dw.canal.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.atyuan.gmall.dw.canal.handler.CanalHandler;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Create by lxyua on 2019/02/13 18:23
 */
public class CanalClient {
    public static void main(String[] args) {
        CanalConnector canalConnector = CanalConnectors.newSingleConnector((new InetSocketAddress("hadoop102", 11111)), "example", "", "");
        while (true) {
            canalConnector.connect();
            // 订阅监控的数据库.表
            canalConnector.subscribe("gmall.order_info");
            //一个message包含多个sql
            Message message = canalConnector.get(100);

            if (message.getEntries().size() == 0) {
                System.out.println("暂时没有数据，休息5秒");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //
                List<CanalEntry.Entry> entries = message.getEntries();
                for (CanalEntry.Entry entry : entries) {
                    CanalEntry.EntryType entryType = entry.getEntryType();
                    if (entryType == CanalEntry.EntryType.TRANSACTIONBEGIN && entryType == CanalEntry.EntryType.TRANSACTIONEND) {
                        continue;
                    }
                    String tableName = entry.getHeader().getTableName();

                    CanalEntry.RowChange rowChange = null;

                    try {
                        rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }

                    List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();

                    //rowChange.getEventType 操作类型例如:update
                    CanalHandler.handle(tableName, rowChange.getEventType(), rowDatasList);
                }
            }
        }
    }
}
