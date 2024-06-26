package com.isaac.stock.predict;

import com.alibaba.fastjson.JSONArray;
import com.isaac.stock.model.RecurrentNets;
import com.isaac.stock.representation.PriceCategory;
import com.isaac.stock.representation.StockDataSetIterator;
import com.isaac.stock.representation.DatabaseToCSV;
import com.isaac.stock.utils.PlotUtil;
import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.Math.abs;


/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class StockPricePrediction implements Runnable  {
    static String url = "jdbc:mysql://127.0.0.1:3306/akshare";
    static String user = "root";
    static String password = "Root";
    private static double co1 = 0;
    private static double co2 = 0;
    private static double[] data1 = new double[100];
    private static double[] data2 = new double[100];


    private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

    private static int exampleLength = 5; // time series length, assume 22 working days per month

    ///////////////////多线程
    @Override
    public void run() {

        System.out.println("通过实现Runnable接口创建的线程正在运行...");
        // 这里放置你希望线程执行的代码
    }

    // 创建线程1
    Thread thread1 = new Thread(new Runnable() {
        public void run() {
            // 线程1的业务逻辑
            try {
                make_model();
//                poc_head_stocks1();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    // 创建线程2
    Thread thread2 = new Thread(new Runnable() {
        public void run() {
            // 线程2的业务逻辑
            try {
                make_model2();
//                poc_head_stocks2();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    // 创建线程3
    Thread thread3 = new Thread(new Runnable() {
        public void run() {
            // 线程3的业务逻辑
            try {
                make_model3();
//                poc_head_stocks3();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    // 创建线程3
    Thread thread4 = new Thread(new Runnable() {
        public void run() {
            // 线程3的业务逻辑
            try {
                make_model4();
//                poc_head_stocks4();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    ///////////////////////////

    //选股
    private static void proc_vol(int ifprice) throws IOException, SQLException {//计算vol+price,并测试
        JSONObject jSONObject = new JSONObject();
        String sql_str = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'akshare'";
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql_str);
        int i = 0;
        while (resultSet.next()) {
            String stockname = resultSet.getString("TABLE_NAME");
            if (ifprice == 0) {
                sel_stocks_tools_vol(2, stockname);
            } else {
                sel_stocks_tools_volprice(2, stockname);
            }

        }
    }

//    //板块+分成股
//    public static void main(String[] args) throws IOException, SQLException {
//        JSONObject jSONObject = new JSONObject();
//        String sql_str = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'akshare'";
//        try {
//            Connection conn = DriverManager.getConnection(url, user, password);
//            Statement statement = conn.createStatement();
//            ResultSet resultSet = statement.executeQuery(sql_str);
//            Statement statement1 = conn.createStatement();
//            while (resultSet.next()) {
//                String stockname = resultSet.getString("TABLE_NAME");
//                jSONObject = predict_run(stockname);
//                String str_res = jSONObject.toJSONString();
//                String sql_insert = "INSERT INTO ansys_results (`code`, `name`,`results`,`correctness`, `similarity` ) VALUES ('" + stockname + "','" + jSONObject.get("name") + "','" + str_res + "','" + jSONObject.get("correctness") + "','" + jSONObject.get("similarity") + "')";
//                statement1.executeUpdate(sql_insert);
//            }
//
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//    }
//历史数据分析
//    public static void main(String[] args) throws IOException, SQLException {
//        JSONObject jSONObject = new JSONObject();
//        his_ansys_main(44,"688425");
//
//
//
//    }

    private static JSONObject predict_run(String stockname) throws IOException, InterruptedException {
        String file = "E:/lipei/use/lstm/temp/inputdata_predict" + stockname + ".csv";
        String symbol = stockname; // stock name
        DatabaseToCSV databaseToCSV = new DatabaseToCSV();
        JSONObject jSONObject = databaseToCSV.getStocksData(symbol, 0);
        JSONObject res = new JSONObject();


        int batchSize = 64; // mini-batch size
        double splitRatio = 0.9; // 90% for training, 10% for testing
        int epochs = 44; // training epochs

        log.info("Create dataSet iterator...");
//        Thread.sleep(10000);
        PriceCategory category = PriceCategory.CLOSE; // CLOSE: predict close price
        StockDataSetIterator iterator = new StockDataSetIterator(file, symbol, batchSize, exampleLength, splitRatio, category);
        log.info("Load test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

        log.info("Build lstm networks...");
        MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
            iterator.reset(); // reset iterator
            net.rnnClearPreviousState(); // clear previous state
        }

        log.info("Saving model...");
        File locationToSave = new File("src/main/resources/StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"));
        // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
        ModelSerializer.writeModel(net, locationToSave, true);

        log.info("Load model...");
        net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        log.info("Testing...");
        if (category.equals(PriceCategory.ALL)) {
            INDArray max = Nd4j.create(iterator.getMaxArray());
            INDArray min = Nd4j.create(iterator.getMinArray());
            predictAllCategories(net, test, max, min);
        } else {
            double max = iterator.getMaxNum(category);
            double min = iterator.getMinNum(category);
            res = predictPriceOneAhead(net, test, max, min, category);
        }
        log.info("Done...");
        res.put("date", jSONObject.get("date"));
        res.put("name", jSONObject.get("name"));
        return res;
    }

    private static void his_ansys_main(int day_co, String stockname, int ifp) throws SQLException {
        double[] correctness_vol = new double[100];
        double[] correctness_price = new double[100];
        double[] similarity_vol = new double[100];
        double[] similarity_price = new double[100];
        String[] date = new String[100];
        JSONObject jSONObject_res = new JSONObject();
        Statement statement = null;


        for (int i = day_co; i > 0; i--) {
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                statement = conn.createStatement();
                int ifprice = ifp;
                JSONObject jSONObject = his_ansys(stockname, i, ifprice);
                if (ifprice == 1) {
                    correctness_price[day_co - i] = Double.parseDouble(jSONObject.get("correctness_price").toString());
                    similarity_price[day_co - i] = Double.parseDouble(jSONObject.get("similarity_price").toString());
                }
                correctness_vol[day_co - i] = Double.parseDouble(jSONObject.get("correctness_vol").toString());
                similarity_vol[day_co - i] = Double.parseDouble(jSONObject.get("similarity_vol").toString());
                date[day_co - i] = jSONObject.get("date").toString();
            } catch (InterruptedException | SQLException | IOException e) {
                e.printStackTrace();
            }

        }
        PlotUtil.plot(correctness_vol, correctness_price, "正确率");
        PlotUtil.plot(similarity_vol, similarity_price, "相似度");
        jSONObject_res.put("date", date);
        jSONObject_res.put("correctness_vol", correctness_vol);
        jSONObject_res.put("correctness_price", correctness_price);
        jSONObject_res.put("similarity_vol", similarity_vol);
        jSONObject_res.put("similarity_price", similarity_price);
        String str_res = jSONObject_res.toJSONString();
        String sql_update = "update ansys_results set his_ansys='" + str_res + "' where name= " + stockname;
        statement.executeUpdate(sql_update);

    }

    private static void sel_stocks_tools_volprice(int day_co, String stockname) throws SQLException {//进行成交量和收盘价预测、相似计算
        double[] correctness_vol = new double[100];
        double[] correctness_price = new double[100];
        double[] similarity_vol = new double[100];
        double[] similarity_price = new double[100];
        String[] date = new String[100];
        JSONObject jSONObject_res = new JSONObject();
        Statement statement = null;
        String name = "";


        for (int i = day_co; i > 0; i--) {
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                statement = conn.createStatement();
                JSONObject jSONObject = his_ansys(stockname, i, 1);
                correctness_vol[day_co - i] = Double.parseDouble(jSONObject.get("correctness_vol").toString());
                correctness_price[day_co - i] = Double.parseDouble(jSONObject.get("correctness_price").toString());
                similarity_vol[day_co - i] = Double.parseDouble(jSONObject.get("similarity_vol").toString());
                similarity_price[day_co - i] = Double.parseDouble(jSONObject.get("similarity_price").toString());
                date[day_co - i] = jSONObject.get("date").toString();
                name = jSONObject.get("name").toString();
            } catch (InterruptedException | SQLException | IOException e) {
                e.printStackTrace();
            }

        }
        jSONObject_res.put("date", date);
        jSONObject_res.put("correctness_vol", correctness_vol);
        jSONObject_res.put("correctness_price", correctness_price);
        jSONObject_res.put("similarity_vol", similarity_vol);
        jSONObject_res.put("similarity_price", similarity_price);
        String str_res = jSONObject_res.toJSONString();
        double min_v = Integer.MAX_VALUE;
        double min_p = Integer.MAX_VALUE;
        double[] s_vol = new double[day_co];
        double[] s_price = new double[day_co];
        for (int j = 0; j < day_co; j++) {
            s_vol[j] = similarity_vol[j];
            s_price[j] = similarity_price[j];

        }
        for (double num : s_vol) {
            min_v = Math.min(min_v, num); // 找到最小值
        }
        for (double num : s_price) {
            min_p = Math.min(min_p, num); // 找到最小值
        }
//        boolean t1=s_vol[0]/min_v>1.15 &&s_vol[4]/min_v>1.15;
//        boolean t2=s_price[0]/min_p>1.15 &&s_price[4]/min_p>1.15;

        boolean t1 = s_vol[1] / s_vol[0] > 1.15;
        boolean t2 = s_price[1] / s_price[0] > 1.15;
        log.info(stockname + "处理中...");
        if (t1 || t2) {
            String sql_update = "insert into  sel_stocks  (`code`, `results`) VALUES ('" + stockname + "','" + str_res + "')";
            statement.executeUpdate(sql_update);
        }

    }

    private static void sel_stocks_tools_vol(int day_co, String stockname) throws SQLException {//只进行成交量预测、相似计算
        double[] correctness_vol = new double[100];
        double[] similarity_vol = new double[100];
        String[] date = new String[100];
        JSONObject jSONObject_res = new JSONObject();
        Statement statement = null;
        String name = "";


        for (int i = day_co; i > 0; i--) {
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                statement = conn.createStatement();
                int ifprice = 0;
                JSONObject jSONObject = his_ansys(stockname, i, ifprice);

                correctness_vol[day_co - i] = Double.parseDouble(jSONObject.get("correctness_vol").toString());
                similarity_vol[day_co - i] = Double.parseDouble(jSONObject.get("similarity_vol").toString());
                date[day_co - i] = jSONObject.get("date").toString();
                name = jSONObject.get("name").toString();
            } catch (InterruptedException | SQLException | IOException e) {
                e.printStackTrace();
            }

        }
        jSONObject_res.put("date", date);
        jSONObject_res.put("correctness_vol", correctness_vol);
        jSONObject_res.put("similarity_vol", similarity_vol);
        String str_res = jSONObject_res.toJSONString();
        double min_v = Integer.MAX_VALUE;
        double min_p = Integer.MAX_VALUE;
        double[] s_vol = new double[day_co];
        double[] s_price = new double[day_co];
        for (int j = 0; j < day_co; j++) {
            s_vol[j] = similarity_vol[j];

        }
        for (double num : s_vol) {
            min_v = Math.min(min_v, num); // 找到最小值
        }

        boolean t1 = s_vol[1] / s_vol[0] > 1.15;
        log.info(stockname + "处理中...");
        if (t1) {
            String sql_update = "insert into  sel_stocks  (`code`, `results`) VALUES ('" + stockname + "','" + str_res + "')";
            statement.executeUpdate(sql_update);
        }

    }

    private static void make_model() throws SQLException, IOException, InterruptedException {//只进行最近一天的成交量预测、相似计算模型生成
        String sql_str = "SELECT 代码 FROM `filtered_stocks` order by `index` asc";
//        String sql_str = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'akshare'";
        JSONObject res = new JSONObject();
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii = -1;
        while (resultSet.next()) {
            ii++;
            String stockname = resultSet.getString("代码");
            if (ii >=0 && ii <= 70 && stockname.chars().allMatch(Character::isDigit)) {
                sql_str = "SELECT code,similarity FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("similarity");
                }
                //取股票数据库中最后一天的日期，若similarity_str里已有，则不进行his_ansys
                sql_str = "SELECT 日期 FROM `" + stockname + "` order by 日期 desc limit 1";
                ResultSet resultSet2 = statement1.executeQuery(sql_str);
                resultSet2.next();
                String last_day = resultSet2.getString("日期");
                if (!(similarity_str ==null) ) {

                    if (similarity_str.contains(last_day)) {

                        System.out.println("已计算过");
                        continue;
                    }
                }
                        System.out.println("计算。。。");

                        res = his_ansys(stockname, 1, 0);


                        JSONObject jsonObject1 = new JSONObject();

                        jsonObject1.put("date", res.get("date"));
                        jsonObject1.put("similarity_vol", res.get("similarity_vol"));

                        similarity_str = similarity_str + "," + jsonObject1.toJSONString();
                        String sql_update = "Update ansys_results set similarity= '" + similarity_str + "' where code = '" + stockname + "'";
                        statement1.executeUpdate(sql_update);
                        sql_update = "Update filtered_stocks set 相似度= '" + res.get("similarity_vol") + "' where 代码 = '" + stockname + "'";
                        statement1.executeUpdate(sql_update);
                        log.info(stockname + "处理完成...");
                    }
                }
            }

    private static void make_model2() throws SQLException, IOException, InterruptedException {//只进行最近一天的成交量预测、相似计算模型生成
        String sql_str = "SELECT 代码 FROM `filtered_stocks` order by `index` asc";
        JSONObject res = new JSONObject();
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii = -1;
        while (resultSet.next()) {
            ii++;
            String stockname = resultSet.getString("代码");
            if (ii > 70 && ii <= 140 && stockname.chars().allMatch(Character::isDigit) ) {

                sql_str = "SELECT code,similarity FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("similarity");
                }
                //取股票数据库中最后一天的日期，若similarity_str里已有，则不进行his_ansys
                sql_str = "SELECT 日期 FROM `" + stockname + "` order by 日期 desc limit 1";
                ResultSet resultSet2 = statement1.executeQuery(sql_str);
                resultSet2.next();
                String last_day = resultSet2.getString("日期");
                if (!(similarity_str ==null) ) {

                    if (similarity_str.contains(last_day)) {

                        System.out.println("已计算过");
                        continue;
                    }
                }
                        System.out.println("计算。。。");

                        res = his_ansys(stockname, 1, 0);


                        JSONObject jsonObject1 = new JSONObject();

                        jsonObject1.put("date", res.get("date"));
                        jsonObject1.put("similarity_vol", res.get("similarity_vol"));

                        similarity_str = similarity_str + "," + jsonObject1.toJSONString();
                        String sql_update = "Update ansys_results set similarity= '" + similarity_str + "' where code = '" + stockname + "'";
                        statement1.executeUpdate(sql_update);
                sql_update = "Update filtered_stocks set 相似度= '" + res.get("similarity_vol") + "' where 代码 = '" + stockname + "'";
                statement1.executeUpdate(sql_update);
                        log.info(stockname + "处理完成...");
                    }
                }
            }
    private static void make_model3() throws SQLException, IOException, InterruptedException {//只进行最近一天的成交量预测、相似计算模型生成
        String sql_str = "SELECT 代码 FROM `filtered_stocks` order by `index` asc";
        JSONObject res = new JSONObject();
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii = -1;
        while (resultSet.next()) {
            ii++;
            String stockname = resultSet.getString("代码");
            if (ii > 140 && ii<=210 && stockname.chars().allMatch(Character::isDigit)) {

                sql_str = "SELECT code,similarity FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("similarity");
                }
                //取股票数据库中最后一天的日期，若similarity_str里已有，则不进行his_ansys
                sql_str = "SELECT 日期 FROM `" + stockname + "` order by 日期 desc limit 1";
                ResultSet resultSet2 = statement1.executeQuery(sql_str);
                resultSet2.next();
                String last_day = resultSet2.getString("日期");
                if (!(similarity_str ==null) ) {

                    if (similarity_str.contains(last_day)) {

                        System.out.println("已计算过");
                        continue;
                    }
                }
                        System.out.println("计算。。。");

                        res = his_ansys(stockname, 1, 0);


                        JSONObject jsonObject1 = new JSONObject();

                        jsonObject1.put("date", res.get("date"));
                        jsonObject1.put("similarity_vol", res.get("similarity_vol"));

                        similarity_str = similarity_str + "," + jsonObject1.toJSONString();
                        String sql_update = "Update ansys_results set similarity= '" + similarity_str + "' where code = '" + stockname + "'";
                        statement1.executeUpdate(sql_update);
                sql_update = "Update filtered_stocks set 相似度= '" + res.get("similarity_vol") + "' where 代码 = '" + stockname + "'";
                statement1.executeUpdate(sql_update);
                        log.info(stockname + "处理完成...");
                    }
                }
        }



    private static void make_model4() throws SQLException, IOException, InterruptedException {//只进行最近一天的成交量预测、相似计算模型生成
        String sql_str = "SELECT 代码 FROM `filtered_stocks` order by `index` asc";
        JSONObject res = new JSONObject();
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii = -1;
        while (resultSet.next()) {
            ii++;
            String stockname = resultSet.getString("代码");
            if (ii > 210 && ii<=280 && stockname.chars().allMatch(Character::isDigit)) {

                sql_str = "SELECT code,similarity FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("similarity");
                }
                //取股票数据库中最后一天的日期，若similarity_str里已有，则不进行his_ansys
                sql_str = "SELECT 日期 FROM `" + stockname + "` order by 日期 desc limit 1";
                ResultSet resultSet2 = statement1.executeQuery(sql_str);
                resultSet2.next();
                String last_day = resultSet2.getString("日期");
                if (!(similarity_str ==null) ) {

                    if (similarity_str.contains(last_day)) {

                        System.out.println("已计算过");
                        continue;
                    }
                }
                        System.out.println("计算。。。");

                        res = his_ansys(stockname, 1, 0);


                        JSONObject jsonObject1 = new JSONObject();

                        jsonObject1.put("date", res.get("date"));
                        jsonObject1.put("similarity_vol", res.get("similarity_vol"));

                        similarity_str = similarity_str + "," + jsonObject1.toJSONString();
                        String sql_update = "Update ansys_results set similarity= '" + similarity_str + "' where code = '" + stockname + "'";
                        statement1.executeUpdate(sql_update);
                sql_update = "Update filtered_stocks set 相似度= '" + res.get("similarity_vol") + "' where 代码 = '" + stockname + "'";
                statement1.executeUpdate(sql_update);
                        log.info(stockname + "处理完成...");
                    }
                }
            }

    private static void poc_head_stocks1() throws SQLException, IOException, InterruptedException {
        Connection conn = DriverManager.getConnection(url, user, password);
        int day_long=22;
        double[] similarity_vol = new double[day_long];
        String[] date = new String[day_long];
        JSONObject jsonObject=new JSONObject();
        JSONObject jsonObject_res=new JSONObject();
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        String sql_str = "SELECT 代码,相似度 FROM `filtered_stocks` WHERE `相似度` IS NOT NULL order by `相似度` asc limit 20";
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii=-1;
        while (resultSet.next()) {
            ii++;
            if (ii >300 && ii < 400) {
                String stockname = resultSet.getString("代码");
                sql_str = "SELECT code,results FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
            for(int i=day_long-1;i>=0;i--) {

                    jsonObject = his_ansys(stockname, i, 0);
                    similarity_vol[day_long-1 - i] = Double.parseDouble(jsonObject.get("similarity_vol").toString());
                    date[day_long-1 - i] = jsonObject.get("date").toString();

                }
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("results");
                }
                jsonObject_res.put("date", date);
                jsonObject_res.put("similarity_vol", similarity_vol);
                similarity_str = similarity_str + "," + jsonObject_res.toJSONString();
                String sql_update = "Update ansys_results set results= '" + similarity_str + "' where code = '" + stockname + "'";
                statement1.executeUpdate(sql_update);

            }




        }


    }
    private static void poc_head_stocks2() throws SQLException, IOException, InterruptedException {
        Connection conn = DriverManager.getConnection(url, user, password);
        int day_long=22;
        double[] similarity_vol = new double[day_long];
        String[] date = new String[day_long];
        JSONObject jsonObject=new JSONObject();
        JSONObject jsonObject_res=new JSONObject();
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        String sql_str = "SELECT 代码,相似度 FROM `filtered_stocks` WHERE `相似度` IS NOT NULL order by `相似度` asc limit 20";
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii=-1;
        while (resultSet.next()) {
            ii++;
            if (ii >= 3 && ii < 6) {
                String stockname = resultSet.getString("代码");
                sql_str = "SELECT code,results FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                for(int i=day_long-1;i>=0;i--) {

                    jsonObject = his_ansys(stockname, i, 0);
                    similarity_vol[day_long-1 - i] = Double.parseDouble(jsonObject.get("similarity_vol").toString());
                    date[day_long-1 - i] = jsonObject.get("date").toString();

                }
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("results");
                }
                jsonObject_res.put("date", date);
                jsonObject_res.put("similarity_vol", similarity_vol);
                similarity_str = similarity_str + "," + jsonObject_res.toJSONString();
                String sql_update = "Update ansys_results set results= '" + similarity_str + "' where code = '" + stockname + "'";
                statement1.executeUpdate(sql_update);

            }




        }


    }
    private static void poc_head_stocks3() throws SQLException, IOException, InterruptedException {
        Connection conn = DriverManager.getConnection(url, user, password);
        int day_long=22;
        double[] similarity_vol = new double[day_long];
        String[] date = new String[day_long];
        JSONObject jsonObject=new JSONObject();
        JSONObject jsonObject_res=new JSONObject();
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        String sql_str = "SELECT 代码,相似度 FROM `filtered_stocks` WHERE `相似度` IS NOT NULL order by `相似度` asc limit 20";
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii=-1;
        while (resultSet.next()) {
            ii++;
            if (ii >= 6 && ii < 9) {
                String stockname = resultSet.getString("代码");
                sql_str = "SELECT code,results FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                for(int i=day_long-1;i>=0;i--) {

                    jsonObject = his_ansys(stockname, i, 0);
                    similarity_vol[day_long-1 - i] = Double.parseDouble(jsonObject.get("similarity_vol").toString());
                    date[day_long-1 - i] = jsonObject.get("date").toString();

                }
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("results");
                }
                jsonObject_res.put("date", date);
                jsonObject_res.put("similarity_vol", similarity_vol);
                similarity_str = similarity_str + "," + jsonObject_res.toJSONString();
                String sql_update = "Update ansys_results set results= '" + similarity_str + "' where code = '" + stockname + "'";
                statement1.executeUpdate(sql_update);

            }




        }


    }
    private static void poc_head_stocks4() throws SQLException, IOException, InterruptedException {
        Connection conn = DriverManager.getConnection(url, user, password);
        int day_long=22;
        double[] similarity_vol = new double[day_long];
        String[] date = new String[day_long];
        JSONObject jsonObject=new JSONObject();
        JSONObject jsonObject_res=new JSONObject();
        Statement statement = conn.createStatement();
        Statement statement1 = conn.createStatement();
        String sql_str = "SELECT 代码,相似度 FROM `filtered_stocks` WHERE `相似度` IS NOT NULL order by `相似度` asc limit 20";
        ResultSet resultSet = statement.executeQuery(sql_str);
        int ii=-1;
        while (resultSet.next()) {
            ii++;
            if (ii >= 9 && ii < 12) {
                String stockname = resultSet.getString("代码");
                sql_str = "SELECT code,results FROM ansys_results WHERE code = '" + stockname + "'";
                ResultSet resultSet1 = statement1.executeQuery(sql_str);
                String similarity_str = null;
                for(int i=day_long-1;i>=0;i--) {

                    jsonObject = his_ansys(stockname, i, 0);
                    similarity_vol[day_long-1 - i] = Double.parseDouble(jsonObject.get("similarity_vol").toString());
                    date[day_long-1 - i] = jsonObject.get("date").toString();

                }
                if (!resultSet1.next()) {
                    String sql_insert = "INSERT INTO ansys_results (`code`, `name` ) VALUES ('" + stockname + "','" + stockname + "')";
                    statement1.execute(sql_insert);

                } else {
                    similarity_str = resultSet1.getString("results");
                }
                jsonObject_res.put("date", date);
                jsonObject_res.put("similarity_vol", similarity_vol);
                similarity_str = similarity_str + "," + jsonObject_res.toJSONString();
                String sql_update = "Update ansys_results set results= '" + similarity_str + "' where code = '" + stockname + "'";
                statement1.executeUpdate(sql_update);

            }




        }


    }




    private static JSONObject his_ansys(String stockname, int day_co, int ifprice) throws IOException, InterruptedException {
        String file = "E:/lipei/use/lstm/temp/inputdata_predict" + stockname + ".csv";
        String symbol = stockname; // stock name
        DatabaseToCSV databaseToCSV = new DatabaseToCSV();
        JSONObject jSONObject = databaseToCSV.getStocksData(symbol, day_co);
        JSONObject res = new JSONObject();
        JSONObject res1 = new JSONObject();


        int batchSize = 64; // mini-batch size
        double splitRatio = 0.9; // 90% for training, 10% for testing
        int epochs = 44; // training epochs

        log.info("Create dataSet iterator...");
//        Thread.sleep(10000);

        PriceCategory[] category = {PriceCategory.VOLUME, PriceCategory.CLOSE}; // CLOSE: predict close price
        for (int ik = 0; ik < ifprice + 1; ik++) {
            StockDataSetIterator iterator = new StockDataSetIterator(file, symbol, batchSize, exampleLength, splitRatio, category[ik]);
            log.info("Load test dataset...");
            List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();
            log.info("Build lstm networks...");
            MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

            log.info("Training...");
            for (int i = 0; i < epochs; i++) {
                while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
                iterator.reset(); // reset iterator
                net.rnnClearPreviousState(); // clear previous state
            }

            log.info("Saving model...");
            String file_path;
            if (ik == 0) {
                file_path = "E:/lipei/use/lstm/temp/StockVolLSTM" + stockname + ".zip";
            } else {
                file_path = "E:/lipei/use/lstm/temp/StockPriceLSTM" + stockname + ".zip";
            }
            File locationToSave = new File(file_path);
            // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
            ModelSerializer.writeModel(net, locationToSave, true);

            log.info("Load model...");
            net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

            log.info("Testing...");
            if (category.equals(PriceCategory.ALL)) {
                INDArray max = Nd4j.create(iterator.getMaxArray());
                INDArray min = Nd4j.create(iterator.getMinArray());
                predictAllCategories(net, test, max, min);
            } else {
                double max = iterator.getMaxNum(category[ik]);
                double min = iterator.getMinNum(category[ik]);
                res1 = predictPriceOneAhead(net, test, max, min, category[ik]);
            }
            if (ik == 0) {
                res.put("correctness_vol", res1.get("correctness"));
                res.put("similarity_vol", res1.get("similarity"));

            } else {
                res.put("correctness_price", res1.get("correctness"));
                res.put("similarity_price", res1.get("similarity"));

            }
            res.put("date", jSONObject.get("date"));
            res.put("name", jSONObject.get("name"));
        }
        log.info("Done...");

        return res;
    }

    /**
     * Predict one feature of a stock one-day ahead
     */
    private static JSONObject predictPriceOneAhead(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min, PriceCategory category) {
        double[] predicts = new double[testData.size()];
        double[] actuals = new double[testData.size()];
        JSONObject jSONObject = new JSONObject();
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getDouble(exampleLength - 1) * (max - min) + min;
            actuals[i] = testData.get(i).getValue().getDouble(0);
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict,Actual");
        for (int i = 0; i < predicts.length; i++) {
            int fh1 = 0;

            if (i > 0) {
                if ((predicts[i] > predicts[i - 1] && actuals[i] > actuals[i - 1]) || (predicts[i] < predicts[i - 1] && actuals[i] < actuals[i - 1])) {
                    fh1 = 1;
                    co1 = co1 + 1;
                } else {
                    fh1 = 2;
                    co2 = co2 + 1;
                }

            }
            log.info(predicts[i] + "," + actuals[i] + "," + fh1);
            data1[i] = predicts[i];
            data2[i] = actuals[i];
        }
//        log.info("Plot...");
//        PlotUtil.plot(predicts, actuals, String.valueOf(category));
        jSONObject.put("Predictions", data1);
        jSONObject.put("Actual", data2);
        jSONObject.put("correctness", (co1 / (co1 + co2)));
        spearman sp_cal = new spearman(data1, data2);
        jSONObject.put("similarity", sp_cal.getR());
        co1 = 0;
        co2 = 0;
        data1 = new double[100];
        data2 = new double[100];

        return jSONObject;

    }

    private static void predictPriceMultiple(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min) {
        // TODO
    }

    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    private static void predictAllCategories(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
        INDArray[] predicts = new INDArray[testData.size()];
        INDArray[] actuals = new INDArray[testData.size()];
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
            actuals[i] = testData.get(i).getValue();
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict\tActual");
        for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);
        log.info("Plot...");
        for (int n = 0; n < 5; n++) {
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            for (int i = 0; i < predicts.length; i++) {
                pred[i] = predicts[i].getDouble(n);
                actu[i] = actuals[i].getDouble(n);
            }
            String name;
            switch (n) {
                case 0:
                    name = "Stock OPEN Price";
                    break;
                case 1:
                    name = "Stock CLOSE Price";
                    break;
                case 2:
                    name = "Stock LOW Price";
                    break;
                case 3:
                    name = "Stock HIGH Price";
                    break;
                case 4:
                    name = "Stock VOLUME Amount";
                    break;
                default:
                    throw new NoSuchElementException();
            }
            PlotUtil.plot(pred, actu, name);
        }
    }

}
