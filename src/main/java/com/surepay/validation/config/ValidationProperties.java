package com.surepay.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "validation")
public class ValidationProperties {
    
    private Balance balance = new Balance();
    private Error error = new Error();
    private Pagination pagination = new Pagination();
    
    public Balance getBalance() {
        return balance;
    }
    
    public void setBalance(Balance balance) {
        this.balance = balance;
    }
    
    public Error getError() {
        return error;
    }
    
    public void setError(Error error) {
        this.error = error;
    }
    
    public Pagination getPagination() {
        return pagination;
    }
    
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
    
    public static class Balance {
        private BigDecimal tolerance = new BigDecimal("0.01");
        
        public BigDecimal getTolerance() {
            return tolerance;
        }
        
        public void setTolerance(BigDecimal tolerance) {
            this.tolerance = tolerance;
        }
    }
    
    public static class Error {
        private int batchSize = 1000;
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
    
    public static class Pagination {
        private int defaultPageSize = 1000;
        private int maxPageSize = 10000;
        
        public int getDefaultPageSize() {
            return defaultPageSize;
        }
        
        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }
        
        public int getMaxPageSize() {
            return maxPageSize;
        }
        
        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }
}

