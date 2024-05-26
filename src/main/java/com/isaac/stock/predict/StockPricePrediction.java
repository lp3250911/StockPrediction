package com.isaac.stock.predict;

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
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.Math.abs;


/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class StockPricePrediction {
    static String url = "jdbc:mysql://127.0.0.1:3306/akshare";
    static String user = "root";
    static String password = "Root";
    private static double co1 = 0;
    private static double co2 = 0;
    private static double[] data1 = new double[100];
    private static double[] data2 = new double[100];


    private static final Logger log = LoggerFactory.getLogger(StockPricePrediction.class);

    private static int exampleLength = 5; // time series length, assume 22 working days per month

//    public static void main(String[] args) throws IOException, SQLException {
//        JSONObject jSONObject = new JSONObject();
//
//
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

    public static void main(String[] args) throws IOException, SQLException {
        JSONObject jSONObject = new JSONObject();
        his_ansys_main(22,"000670");



    }

    private static JSONObject predict_run(String stockname) throws IOException, InterruptedException {
        String file = "E:/lipei/use/lstm/temp/inputdata_predict" + stockname + ".csv";
        String symbol = stockname; // stock name
        DatabaseToCSV databaseToCSV = new DatabaseToCSV();
        JSONObject jSONObject = databaseToCSV.getStocksData(symbol,0);
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

    private static void his_ansys_main(int day_co,String stockname) {
        double[] correctness_vol = new double[100];
        double[] correctness_price = new double[100];
        double[] similarity_vol = new double[100];
        double[] similarity_price = new double[100];
        String[] date = new String[100];


        for (int i = day_co; i >0; i--) {
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                Statement statement = conn.createStatement();
                JSONObject jSONObject = his_ansys(stockname,i);
                correctness_vol[day_co-i]=Double.parseDouble(jSONObject.get("correctness_vol").toString());
                correctness_price[day_co-i]=Double.parseDouble(jSONObject.get("correctness_price").toString());
                similarity_vol[day_co-i]=Double.parseDouble(jSONObject.get("similarity_vol").toString());
                similarity_price[day_co-i]=Double.parseDouble(jSONObject.get("similarity_price").toString());
                date[day_co-i]=jSONObject.get("date").toString();
            } catch (InterruptedException | SQLException | IOException e) {
                e.printStackTrace();
            }
            PlotUtil.plot(correctness_vol, correctness_price, "正确率");
            PlotUtil.plot(similarity_vol,similarity_price,"相似度");
        }

    }

    private static JSONObject his_ansys(String stockname, int day_co) throws IOException, InterruptedException {
        String file = "E:/lipei/use/lstm/temp/inputdata_predict" + stockname + ".csv";
        String symbol = stockname; // stock name
        DatabaseToCSV databaseToCSV = new DatabaseToCSV();
        JSONObject jSONObject = databaseToCSV.getStocksData(symbol,day_co);
        JSONObject res = new JSONObject();
        JSONObject res1 = new JSONObject();


        int batchSize = 64; // mini-batch size
        double splitRatio = 0.9; // 90% for training, 10% for testing
        int epochs = 44; // training epochs

        log.info("Create dataSet iterator...");
//        Thread.sleep(10000);
        PriceCategory[] category = {PriceCategory.CLOSE, PriceCategory.VOLUME}; // CLOSE: predict close price
        for (int ik = 0; ik < 2; ik++) {
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
                double max = iterator.getMaxNum(category[ik]);
                double min = iterator.getMinNum(category[ik]);
                res1 = predictPriceOneAhead(net, test, max, min, category[ik]);
            }
            if(ik==1) {
                res.put("correctness_vol",res1.get("correctness"));
                res.put("similarity_vol",res1.get("similarity"));

            }else{
                res.put("correctness_price",res1.get("correctness"));
                res.put("similarity_price",res1.get("similarity"));

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
