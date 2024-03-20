package com.antalex.db.service.impl;

import com.antalex.db.service.api.ResultQuery;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResultParallelQuery implements ResultQuery {
    private List<ResultQuery> results = new ArrayList<>();
    private ResultQuery currentResult;
    private int currentIndex;
    private int keyCount;
    private Set<String> keyValues = new HashSet<>();

    public ResultParallelQuery(int keyCount) {
        this.keyCount = keyCount;
    }

    public void add(ResultQuery result) {
        this.results.add(result);
    }

    @Override
    public boolean next() throws Exception {
        if (currentIndex >= results.size()) {
            return false;
        }
        if (currentResult == null) {
            this.currentResult = results.get(currentIndex);
        }
        if (!currentResult.next() || !uniqueKey()) {
            this.currentIndex++;
            next();
        }
        return true;
    }

    @Override
    public long getLong(int idx) throws Exception {
        return currentResult.getLong(idx);
    }

    @Override
    public Object getObject(int idx) throws Exception {
        return currentResult.getObject(idx);
    }

    @Override
    public short getShort(int idx) throws Exception {
        return currentResult.getShort(idx);
    }

    @Override
    public boolean getBoolean(int idx) throws Exception {
        return currentResult.getBoolean(idx);
    }

    @Override
    public String getString(int idx) throws Exception {
        return currentResult.getString(idx);
    }

    private boolean uniqueKey() throws Exception {
        if (keyCount > 0) {
            String key = StringUtils.EMPTY;
            for (int i = 1; i <= keyCount; i++) {
                key = key.concat(currentResult.getLong(i) + "#");
            }
            if (keyValues.contains(key)) {
                return false;
            }
            keyValues.add(key);
        }
        return true;
    }
}
