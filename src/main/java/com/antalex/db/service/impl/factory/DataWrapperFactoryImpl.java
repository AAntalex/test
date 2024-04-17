package com.antalex.db.service.impl.factory;

import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import com.antalex.db.service.impl.wrapers.JSonWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataWrapperFactoryImpl implements DataWrapperFactory {
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public DataWrapper createDataWrapper(DataFormat dataFormat) {
        if (dataFormat == DataFormat.JSON) {
            return new JSonWrapper(objectMapper);
        }
        return null;
    }
}
