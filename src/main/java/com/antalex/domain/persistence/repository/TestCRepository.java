package com.antalex.domain.persistence.repository;

import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TestCRepository extends CrudRepository<TestCEntity, Long> {
    List<TestCEntity> findAllByValueLike(String value);
}