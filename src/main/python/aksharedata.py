import akshare as ak
import numpy as np
import pymysql
import pandas as pd
from sqlalchemy import create_engine
from datetime import datetime, timedelta
conn = create_engine('mysql://root:Root@127.0.0.1:3306/akshare?charset=utf8mb4&use_unicode=1')
conn1 = pymysql.connect(host='localhost', user='root', password='Root', port=3306, db='akshare', charset='utf8mb4')
conn2 = pymysql.connect(host='localhost', user='root', password='Root', port=3306, db='akshare', charset='utf8mb4')


# stock_zh_a_hist_df = ak.stock_zh_a_hist(symbol="688425", period="daily", start_date="20170101", end_date='20250101', adjust="")
# # print(stock_zh_a_hist_df)
# # 板块成分股
# stock_board_concept_cons_em_df = ak.stock_board_concept_cons_em(symbol="车联网")
# print(stock_board_concept_cons_em_df)
#
# 工业板块
# stock_board_industry_name_em_df = ak.stock_board_industry_name_em()

def stocks():
    # 获取指定股票概念的成分股，并保存至Excel文件。
    # param name: 股票概念名称
    # 获取所有股票概念及其成分股信息
    concept_stocks_df = ak.stock_board_concept_name_em()

    i=0
    for index,row_concept in concept_stocks_df.iterrows():

        if ak.stock_board_concept_cons_em(symbol=row_concept[1]).size>0:
            stock_board_concept_cons_em_df = ak.stock_board_concept_cons_em(symbol=row_concept[1])
            for index1,row_chengfengu in stock_board_concept_cons_em_df.iterrows():
                stock_zh_a_hist_df = ak.stock_zh_a_hist(symbol=row_chengfengu[1], period="daily", start_date="20210101", end_date='20250101', adjust="")
                stock_zh_a_hist_df['板块概念']=row_concept[1]
                stock_zh_a_hist_df['代码']=row_chengfengu[1]
                stock_zh_a_hist_df['名称']=row_chengfengu[2]
                pd.io.sql.to_sql(stock_zh_a_hist_df, row_chengfengu[1], conn, if_exists='replace')


        pass
    pass


# 获取A股股票列表
# stock_list_df = ak.stock_zh_a_spot_emission()
# print(stock_list_df)
#
# stock_info_sh_df = ak.stock_info_sh_name_code(indicator="主板A股")
# print(stock_info_sh_df)
def conceptbk():
    concept_stocks_df = ak.stock_board_concept_name_em()
    for index,row_concept in concept_stocks_df.iterrows():
        oard_concept_hist_em_df = ak.stock_board_concept_hist_em(symbol=row_concept[1], start_date="20210101", end_date="20250108", adjust="")
        pd.io.sql.to_sql(oard_concept_hist_em_df, row_concept[1], conn, if_exists='replace')

def etf_sel():
    # 获取上证ETF成分股
    # etf_data = ak.stock_em_etf_member_v1(symbol="510050")
    # print(etf_data)

    # 获取深证ETF成分股
    etf_df = ak.fund_etf_category_sina(symbol="ETF基金")
    for index,row_etf in etf_df.iterrows():
        etf_his_df=ak.fund_etf_hist_sina(symbol=row_etf[0])
        etf_his_df['名称']=row_etf[1]
        pd.io.sql.to_sql(etf_his_df, row_etf[0], conn, if_exists='replace')


def sel_stocks():#单独导入日线数据
    stock_zh_a_hist_df = ak.stock_zh_a_hist(symbol='603259', period="daily", start_date="20210101", end_date='20250101', adjust="")
    stock_zh_a_hist_df['板块概念']='医疗服务'
    stock_zh_a_hist_df['代码']='603259'
    stock_zh_a_hist_df['名称']='药明康德'
    pd.io.sql.to_sql(stock_zh_a_hist_df, '603259', conn, if_exists='replace')

def append_stocks():
    # 取表名
    sql_str = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'akshare'";
    cur1 = conn1.cursor()
    cur1.execute(sql_str)
    conn1.commit()
    rows = cur1.fetchall()
    for row in rows:
        stockname=row[0]
        if stockname.isdigit():
            stockname=row[0]
            cur2 = conn2.cursor()
            sel_sql="select * from `"+stockname+"` order by 日期 desc limit 1"
            cur2.execute(sel_sql)
            conn2.commit()
            lasttime=cur2.fetchall()
            for lt in lasttime:
                last_day=lt[1]
                bkgn=lt[12]
                code=lt[13]
                name=lt[14]
            date_str = last_day.strftime("%Y-%m-%d")
            date_obj = datetime.strptime(date_str, "%Y-%m-%d")
            next_day = date_obj + timedelta(days=1)
            str_day= next_day.strftime("%Y%m%d")
            stock_zh_a_hist_df = ak.stock_zh_a_hist(symbol=code, period="daily", start_date=str_day, end_date='20250101', adjust="")
            stock_zh_a_hist_df['板块概念']=bkgn
            stock_zh_a_hist_df['代码']=code
            stock_zh_a_hist_df['名称']=name
            pd.io.sql.to_sql(stock_zh_a_hist_df, code, conn, if_exists='append')

def append_conceptbk():
    # 取表名
    concept_stocks_df = ak.stock_board_concept_name_em()
    for index,row_concept in concept_stocks_df.iterrows():
        stockname=row_concept[1]
        cur2 = conn2.cursor()
        sel_sql="select * from `"+stockname+"` order by 日期 desc limit 1"
        cur2.execute(sel_sql)
        conn2.commit()
        lasttime=cur2.fetchall()
        for lt in lasttime:
            last_day=lt[1]
        date_str = last_day
        date_obj = datetime.strptime(date_str, "%Y-%m-%d")
        next_day = date_obj + timedelta(days=1)
        str_day= next_day.strftime("%Y%m%d")
        oard_concept_hist_em_df = ak.stock_board_concept_hist_em(symbol=row_concept[1], start_date=str_day, end_date="20250108", adjust="")
        pd.io.sql.to_sql(oard_concept_hist_em_df, row_concept[1], conn, if_exists='append')




def head_stocks_find():
    sql_str = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'akshare'";
    cur1 = conn1.cursor()
    cur1.execute(sql_str)
    conn1.commit()
    rows = cur1.fetchall()
    for row in rows:
        stockname=row[0]
        if stockname == 'ansys_results' or stockname == 'sel_stocks' or stockname == 'head_stocks':
            continue








if __name__ == '__main__':
    conceptbk()