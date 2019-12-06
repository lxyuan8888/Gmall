package com.atyuan.gmall.dw.realtime.bean

/**
  * Create by lxyua on 2019/02/13 21:01
  */
case class OrderInfo(area: String,
                     var consignee: String,
                     orderComment: String,
                     var consigneeTel: String,
                     operateTime: String,
                     orderStatus: String,
                     paymentWay: String,
                     userId: String,
                     imgUrl: String,
                     totalAmount: Double,
                     expireTime: String,
                     deliveryAddress: String,
                     createTime: String,
                     trackingNo: String,
                     parentOrderId: String,
                     outTradeNo: String,
                     var id: String,
                     tradeBody: String,
                     var createDate: String,
                     var createHour: String,
                     var createHourMinute: String) {
  def getId():String ={
    id
  }
  def setId(id:String)={
    this.id=id
  }
}
