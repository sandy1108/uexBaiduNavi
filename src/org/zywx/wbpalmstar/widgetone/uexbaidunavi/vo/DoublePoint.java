package org.zywx.wbpalmstar.widgetone.uexbaidunavi.vo;

/**
 * Created by ylt on 15/4/7.
 */
public class DoublePoint {
    public double x;
    public double y;

    public DoublePoint(double x,double y){
        this.x=x;
        this.y=y;
    }

    public  DoublePoint(){

    }

    public double getX() {
        return x;
    }

    public void set(double x,double y){
        this.x=x;
        this.y=y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
