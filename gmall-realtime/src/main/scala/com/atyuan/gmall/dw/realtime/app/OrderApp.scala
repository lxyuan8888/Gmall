package com.atyuan.gmall.dw.realtime.app

import com.alibaba.fastjson.JSON
import com.atyuan.gmall.dw.common.util.{MyEsUtil, MyKafkaUtil}
import com.atyuan.gmall.dw.constant.GmallConstant
import com.atyuan.gmall.dw.realtime.bean.OrderInfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Create by lxyua on 2019/02/13 20:28
  */
object OrderApp {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setAppName("gmall").setMaster("local[*]")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(2))
    val recordDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_TOPIC_ORDER, ssc)
    val orderDStream: DStream[OrderInfo] = recordDstream.map(_.value()).map(jsonString => {
      val orderInfo: OrderInfo = JSON.parseObject(jsonString, classOf[OrderInfo])

      //对敏感数据进行脱敏处理，必须不可逆
      orderInfo.consignee = orderInfo.consignee.splitAt(1)._1 + "**"
      orderInfo.consigneeTel = orderInfo.consigneeTel.splitAt(3)._1 + "**"

      //时间 2019-02-13 15:16:23
      orderInfo.createDate = orderInfo.createTime.split(" ")(0)
      orderInfo.createHour = orderInfo.createTime.split(" ")(0).split(":")(0)
      orderInfo.createHourMinute = orderInfo.createTime.substring(orderInfo.createTime.indexOf(" "),orderInfo.createTime.lastIndexOf(":"))
      orderInfo
    })

    orderDStream.foreachRDD(rdd =>
      rdd.foreachPartition(orderItr =>
        MyEsUtil.executeIndexBulk(GmallConstant.ES_INDEX_ORDER, orderItr.toList, "id")
      ))

    ssc.start()
    ssc.awaitTermination()
  }
}
