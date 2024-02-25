package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.abstractive.AbstractTransactionalTask;
import com.antalex.db.service.api.TransactionalQuery;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionalSQLTask extends AbstractTransactionalTask {
    private Connection connection;
    private Map<String, TransactionalQuery> queries = new HashMap<>();

    TransactionalSQLTask(Connection connection, ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
    }

    @Override
    public void commit() throws SQLException {
        completion(false);
    }

    @Override
    public void rollback() throws SQLException {
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
    public TransactionalQuery addQuery(String query, QueryType queryType, String name) {
        TransactionalQuery transactionalQuery = this.queries.get(query);
        if (transactionalQuery == null) {
            try {
                if (queryType == QueryType.DML) {
                    connection.setAutoCommit(false);
                }
                transactionalQuery = new TransactionalSQLQuery(queryType, connection.prepareStatement(query));
            } catch (SQLException err) {
                throw new RuntimeException(err);
            }
            this.queries.put(query, transactionalQuery);
            this.addStep((Runnable) transactionalQuery, name);
        }
        return transactionalQuery;
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
