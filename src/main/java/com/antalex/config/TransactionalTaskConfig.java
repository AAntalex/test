package com.antalex.config;

import com.antalex.db.service.api.TransactionalExternalTaskFactory;
import com.antalex.service.impl.TestTaskFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionalTaskConfig {
    @Bean(name="externalTaskFactory")
    public TransactionalExternalTaskFactory getExternalTaskFactory() {
        return new TestTaskFactoryImpl();
    }
}
