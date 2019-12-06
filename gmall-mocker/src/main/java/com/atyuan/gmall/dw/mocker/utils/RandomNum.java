package com.atyuan.gmall.dw.mocker.utils;

import java.util.Random;

/**
 * Create by lxyua on 2019/01/16 13:54
 */
public class RandomNum {
    public static final  int getRandInt(int fromNum,int toNum){
        return   fromNum+ new Random().nextInt(toNum-fromNum+1);
    }
}
