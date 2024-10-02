package com.tracelytics.joboe;


/**
 * A bucket that contains tokens to be consumed. The bucket is refilled based on the replenish rate.
 * 
 * This class is converted from https://github.com/tracelytics/scribe/blob/FastTransformer/src/token_bucket.h
 * 
 * @author Patson Luk
 *
 */
public class TokenBucket {
    private double ratePerSecond; //replenish rate in second
    private double capacity; //capacity of the bucket, available token should never exceed this number
    private double availableTokens;
    private long lastCheck;
    
    public TokenBucket(double bucketCapacity, double ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        this.capacity = bucketCapacity;
        this.availableTokens = bucketCapacity;
        
        this.lastCheck = System.currentTimeMillis(); //do not use System.nanoTime as we need JRE 1.5 support
    }

    /**
     * Consumes one available token
     * @return true if available token is consumed, false if there is no available token to be consumed
     */
    public boolean consume() {
        return consume(1);
    }
    
    /**
     * Consumes # of available token as the size provided
     * @param size # of tokens to be consumed
     * @return
     */
    public synchronized boolean consume(int size) {
        updateAvailable();
        
        if (availableTokens >= size) {
            availableTokens -= size;
            
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Updates the available tokens based on last checked time and replenish rate
     */
    private synchronized void updateAvailable() {
        if (availableTokens < capacity) {
            //calculate time since last check
            long now =  System.currentTimeMillis();
            long delta = now - lastCheck;
            
         // return if "time went backwards", possible on some VMs or with NTP
            if (delta <= 0) {
                return;
            }
            
            //compute number of new tokens generated since last check
            double newTokens = ratePerSecond * (double)delta / 1000; //delta in millisec
            if (availableTokens + newTokens < capacity) {
                availableTokens = availableTokens + newTokens;
            } else {
                availableTokens = capacity;
            }
            
            // update time of last check
            lastCheck = now;
        }
    }
    
    /**
     * Sets the replenish rate 
     * @param ratePerSecond Replenish rate per second
     */
    synchronized void setRatePerSecond(double ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        
    }
    
    /**
     * Sets the capacity. This would adjust the available token if available token > capacity
     * @param capacity
     */
    synchronized void setCapacity(double capacity) {
        this.capacity = capacity;
        if (availableTokens > capacity) {
            availableTokens = capacity;
        }
    }
    
    double getRatePerSecond() {
        return ratePerSecond;
    }
    
    double getCapacity() {
        return capacity;
    }
}
