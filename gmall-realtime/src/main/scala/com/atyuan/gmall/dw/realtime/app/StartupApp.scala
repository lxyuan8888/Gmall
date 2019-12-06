package com.atyuan.gmall.dw.realtime.app

import java.text.SimpleDateFormat
import java.util
import java.util.{Date, Properties}

import com.alibaba.fastjson.JSON
import com.atyuan.gmall.dw.common.util.{MyEsUtil, MyKafkaUtil, PropertiesUtil}
import com.atyuan.gmall.dw.constant.GmallConstant
import com.atyuan.gmall.dw.realtime.bean.Startup
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

/**
  * Create by lxyua on 2019/02/14 13:26
  */
object StartupApp {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setAppName("gmall").setMaster("local[*]")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(10))
    val recordDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_TOPIC_STARTUP, ssc)

    val startupDstream: DStream[Startup] = recordDstream.map(_.value()).map { startupjson =>
      val startup: Startup = JSON.parseObject(startupjson, classOf[Startup])
      startup
    }

    val startupFilteredDstream: DStream[Startup] = startupDstream.transform { rdd =>
      ///  drive 操作    把当前已经记录的日活用户取出来
      val properties: Properties = PropertiesUtil.load("config.properties")
      val jedisDriver = new Jedis(properties.getProperty("redis.host"), properties.getProperty("redis.port").toInt)
      val dauSet: util.Set[String] = jedisDriver.smembers("dau:" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
      //通过广播变量发送到executor
      val dausetBC: Broadcast[util.Set[String]] = sc.broadcast(dauSet)
      println("过滤前：" + rdd.count())
      val rddFiltered: RDD[Startup] = rdd.filter { startup => //executor 操作
        val dausetEx: util.Set[String] = dausetBC.value
        !dausetEx.contains(startup.mid)
      }
      println("过滤后：" + rddFiltered.count())
      rddFiltered
    }

    startupFilteredDstream.foreachRDD(rdd => {
      // jedis
      rdd.foreachPartition { startupItr =>
        //jedis
        val properties: Properties = PropertiesUtil.load("config.properties")
        val jedisExecutor = new Jedis(properties.getProperty("redis.host"), properties.getProperty("redis.port").toInt)
        val startupList = new ListBuffer[Startup]()
        for (startup <- startupItr) {

          startup.logDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(startup.ts))
          startup.logHour = new SimpleDateFormat("HH").format(new Date(startup.ts))
          startup.logHourMinute = new SimpleDateFormat("HH:mm").format(new Date(startup.ts))
          jedisExecutor.sadd("dau:" + startup.logDate, startup.mid)
          startupList += startup
        }
        jedisExecutor.close()
        //startup -> es
        MyEsUtil.executeIndexBulk(GmallConstant.ES_INDEX_DAU, startupList.toList, null)
      }


    }
    )

    ssc.start()
    ssc.awaitTermination()
  }
}
