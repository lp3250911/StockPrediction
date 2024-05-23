package com.isaac.stock.representation;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseToCSV {

    public static void main(String[] args) {
        // 数据库连接配置
        String url = "jdbc:mysql://localhost:3306/tushare";
        String user = "root";
        String password = "Root@1234";
        String stockname="603259.sh";

        // CSV文件路径
        String csvFile = "inputdata_predict.csv";
        String str_sql="SELECT trade_date,  ts_code ,  open , close,low ,high, vol  FROM `"+stockname+"` order by trade_date desc ";

        // 使用try-with-resources确保资源正确关闭
        try (
                Connection conn = DriverManager.getConnection(url, user, password);
                Statement statement = conn.createStatement();

                ResultSet resultSet = statement.executeQuery(str_sql);
                CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile), ',', CSVWriter.NO_QUOTE_CHARACTER);
        ) {
            // 写入标题行date,symbol,open,close,low,high,volume
            String[] headers = {"date", "symbol", "open","close","low","high","volume"}; // 根据实际情况修改
            csvWriter.writeNext(headers);

            // 写入数据行
            while (resultSet.next()) {
                String str_temp=resultSet.getString("trade_date").split(" ")[0];
                String[] data = {
                        str_temp,
//                        resultSet.getString("trade_date"),
                        resultSet.getString("ts_code"),
                        resultSet.getString("open"),
                        resultSet.getString("close"),
                        resultSet.getString("low"),
                        resultSet.getString("high"),
                        resultSet.getString("vol")
                };
                csvWriter.writeNext(data);
            }
//            csvWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}