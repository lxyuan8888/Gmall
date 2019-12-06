package com.atyuan.gmall.dw.publisher.publisher.service.impl;

import com.atyuan.gmall.dw.constant.GmallConstant;
import com.atyuan.gmall.dw.publisher.publisher.service.RealtimePublishService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create by lxyua on 2019/02/13 08:24
 */
@Service
public class RealtimePublishServiceImpl implements RealtimePublishService {
    @Autowired
    JestClient jestClient;

    @Override
    public int getDauTotal(String date) {
        int dauTotal = 0;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("logDate", date);
        boolQueryBuilder.filter(termQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);

        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(GmallConstant.ES_INDEX_DAU).addType("_doc").build();

        try {
            SearchResult searchResult = jestClient.execute(search);
            dauTotal = searchResult.getTotal();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dauTotal;
    }

    @Override
    public Map getDauHour(String date) {
        Map dauHourMap = new HashMap();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool 过滤
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("logDate", date);
        boolQueryBuilder.filter(termQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);
        //组合聚合语句
        TermsBuilder groupby_hour = AggregationBuilders.terms("groupby_hour");
        groupby_hour.field("logHour.keyword");
        searchSourceBuilder.aggregation(groupby_hour);

        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(GmallConstant.ES_INDEX_DAU).addType("_doc").build();

        try {
            SearchResult searchResult = jestClient.execute(search);
            //从结果集中取出，聚合信息
            List<TermsAggregation.Entry> buckets = searchResult.getAggregations().getTermsAggregation("groupby_hour").getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                dauHourMap.put(bucket.getKey(), bucket.getCount());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dauHourMap;
    }

    @Override
    public Double getTotalAmount(String date) {
        Double totalAmount = 0D;

        //代码编写es查询语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //过滤
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        TermQueryBuilder createDate = new TermQueryBuilder("createDate", date);
        boolQueryBuilder.filter(createDate);
        searchSourceBuilder.query(boolQueryBuilder);

        //聚合
        SumBuilder sumBuilder = AggregationBuilders.sum("sum_totalAmount").field("totalAmount");
        searchSourceBuilder.aggregation(sumBuilder);

        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(GmallConstant.ES_INDEX_ORDER).addType("_doc").build();

        try {
            SearchResult searchResult = jestClient.execute(search);
            totalAmount = searchResult.getAggregations().getSumAggregation("sum_totalAmount").getSum();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return totalAmount;
    }

    @Override
    public Map getTotalAmountHour(String date) {
        Map totalAmountHourMap = new HashMap();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //过滤
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("createDate", date);
        boolQueryBuilder.filter(termQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);

        //聚合
        TermsBuilder termsBuilder = AggregationBuilders.terms("groupby_createHour").field("createHour");
        SumBuilder sumBuilder = AggregationBuilders.sum("sum_totalAmount").field("totalAmount");

        //添加子聚合操作
        termsBuilder.subAggregation(sumBuilder);
        searchSourceBuilder.aggregation(termsBuilder);

        //创建Action对象 Search
        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(GmallConstant.ES_INDEX_ORDER).addType("_doc").build();
        try {
            SearchResult searchResult = jestClient.execute(search);
            //每小时聚合集合
            List<TermsAggregation.Entry> buckets = searchResult.getAggregations().getTermsAggregation("groupby_createHour").getBuckets();

            for (TermsAggregation.Entry bucket : buckets) {
                String hour = bucket.getKey();
                Double sum_totalAmount = bucket.getSumAggregation("totalAmount").getSum();
                totalAmountHourMap.put(hour, sum_totalAmount);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalAmountHourMap;
    }

    @Override
    public Map getSaleDetail(String date, String keyword, int startPage, int size, String aggsField) {
        Map saleDataMap = new HashMap();
        int total = 0;
        Map aggsMap = new HashMap();
        List<Map> detailList = new ArrayList<Map>();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(new TermsQueryBuilder("dt", date));

        //匹配
        if (keyword != null && keyword.length() > 0) {
            boolQueryBuilder.must(new MatchQueryBuilder("sku_name", keyword).operator(MatchQueryBuilder.Operator.AND));
            searchSourceBuilder.query(boolQueryBuilder);
        }
        if (aggsField != null && aggsField.length() > 0) {
            TermsBuilder termsBuilder = AggregationBuilders.terms("groupby_" + aggsField).field(aggsField).size(100);
            searchSourceBuilder.aggregation(termsBuilder);
        }

        searchSourceBuilder.from((startPage - 1) * size);
        searchSourceBuilder.size(size);

        Search build = new Search.Builder(searchSourceBuilder.toString()).addIndex(GmallConstant.ES_INDEX_SALE).addType("_doc").build();

        try {
            SearchResult searchResult = jestClient.execute(build);
            total = searchResult.getTotal();
            List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
            for (SearchResult.Hit<Map, Void> hit : hits) {
                Map map = hit.source;
                detailList.add(map);
            }

            List<TermsAggregation.Entry> buckets = searchResult.getAggregations().getTermsAggregation("groupby_" + aggsField).getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                String key = bucket.getKey();
                Long count = bucket.getCount();
                aggsMap.put(key, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        saleDataMap.put("detail", detailList);
        saleDataMap.put("total", total);
        saleDataMap.put("aggs", aggsMap);
        return saleDataMap;
    }
}
