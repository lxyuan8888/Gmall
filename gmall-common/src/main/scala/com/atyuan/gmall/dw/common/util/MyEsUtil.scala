package com.atyuan.gmall.dw.common.util

import java.util.Objects

import io.searchbox.client.config.HttpClientConfig
import io.searchbox.client.{JestClient, JestClientFactory}
import io.searchbox.core.{Bulk, BulkResult, Index}
import org.apache.commons.beanutils.BeanUtils

/**
  * Create by lxyua on 2019/02/12 20:00
  */
object MyEsUtil {
  private val ES_HOST = "http://hadoop102"
  private val ES_HTTP_PORT = 9200
  private var factory: JestClientFactory = null

  /**
    * 获取客户端
    *
    * @return jestclient
    */
  def getClient: JestClient = {
    if (factory == null) build()
    factory.getObject
  }

  /**
    * 关闭客户端
    */
  def close(client: JestClient): Unit = {
    if (!Objects.isNull(client)) try
      client.shutdownClient()
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  /**
    * 建立连接
    */
  private def build(): Unit = {
    factory = new JestClientFactory
    factory.setHttpClientConfig(new HttpClientConfig.Builder(ES_HOST + ":" + ES_HTTP_PORT).multiThreaded(true)
      .maxTotalConnection(20) //连接总数
      .connTimeout(10000).readTimeout(10000).build)

  }

  def executeIndexBulk(indexName: String, list: List[Any], idColumn: String): Unit = {
    val jestClient: JestClient = getClient
    val bulkBuilder: Bulk.Builder = new Bulk.Builder().defaultIndex(indexName).defaultType("_doc")
    for (doc <- list ) {
      val indexBuilder = new Index.Builder(doc)
      if(idColumn!=null&&idColumn.size>0){
        val idValue: String = BeanUtils.getProperty(doc,idColumn)
        if(idValue!=null){
          indexBuilder.id(idValue)
        }
      }
      val index: Index = indexBuilder.build()
      bulkBuilder.addAction(index)
    }
    val result: BulkResult = jestClient.execute(bulkBuilder.build())
    println("已保存：" + result.getItems.size() + "条")
    close(jestClient)
    /// bulk == batch

    //  val index: Index = new Index.Builder(json).index("gmall0808_dau").`type`("_doc").id("3").build()

  }

  def main(args: Array[String]): Unit = {
    executeIndexBulk(null, null, null)
  }
}
