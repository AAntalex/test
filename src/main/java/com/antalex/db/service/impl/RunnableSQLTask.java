package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.abstractive.AbstractRunnableTask;
import com.antalex.db.service.api.RunnableQuery;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
public class RunnableSQLTask extends AbstractRunnableTask {
    private Connection connection;
    private Map<String, RunnableQuery> queries = new HashMap<>();

    RunnableSQLTask(Connection connection, ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
    }

    @Override
    public void confirm() throws SQLException {
        completion(false);
    }

    @Override
    public void revoke() throws SQLException {
        completion(true);
    }

    @Override
    public void finish() {
        if (this.status == TaskStatus.COMPLETION) {
            try {
                if (this.parallelCommit) {
                    this.future.get();
                }
                if (!this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (Exception err) {
                this.error = err.getLocalizedMessage();
            }
            this.status = TaskStatus.FINISHED;
        }
    }

    @Override
    public RunnableQuery addQuery(String query, QueryType queryType, String name) {
        RunnableQuery runnableQuery = this.queries.get(query);
        if (runnableQuery == null) {
            try {
                if (queryType == QueryType.DML) {
                    connection.setAutoCommit(false);
                }
                runnableQuery = new RunnableSQLQuery(queryType, connection.prepareStatement(query));
            } catch (SQLException err) {
                throw new RuntimeException(err);
            }
            this.queries.put(query, runnableQuery);
            this.addStep((Runnable) runnableQuery, name);
        }
        return runnableQuery;
    }

    public Connection getConnection() {
        return connection;
    }

    private void completion(boolean revoke) throws SQLException {
        if (this.status == TaskStatus.DONE) {
            if (!this.connection.isClosed() && !this.connection.getAutoCommit()) {
                Runnable target = () -> {
                    try {
                        log.debug(String.format("%s for \"%s\"...", revoke ? "ROLLBACK" : "COMMIT", this.name));
                        if (revoke) {
                            this.connection.rollback();
                        } else {
                            this.connection.commit();
                        }
                    } catch (SQLException err) {
                        this.error = err.getLocalizedMessage();
                    }
                };
                if (this.parallelCommit) {
                    this.future = this.executorService.submit(target);
                } else {
                    target.run();
                }
            }
            this.status = TaskStatus.COMPLETION;
        }
    }
}
