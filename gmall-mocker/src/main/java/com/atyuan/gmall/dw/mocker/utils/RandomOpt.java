package com.atyuan.gmall.dw.mocker.utils;

/**
 * Create by lxyua on 2019/01/16 13:53
 */
public class RandomOpt<T> {
    T value ;
    int weight;

    public RandomOpt ( T value, int weight ){
        this.value=value ;
        this.weight=weight;
    }

    public T getValue() {
        return value;
    }

    public int getWeight() {
        return weight;
    }

}
