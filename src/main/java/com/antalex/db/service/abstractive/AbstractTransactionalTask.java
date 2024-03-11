package com.antalex.db.service.abstractive;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.service.api.TransactionalTask;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractTransactionalTask implements TransactionalTask {
    private static final String DELIMETER_CONTENT = "\n";

    protected ExecutorService executorService;
    protected String name;
    protected String errorCompletion;
    protected Future future;
    protected TaskStatus status = TaskStatus.CREATED;
    protected boolean parallelCommit;
    protected Shard shard;

    private String error;
    private Map<String, TransactionalQuery> queries = new HashMap<>();
    private List<TransactionalQuery> dmlQueries = new ArrayList<>();
    private TransactionalTask mainTask;
    private List<Step> steps = new ArrayList<>();
    private List<Step> commitSteps = new ArrayList<>();
    private List<Step> rollbackSteps = new ArrayList<>();
    private List<Step> afterCommitSteps = new ArrayList<>();
    private List<Step> afterRollbackSteps = new ArrayList<>();

    @Override
    public void run(Boolean parallelRun) {
        if (this.status == TaskStatus.CREATED) {
            Runnable target = () ->
                    steps.stream()
                            .forEachOrdered(step -> {
                                if (this.error == null) {
                                    try {
                                        log.debug(
                                                String.format("Running \"%s\", step \"%s\"...", this.name, step.name)
                                        );
                                        step.target.run();
                                    } catch (Exception err) {
                                        this.error = step.name + ":\n" + err.getLocalizedMessage();
                                    }
                                }
                            });
            if (parallelRun) {
                this.future = this.executorService.submit(target);
                this.status = TaskStatus.RUNNING;
            } else {
                target.run();
                this.status = TaskStatus.DONE;
                this.parallelCommit = false;
            }
        }
    }

    @Override
    public void waitTask() {
        if (this.status == TaskStatus.RUNNING) {
            try {
                log.debug(String.format("Waiting \"%s\"...", this.name));
                this.future.get();
            } catch (Exception err) {
                throw new ShardDataBaseException(err);
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
                        Stream.concat(steps.stream(), (rollback ? afterRollbackSteps : afterCommitSteps).stream())
                                .forEachOrdered(step -> {
                                    if (this.errorCompletion == null) {
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
                                    }
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
    public void addStepAfterCommit(Runnable target, String name) {
        afterCommitSteps.add(new Step(target, name));
    }

    @Override
    public void addStepAfterCommit(Runnable target) {
        addStepAfterCommit(target, String.valueOf(afterCommitSteps.size() + 1));
    }

    @Override
    public void addStepAfterRollback(Runnable target, String name) {
        afterRollbackSteps.add(new Step(target, name));
    }

    @Override
    public void addStepAfterRollback(Runnable target) {
        addStepAfterRollback(target, String.valueOf(afterRollbackSteps.size() + 1));
    }

    @Override
    public List<TransactionalQuery> getDmlQueries() {
        return dmlQueries;
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

    @Override
    public void setParallelCommit(boolean parallelCommit) {
        this.parallelCommit = parallelCommit;
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
    public TransactionalTask getMainTask() {
        return this.mainTask;
    }

    @Override
    public void setMainTask(TransactionalTask mainTask) {
        this.mainTask = mainTask;
    }

    @Override
    public TransactionalQuery addQuery(String query, QueryType queryType, String name) {
        TransactionalQuery transactionalQuery = this.queries.get(query);
        if (transactionalQuery == null) {
            transactionalQuery = createQuery(query, queryType);
            this.queries.put(query, transactionalQuery);
            if (queryType == QueryType.DML || queryType == QueryType.LOCK) {
                dmlQueries.add(transactionalQuery);
            }
            if (queryType != QueryType.LOCK) {
                this.addStep((Runnable) transactionalQuery, name);
            }
        }
        return transactionalQuery;
    }

    @Override
    public TransactionalQuery addQuery(String query, QueryType queryType) {
        return addQuery(query, queryType, query);
    }
}
