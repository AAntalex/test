package com.antalex.db.service;

import javax.persistence.EntityTransaction;

public interface SharedTransactionManager {
    EntityTransaction getTransaction();
    EntityTransaction getCurrentTransaction();
    void setAutonomousTransaction();
    void setParallelRun(Boolean parallelRun);
}
