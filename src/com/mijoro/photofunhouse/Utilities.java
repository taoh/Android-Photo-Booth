package com.mijoro.photofunhouse;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class Utilities {
    static final public int nextPowerOfTwo(int x) {
        double val = (double) x;
        return (int) Math.pow(2, Math.ceil(log2(val)));
    }
    static final public double log2(double x) {
        return Math.log(x)/Math.log(2);
    }
    
    public static String readRawFile(Context c, int id) {
        InputStream is = c.getResources().openRawResource(id);
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        try {
            for (int n; (n = is.read(b)) != -1;) {
                out.append(new String(b, 0, n));
            }
        } catch (IOException e) {
            return null;
        }
        return out.toString();
    }
}
