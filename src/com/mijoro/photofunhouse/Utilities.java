package com.mijoro.photofunhouse;

public class Utilities {
    static final public int nextPowerOfTwo(int x) {
        double val = (double) x;
        return (int) Math.pow(2, Math.ceil(log2(val)));
    }
    static final public double log2(double x) {
        return Math.log(x)/Math.log(2);
    }
}
