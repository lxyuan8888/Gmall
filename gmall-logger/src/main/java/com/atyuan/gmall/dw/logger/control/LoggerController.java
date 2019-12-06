package com.atyuan.gmall.dw.logger.control;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atyuan.gmall.dw.constant.GmallConstant;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

/**
 * Create by lxyua on 2019/01/16 19:01
 */
@RestController
public class LoggerController {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerController.class);

    @Autowired
    KafkaTemplate kafkaTemplate;

    @PostMapping("/log")
    public String getLog(@RequestParam("log") String log) {
        JSONObject jsonObject = JSON.parseObject(log);
        jsonObject.put("ts", System.currentTimeMillis() + 3600 * 1000 * 24 + new Random().nextInt(3600 * 1000 * 5));
        String newLog = jsonObject.toJSONString();

        //根据不同的日志类型发送到不同的kafka的topic
        if ("startup".equals(jsonObject.getString("type"))) {
            kafkaTemplate.send(GmallConstant.KAFKA_TOPIC_STARTUP, newLog);
        } else {
            kafkaTemplate.send(GmallConstant.KAFKA_TOPIC_EVENT, newLog);
        }

        logger.info(newLog);
        return "";
    }
}
