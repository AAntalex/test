package com.antalex.prifiler.holder;

import com.antalex.prifiler.model.TimeCounter;

import java.util.Optional;

public class TimeCounterHolder {
    private TimeCounterHolder() {
        throw new IllegalStateException("Cache holder class!!!");
    }

    private static final ThreadLocal<TimeCounter> timeCounterThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isStartedThreadLocal = new ThreadLocal<>();

    public static TimeCounter getTimeCounter() {
        return timeCounterThreadLocal.get();
    }
    public static void setTimeCounter(TimeCounter timeCounter) {
        timeCounterThreadLocal.set(timeCounter);
    }
    public static Boolean getIsStarted() {
        return Optional.ofNullable(isStartedThreadLocal.get()).orElse(false);
    }
    public static void setIsStarted(Boolean isStarted) {
        isStartedThreadLocal.set(isStarted);
    }
}
