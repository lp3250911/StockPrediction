package com.isaac.stock.predict;

public class stock_impl {
    public static void main(String[] args) {
        StockPricePrediction stockPricePrediction = new StockPricePrediction();
        Thread thread1 = new Thread(stockPricePrediction.thread1);
        Thread thread2 = new Thread(stockPricePrediction.thread2);
        Thread thread3 = new Thread(stockPricePrediction.thread3);
        Thread thread4 = new Thread(stockPricePrediction.thread4);
        thread1.start(); // 启动线程
        thread2.start(); // 启动线程
        thread3.start(); // 启动线程
        thread4.start(); // 启动线程
        System.out.println("主线程仍在运行...");

    }
}
