package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableExternalTaskFactory;
import com.antalex.db.service.api.RunnableSQLTaskFactory;
import com.antalex.db.service.api.SharedTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SharedTransactionFactoryImpl implements SharedTransactionFactory {
    @Autowired
    private RunnableSQLTaskFactory runnableSQLTaskFactory;
    @Autowired
    private RunnableExternalTaskFactory runnableExternalTaskFactory;

    @Override
    public SharedEntityTransaction createTransaction() {
        return null;
    }
}
