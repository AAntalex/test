package com.antalex.domain.persistence.repository;

import com.antalex.domain.persistence.entity.TestAEntity;
import org.springframework.data.repository.CrudRepository;

public interface TestARepository extends CrudRepository<TestAEntity, Long> {
}