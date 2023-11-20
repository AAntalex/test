package com.antalex.service.impl;

import com.antalex.config.SpringJdbcConfig;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.service.AdditionalParameterService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdditionalParameterImpl implements AdditionalParameterService {
    private final DataSource dataSource;
    private final AdditionalParameterRepository additionalParameterRepository;

    private static final String INS_QUERY = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, C_PARENT_ID, C_CODE, C_VALUE) VALUES (SEQ_ID.NEXTVAL, ?, ?, ?)";

    AdditionalParameterImpl(SpringJdbcConfig springJdbcConfig,
                            AdditionalParameterRepository additionalParameterRepository)
    {
        this.dataSource = springJdbcConfig.getDataSource();
        this.additionalParameterRepository = additionalParameterRepository;
    }

//    @Transactional
    @Override
    public void saveJPA(List<AdditionalParameterEntity> entities) {
        additionalParameterRepository.saveAll(entities);
    }

    @Override
    public void save(List<AdditionalParameterEntity> entities) {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement preparedStatement = connection.prepareStatement(INS_QUERY);
            entities.forEach(
                    entity -> {
                        try {
                            preparedStatement.setString(1, entity.getParentId());
                            preparedStatement.setString(2, entity.getCode());
                            preparedStatement.setString(3, entity.getValue());
                            preparedStatement.addBatch();
                        } catch (SQLException err) {
                            throw new RuntimeException(err);
                        }
                    }
            );

            preparedStatement.executeBatch();

            connection.commit();

            connection.close();

        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<AdditionalParameterEntity> generate(int count, String parentId, String codePrefix, String valuePrefix) {
        return IntStream
                .range(0, count)
                .mapToObj(
                        idx -> {
                            AdditionalParameterEntity entity = new AdditionalParameterEntity();
                            entity.setParentId(parentId);
                            entity.setCode(codePrefix + Integer.toString(idx));
                            entity.setValue(valuePrefix + Integer.toString(idx));
                            return entity;
                        })
                .collect(Collectors.toList());
    }
}
