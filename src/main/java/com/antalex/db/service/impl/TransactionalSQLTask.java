package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.abstractive.AbstractTransactionalTask;
import com.antalex.db.service.api.TransactionalQuery;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionalSQLTask extends AbstractTransactionalTask {
    private Connection connection;

    TransactionalSQLTask(Connection connection, ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
    }

    @Override
    public void commit() throws SQLException {

        this.connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        this.connection.rollback();

    }

    @Override
    public Boolean needCommit() {
        try {
            return !this.connection.isClosed() && !this.connection.getAutoCommit();
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
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
                this.errorCompletion = err.getLocalizedMessage();
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
}
