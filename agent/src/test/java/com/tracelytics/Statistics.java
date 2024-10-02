package com.tracelytics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Improved from http://stackoverflow.com/a/7988556
 * 
 * Take note that this implementation is a simple approach and can return slightly off value for percentile exact definition.
 * 
 * But this should be good enough for general stat purposes
 * 
 * @author pluk
 *
 * @param <T>
 */
public class Statistics<T extends Number & Comparable<T>> {
    private List<T> data;
    private int size;   

    public Statistics(T... data) 
    {
        this.data = Arrays.asList(data);
        size = data.length;
    }
    
    public Statistics(List<T> data) 
    {
        this.data = data;
        size = data.size();
    }
    
    public T getSum() {
        Double sum = 0.0;
        for(T a : data) {
            sum += a.doubleValue();
        }
        return (T)sum;
    }
    
    public double getMean()
    {
        return getSum().doubleValue() / size;
    }

    public double getVariance()
    {
        double mean = getMean();
        double temp = 0;
        for(T a : data) {
            double diff = a.doubleValue() - mean;
            temp += diff * diff;
        }
        return temp / size;
    }

    public double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    
    public Number getMedian() 
    {
       Collections.sort(data);

       if (size % 2 == 0) {
          return (data.get((size / 2) - 1).doubleValue() + data.get(size / 2).doubleValue()) / 2.0;
       } else {
           return data.get(size / 2);
       }
    }
    
    public T getPercentile(double percentage) {
        Collections.sort(data);
        
        int percentileMark = (int) (size * percentage);
        if (percentileMark == size) {
            percentileMark -= 1;
        }
        return data.get(percentileMark);
    }
}
