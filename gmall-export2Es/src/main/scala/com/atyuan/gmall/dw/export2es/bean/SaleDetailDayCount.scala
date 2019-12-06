package com.atyuan.gmall.dw.export2es.bean

/**
  * Create by lxyua on 2019/02/15 23:09
  */
case class SaleDetailDayCount(user_id: String,
                              sku_id: String,
                              user_gender: String,
                              user_age: Int,
                              user_level: String,
                              order_price: Double,
                              sku_name: String,
                              sku_tm_id: String,
                              sku_category1_id: String,
                              sku_category2_id: String,
                              sku_category3_id: String,
                              sku_category1_name: String,
                              sku_category2_name: String,
                              sku_category3_name: String,
                              spu_id: String,
                              sku_num: Long,
                              order_count: Long,
                              order_amount: Double,
                              var dt:String) {

}
