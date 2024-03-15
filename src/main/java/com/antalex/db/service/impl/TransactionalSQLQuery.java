package com.antalex.db.service.impl;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.TransactionalQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TransactionalSQLQuery implements TransactionalQuery, Runnable {
    private PreparedStatement preparedStatement;
    private ExecutorService executorService;
    private Boolean parallelRun;
    private TransactionalSQLQuery mainQuery;
    private QueryType queryType;
    private ResultSet result;
    private int currentIndex;
    private boolean isButch;
    private int count;
    private String query;
    private List<TransactionalQuery> relatedQueries = new ArrayList<>();

    TransactionalSQLQuery(String query, QueryType queryType, PreparedStatement preparedStatement) {
        this.queryType = queryType;
        this.preparedStatement = preparedStatement;
        this.query = query;
    }

    @Override
    public void addRelatedQuery(TransactionalQuery query) {
        relatedQueries.add(query);
    }

    @Override
    public void execute() {
        if (queryType != QueryType.DML) {
            run();
        }
    }

    @Override
    public TransactionalQuery bind(Object o) {
        try {
            preparedStatement.setObject(++currentIndex, o);
            relatedQueries.forEach(query -> query.bind(o));
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
        return this;
    }

    @Override
    public TransactionalQuery addBatch() {
        if (queryType != QueryType.DML) {
            throw new ShardDataBaseException("Метод addBatch предназначен только для DML запросов");
        }
        this.increment();
        this.currentIndex = 0;
        this.isButch = true;
        try {
            this.preparedStatement.addBatch();
            relatedQueries.forEach(TransactionalQuery::addBatch);
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
        return this;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public ResultSet getResult() {
        return result;
    }

    @Override
    public QueryType getQueryType() {
        return queryType;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void run() {
        try {
            if (queryType == QueryType.DML) {
                if (this.isButch) {
                    preparedStatement.executeBatch();
                } else {
                    this.increment();
                    preparedStatement.executeUpdate();
                }
            } else {
                this.increment();
                this.result = preparedStatement.executeQuery();
            }
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
    }

    @Override
    public void setMainQuery(TransactionalQuery mainQuery) {
        this.mainQuery = (TransactionalSQLQuery) mainQuery;
    }

    @Override
    public TransactionalQuery getMainQuery() {
        return mainQuery;
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void setParallelRun(Boolean parallelRun) {
        this.parallelRun = parallelRun;
    }

    private void increment() {
        this.count++;
        if (this.mainQuery != null) {
            this.mainQuery.increment();
        }
    }
}
