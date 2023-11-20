package com.antalex.service;

import com.antalex.domain.persistence.entity.AdditionalParameterEntity;

import java.util.List;

public interface AdditionalParameterService {
    void saveJPA(List<AdditionalParameterEntity> entities);
    void save(List<AdditionalParameterEntity> entities);
    List<AdditionalParameterEntity> generate(int count, String parentId, String codePrefix, String valuePrefix);
}
