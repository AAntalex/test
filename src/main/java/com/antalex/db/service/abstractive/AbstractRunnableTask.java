package com.antalex.db.service.abstractive;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.RunnableQuery;
import com.antalex.db.service.api.RunnableTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public abstract class AbstractRunnableTask implements RunnableTask {
    protected ExecutorService executorService;
    protected String name;
    private List<Step> steps = new ArrayList<>();
    private String error;
    private Future future;
    private boolean isRunning;

    @Override
    public void run() {
        this.future = this.executorService.submit(() ->
                steps
                    .forEach(step -> {
                        try {
                            log.debug(String.format("Running \"%s\", step \"%s\"...", this.name, step.name));
                            step.target.run();
                        } catch (Exception err) {
                            this.error = err.getLocalizedMessage();
                            throw new RuntimeException(err);
                        }
                    })
        );
        this.isRunning = true;
    }

    @Override
    public void waitTask() {
        if (this.isRunning) {
            try {
                log.debug(String.format("Waiting \"%s\"...", this.name));
                this.future.get();
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
        }
    }

    @Override
    public void addStep(Runnable target, String name) {
        steps.add(new Step(target, name));
    }

    @Override
    public void addStep(Runnable target) {
        this.addStep(target, String.valueOf(steps.size() + 1));
    }

    @Override
    public RunnableQuery addQuery(String query, QueryType queryType) {
        return addQuery(query, queryType, String.valueOf(steps.size() + 1));
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getError() {
        return this.error;
    }

    private class Step {
        private Runnable target;
        private String name;

        Step(Runnable target, String name) {
            this.target = target;
            this.name = name;
        }
    }
}
