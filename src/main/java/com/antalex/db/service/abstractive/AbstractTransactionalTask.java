package com.antalex.db.service.abstractive;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.service.api.TransactionalTask;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public abstract class AbstractTransactionalTask implements TransactionalTask {
    protected ExecutorService executorService;
    protected String name;
    protected String error;
    protected String errorCompletion;
    protected Future future;
    protected TaskStatus status = TaskStatus.CREATED;
    protected boolean parallelCommit;
    protected Map<String, TransactionalQuery> queries = new HashMap<>();
    private List<Step> steps = new ArrayList<>();
    private List<Step> commitSteps = new ArrayList<>();
    private List<Step> rollbackSteps = new ArrayList<>();

    @Override
    public void run() {
        if (this.status == TaskStatus.CREATED) {
            this.future = this.executorService.submit(() ->
                    steps.stream()
                            .anyMatch(step -> {
                                try {
                                    log.debug(String.format("Running \"%s\", step \"%s\"...", this.name, step.name));
                                    step.target.run();
                                } catch (Exception err) {
                                    this.error = step.name + ":\n" + err.getLocalizedMessage();
                                }
                                return Objects.nonNull(this.error);
                            })
            );
            this.status = TaskStatus.RUNNING;
        }
    }

    @Override
    public void waitTask() {
        if (this.status == TaskStatus.RUNNING) {
            try {
                log.debug(String.format("Waiting \"%s\"...", this.name));
                this.future.get();
            } catch (Exception err) {
                throw new RuntimeException(err);
            } finally {
                this.status = TaskStatus.DONE;
            }
        }
    }

    @Override
    public void completion(boolean rollback) {
        if (this.status == TaskStatus.DONE) {
            this.status = TaskStatus.COMPLETION;
            List<Step> steps = rollback ? rollbackSteps : commitSteps;
            if (needCommit()) {
                steps.add(
                        new Step(
                                () -> {
                                    try {
                                        if (rollback) {
                                            this.rollback();
                                        } else {
                                            this.commit();
                                        }
                                    } catch (Exception err) {
                                        this.errorCompletion = err.getLocalizedMessage();
                                    }
                                },
                                rollback ? "ROLLBACK" : "COMMIT"
                        )
                );
            }
            if (steps.size() > 0) {
                Runnable target = () ->
                        steps.stream()
                                .anyMatch(step -> {
                                    try {
                                        log.debug(
                                                String.format(
                                                        "%s for \"%s\", step \"%s\"...",
                                                        rollback ? "ROLLBACK" : "COMMIT",
                                                        this.name,
                                                        step.name)
                                        );
                                        step.target.run();
                                    } catch (Exception err) {
                                        this.errorCompletion = err.getLocalizedMessage();
                                    }
                                    return Objects.nonNull(this.errorCompletion);
                                });
                if (this.parallelCommit) {
                    this.future = this.executorService.submit(target);
                } else {
                    target.run();
                }
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
    public void addStepBeforeRollback(Runnable target) {
        addStepBeforeRollback(target, String.valueOf(rollbackSteps.size() + 1));
    }

    @Override
    public void addStepBeforeRollback(Runnable target, String name) {
        rollbackSteps.add(new Step(target, name));
    }

    @Override
    public void addStepBeforeCommit(Runnable target) {
        addStepBeforeCommit(target, String.valueOf(commitSteps.size() + 1));
    }

    @Override
    public void addStepBeforeCommit(Runnable target, String name) {
        commitSteps.add(new Step(target, name));
    }

    @Override
    public TransactionalQuery addQuery(String query, QueryType queryType) {
        return addQuery(query, queryType, query);
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

    @Override
    public String getErrorCompletion() {
        return this.errorCompletion;
    }

    private class Step {
        private Runnable target;
        private String name;

        Step(Runnable target, String name) {
            this.target = target;
            this.name = name;
        }
    }

    @Override
    public void setParallelCommit(boolean parallelCommit) {
        this.parallelCommit = parallelCommit;
    }
}
