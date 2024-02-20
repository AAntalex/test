package com.antalex.db.service.api;

import com.antalex.db.service.impl.SharedEntityTransaction;

public interface SharedTransactionFactory {
    SharedEntityTransaction createTransaction();
}
