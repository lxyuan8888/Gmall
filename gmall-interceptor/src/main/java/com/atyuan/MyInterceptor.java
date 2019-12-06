package com.atyuan;


import com.google.gson.Gson;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create by lxyua on 2019/01/17 13:49
 * flume拦截器，实现Interceptor接口
 */
public class MyInterceptor implements Interceptor {
    Gson gson = null;

    @Override
    public void initialize() {
        gson = new Gson();
    }

    @Override
    public Event intercept(Event event) {
        //得到日志中的log，取出日志类型，放到header中
        String logString = new String(event.getBody());
        HashMap logMap = gson.fromJson(logString, HashMap.class);
        String logType = (String) logMap.get("type");
        Map<String, String> headers = event.getHeaders();
        headers.put("logType", logType);
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        for (Event event : events) {
            intercept(event);
        }
        return events;
    }

    @Override
    public void close() {

    }

    public static class Builder implements Interceptor.Builder {

        @Override
        public Interceptor build() {
            return new MyInterceptor();
        }

        @Override
        public void configure(Context context) {

        }
    }
}
