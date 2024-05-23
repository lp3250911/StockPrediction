'''
Created on 2020年1月30日

@author: JM
'''
import pandas as pd
import tushare as ts
# ts.set_token('cddfe387bfad856561cfc792f0b45b287e25131aaf686e7c544b0ff9')
import MySQLdb
from sqlalchemy import create_engine

engine_ts = create_engine('mysql://root:Root@1234@127.0.0.1:3306/tushare?charset=utf8&use_unicode=1')

def read_data():
    sql = """SELECT * FROM tushare LIMIT 20"""
    df = pd.read_sql_query(sql, engine_ts)
    return df


def write_data(df):
    res = df.to_sql('tushare', engine_ts, index=False, if_exists='append', chunksize=5000)
    print(res)


def get_data():
    pro = ts.pro_api('cddfe387bfad856561cfc792f0b45b287e25131aaf686e7c544b0ff9')
    # df1=pro.user(token='cddfe387bfad856561cfc792f0b45b287e25131aaf686e7c544b0ff9')
    df = pro.stock_basic()
    # print(df1)
    return df


if __name__ == '__main__':
    #     df = read_data()


    df = get_data()
    write_data(df)
    print(df)