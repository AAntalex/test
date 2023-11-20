package com.antalex.prifiler.service;

import com.antalex.prifiler.model.TimeCounter;

public interface ProfilerService {
    Boolean isStarted();
    void start(String name);
    void stop();
    TimeCounter getTimeCounter();
    void startTimeCounter(String name, String method);
    void fixTimeCounter();
    String timeToString(long timeMillis);
    String printTimeCounter(TimeCounter timeCounter);
    String printTimeCounter(String name);
    String printTimeCounter();
    String printTimeCounter(TimeCounter timeCounter, boolean current);
    String printTimeCounter(String name, boolean current);
    String printTimeCounter(boolean current);
}
