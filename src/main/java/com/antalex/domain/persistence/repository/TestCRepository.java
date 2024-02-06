package com.antalex.domain.persistence.repository;

import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import org.springframework.data.repository.CrudRepository;

public interface TestCRepository extends CrudRepository<TestCEntity, Long> {
}