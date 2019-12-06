package com.atyuan.gmall.dw.export2es.app

import com.atyuan.gmall.dw.common.util.MyEsUtil
import com.atyuan.gmall.dw.constant.GmallConstant
import com.atyuan.gmall.dw.export2es.bean.SaleDetailDayCount
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ListBuffer

/**
  * Create by lxyua on 2019/02/15 23:01
  */
class Export2EsApp {
  def main(args: Array[String]): Unit = {
    var dt = ""
    if (args.length > 0) {
      dt = args(0)
    } else {
      dt = "2019-02-14"
    }

    val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("gmall-export")
    val session: SparkSession = SparkSession.builder().enableHiveSupport().config(conf).getOrCreate()
    import session.implicits._
    val rdd: RDD[SaleDetailDayCount] = session.sql("select user_id,sku_id,user_gender,cast(user_age as int) user_age,user_level,cast(order_price as double),sku_name,sku_tm_id, sku_category3_id,sku_category2_id,sku_category1_id,sku_category3_name,sku_category2_name,sku_category1_name,spu_id,sku_num,cast(order_count as bigint) order_count,cast(order_amount as double) order_amount,dt from gmall0808.dws_sale_detail_daycount where dt='" + dt + "'").as[SaleDetailDayCount].rdd
    rdd.foreachPartition { saleDetailItr =>
      var i = 0;
      val saleDetailDayList = new ListBuffer[SaleDetailDayCount]()
      for (saleDetail <- saleDetailDayList) {
        i += 1
        saleDetailDayList += saleDetail
        if (i % 10 == 0) {
          MyEsUtil.executeIndexBulk(GmallConstant.ES_INDEX_SALE, saleDetailDayList.toList, null)
          saleDetailDayList.clear()
        }
      }
      if(saleDetailDayList.size>0){
        MyEsUtil.executeIndexBulk(GmallConstant.ES_INDEX_SALE, saleDetailDayList.toList, null)
      }
    }
  }
}
