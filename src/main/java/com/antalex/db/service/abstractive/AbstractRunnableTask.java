package com.antalex.db.service.abstractive;

import com.antalex.db.service.api.RunnableTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AbstractRunnableTask implements RunnableTask {
    private ExecutorService executorService;
    private String name;
    private String error;
    private Future future;
    private List<Runnable> steps = new ArrayList<>();
    private boolean isRunning;

    @Override
    public void run() {
        this.future = this.executorService.submit(() ->
                steps
                    .forEach(step -> {
                        try {
                            step.run();
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
                this.future.get();
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
        }
    }

    @Override
    public void addStep(Runnable target) {
        steps.add(target);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public String getError() {
        return this.error;
    }
}
