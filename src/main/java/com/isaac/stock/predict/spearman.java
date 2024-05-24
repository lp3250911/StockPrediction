package com.isaac.stock.predict;

import JSci.maths.statistics.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;


public class spearman {
    private double[] x;

    private double[] y;
    SpearmansCorrelation p=new SpearmansCorrelation();

    public spearman(double[] x, double[] y) {
        this.x = x;
        this.y = y;
    }

    public double getR() {

        return p.correlation(x, y);
    }
    public double getTValue() {
        double up=x.length-2;
        double r=getR();
        double down=1-(r*r);
        return r*Math.sqrt(up/down);
    }
    /***
     *
     * @param flag:true=双侧 false=单侧
     * @return
     */
    public double getPValue(boolean flag) {
        TDistribution td=new TDistribution(x.length-2);
        double t=getTValue();
        double cumulative = td.cumulative(t);
        double p=t>0?1-cumulative:cumulative;
        return flag?p*2:p;
    }
    public double getPValue() {
        return getPValue(true);
    }
}
