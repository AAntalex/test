package com.antalex.prifiler.service.impl;

import com.antalex.prifiler.holder.TimeCounterHolder;
import com.antalex.prifiler.model.TimeCounter;
import com.antalex.prifiler.service.ProfilerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class ProfilerServiceImpl implements ProfilerService {
    private static final String PROFILER_METHOD = "PROFILER";

    @Override
    public Boolean isStarted() {
        return TimeCounterHolder.getIsStarted();
    }

    @Override
    public void start(String name) {

        System.out.println("AAAAAAAAAAA START !!!");

        TimeCounterHolder.setIsStarted(true);

        TimeCounter timeCounter = new TimeCounter(name);
        timeCounter.setMethod(String.valueOf(PROFILER_METHOD));
        timeCounter.setStartTime(System.currentTimeMillis());
        timeCounter.addCount();

        TimeCounterHolder.setTimeCounter(timeCounter);
    }

    @Override
    public void stop() {
        System.out.println("AAAAAAAAAAA STOP !!!");

        TimeCounterHolder.getTimeCounter().fix();
        TimeCounterHolder.setIsStarted(false);
    }

    @Override
    public TimeCounter getTimeCounter() {
        return TimeCounterHolder.getTimeCounter();
    }

    @Override
    public void startTimeCounter(String name, String method) {
        if (this.isStarted()) {
            TimeCounterHolder.setTimeCounter(this.getTimeCounter().start(name, method));
        }
    }

    @Override
    public void fixTimeCounter() {
        if (this.isStarted()) {
            TimeCounterHolder.setTimeCounter(this.getTimeCounter().fix());
        }
    }

    @Override
    public String timeToString(long timeMillis) {
        BigDecimal time = new BigDecimal(timeMillis).divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP);
        return time.divide(BigDecimal.valueOf(3600), RoundingMode.FLOOR).toString()
                .concat(" ч. ")
                .concat(
                        time.remainder(BigDecimal.valueOf(3600))
                                .divide(BigDecimal.valueOf(60), RoundingMode.FLOOR)
                                .toString()
                )
                .concat(" мин. ")
                .concat(
                        new BigDecimal(timeMillis).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)
                                .remainder(BigDecimal.valueOf(60)).toString()
                )
                .concat(" сек. ")
                .concat("(").concat(String.valueOf(timeMillis)).concat(" mills)");
    }

    private String timeCounterToString(TimeCounter timeCounter, int level, boolean isAll) {

        String result = (level > 0 ? StringUtils.leftPad(String.valueOf("\t"), level, String.valueOf("\t")) : "")
                .concat(
                        Optional
                                .ofNullable(timeCounter.getMethod())
                                .map(it -> it.concat(String.valueOf(": ")))
                                .orElse(String.valueOf("<NULL>: "))
                )
                .concat(timeCounter.getName())
                .concat(
                        isAll && !PROFILER_METHOD.equals(timeCounter.getMethod())
                                ? String.valueOf(" (").concat(String.valueOf(timeCounter.getCount())).concat(")")
                                : ""
                )
                .concat(String.valueOf(" - "))
                .concat(
                        isAll
                                ? this.timeToString(timeCounter.getAllTime())
                                : this.timeToString(timeCounter.getCurrentTime())
                )
                .concat("\n");
        result = timeCounter.getTimeCounterList()
                .stream()
                .map(it -> this.timeCounterToString(it, level+1, isAll))
                .reduce(result, String::concat);
        return result;
    }

    @Override
    public String printTimeCounter(TimeCounter timeCounter, boolean current) {
        return timeCounterToString(timeCounter, 0, !current);
    }

    @Override
    public String printTimeCounter(String name, boolean current) {
        return Optional.ofNullable(this.getTimeCounter())
                .map(TimeCounter::getTimeCounterMap)
                .map(it -> it.get(name))
                .map(it -> this.printTimeCounter(it, current))
                .orElse("");
    }

    @Override
    public String printTimeCounter(boolean current) {
        return printTimeCounter(this.getTimeCounter(), current);
    }

    @Override
    public String printTimeCounter(TimeCounter timeCounter) {
        return printTimeCounter(timeCounter, false);
    }

    @Override
    public String printTimeCounter(String name) {
        return printTimeCounter(name, false);
    }

    @Override
    public String printTimeCounter() {
        return printTimeCounter(false);
    }
}
