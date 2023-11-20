package com.antalex.prifiler.model;

import java.util.*;

public class TimeCounter {
    private String name;
    private long startTime;
    private long currentTime;
    private long allTime;
    private int count;
    private String method;
    private TimeCounter parentTimeCounter;
    private List<TimeCounter> timeCounterList = new ArrayList<>();
    private Map<String, TimeCounter> timeCounterMap = new HashMap<>();

    public void addCount() {
        this.count++;
    }

    public TimeCounter(String name) {
        this.name = name;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public List<TimeCounter> getTimeCounterList() {
        return timeCounterList;
    }

    public Map<String, TimeCounter> getTimeCounterMap() {
        return timeCounterMap;
    }

    public TimeCounter start(String name) {
        if (Objects.isNull(name)) {
            throw new IllegalStateException("Не задано имя счетчика!");
        }
        TimeCounter timeCounter;
        if (this.timeCounterMap.containsKey(name)) {
            timeCounter = this.timeCounterMap.get(name);
        } else {
            timeCounter = new TimeCounter(name);
            timeCounter.setParentTimeCounter(this);
            this.timeCounterList.add(timeCounter);
            this.timeCounterMap.put(name, timeCounter);
        }
        timeCounter.setStartTime(System.currentTimeMillis());
        timeCounter.addCount();

        return timeCounter;
    }

    public TimeCounter start(String name, String method) {
        TimeCounter timeCounter = this.start(name);
        timeCounter.setMethod(method);
        return timeCounter;
    }

    public TimeCounter fix() {
        this.currentTime = System.currentTimeMillis() - this.startTime;
        this.allTime+=this.currentTime;
        if (Objects.nonNull(this.parentTimeCounter)) {
            return this.parentTimeCounter;
        } else {
            return this;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getAllTime() {
        return allTime;
    }

    public void setAllTime(long allTime) {
        this.allTime = allTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParentTimeCounter(TimeCounter parentTimeCounter) {
        this.parentTimeCounter = parentTimeCounter;
    }
}

