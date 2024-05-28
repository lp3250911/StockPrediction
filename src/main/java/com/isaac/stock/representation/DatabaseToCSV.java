package com.isaac.stock.representation;

import com.opencsv.CSVWriter;
import com.alibaba.fastjson.JSONObject;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static liquibase.util.StringUtil.isNumeric;


public class DatabaseToCSV {
    static String url = "jdbc:mysql://127.0.0.1:3306/akshare";
    static String user = "root";
    static String password = "Root";


    public JSONObject getStocksData(String stockname, int day_co) {
        // 数据库连接配置

//        String stockname="688511";
        JSONObject jSONObject = new JSONObject();


        // CSV文件路径
        String csvFile = "E:/lipei/use/lstm/temp/inputdata_predict" + stockname + ".csv";
//        String csvFile = "src/main/resources/inputdata_predict"+stockname+".csv";
        String str_sql = "";
        str_sql = "SELECT COUNT(*)co FROM `" + stockname + "` order by 日期 asc ";


        // 使用try-with-resources确保资源正确关闭
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(str_sql);
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile), ',', CSVWriter.NO_QUOTE_CHARACTER);
            resultSet.next();
            int co = Integer.parseInt(resultSet.getString("co")) - day_co;
            if (isNumeric(stockname)) {
                str_sql = "SELECT DISTINCT 日期, 代码,  开盘 , 收盘,最低 ,最高, 换手率,名称  FROM `" + stockname + "` order by 日期 asc LIMIT 0," + co;
            } else {
                str_sql = "SELECT DISTINCT date , 名称,  open, close,low ,high, volume  FROM `" + stockname + "` order by date asc  LIMIT 0," + co;
            }
            resultSet = statement.executeQuery(str_sql);
            // 写入标题行date,symbol,open,close,low,high,volume
            String[] headers = {"date", "symbol", "open", "close", "low", "high", "volume"}; // 根据实际情况修改
            csvWriter.writeNext(headers);
            String[] data = new String[7];
            String str_temp = "";
            // 写入数据行
            while (resultSet.next()) {
                if (isNumeric(stockname)) {
                    str_temp = resultSet.getString("日期").split(" ")[0];
                    data[0] = str_temp;
                    data[1] = resultSet.getString("代码");
                    data[2] = resultSet.getString("开盘");
                    data[3] = resultSet.getString("收盘");
                    data[4] = resultSet.getString("最低");
                    data[5] = resultSet.getString("最高");
                    data[6] = resultSet.getString("换手率");
                } else {
                    str_temp = resultSet.getString("date").split(" ")[0];
                    data[0] = str_temp;
                    data[1] = resultSet.getString("名称");
                    data[2] = resultSet.getString("open");
                    data[3] = resultSet.getString("close");
                    data[4] = resultSet.getString("low");
                    data[5] = resultSet.getString("high");
                    data[6] = resultSet.getString("volume");
                }

                csvWriter.writeNext(data);

            }
            csvWriter.close();

            jSONObject.put("date", data[0]);
            jSONObject.put("name", data[1]);

//            csvWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jSONObject;
    }
}