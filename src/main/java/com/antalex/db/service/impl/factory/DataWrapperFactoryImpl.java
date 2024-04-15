package com.antalex.db.service.impl.factory;

import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import com.antalex.db.service.impl.wrapers.JSonWrapper;
import org.springframework.stereotype.Component;

@Component
public class DataWrapperFactoryImpl implements DataWrapperFactory {

    @Override
    public DataWrapper createDataWraper(DataFormat dataFormat) {
        if (dataFormat == DataFormat.JSON) {
            return new JSonWrapper();
        }
        return null;
    }
}
