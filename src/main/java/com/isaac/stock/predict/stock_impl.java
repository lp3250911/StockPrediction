package com.isaac.stock.predict;

public class stock_impl {
    public static void main(String[] args) {
        StockPricePrediction stockPricePrediction = new StockPricePrediction();
        Thread thread = new Thread(stockPricePrediction);
        thread.start(); // 启动线程
        System.out.println("主线程仍在运行...");

    }
}
