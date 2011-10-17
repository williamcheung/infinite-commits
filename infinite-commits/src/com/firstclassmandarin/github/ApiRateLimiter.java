package com.firstclassmandarin.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiRateLimiter {

    private static final ApiRateLimiter instance = new ApiRateLimiter();

    private static final Logger logger = LoggerFactory.getLogger(ApiRateLimiter.class);

    private static final int MAX_CALLS = 60;
    private static final int MAX_DURATION = 60*1000; // milliseconds
    private static final long TIME_TOLERANCE = 500; // milliseconds

    private boolean tracking = false;
    private int totalCalls;
    private long cycleStart; // milliseconds

    private ApiRateLimiter() {
    }

    public static ApiRateLimiter getInstance() {
        return instance;
    }

    public synchronized void preCall() {
        if (!tracking) {
            tracking = true;
            totalCalls = 0;
            cycleStart = System.currentTimeMillis();
        } else if (totalCalls >= MAX_CALLS) {
            long accumulatedTime = System.currentTimeMillis() - cycleStart;
            if (accumulatedTime < MAX_DURATION) {
                tracking = false;
                long sleepTime = MAX_DURATION - accumulatedTime + TIME_TOLERANCE;
                logger.info("Sleep {} to reduce API usage rate.", sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public synchronized void postCall() {
        totalCalls ++;
    }
}
