package com.antalex.domain.persistence.repository;

import com.antalex.domain.persistence.entity.TestBEntity;
import org.springframework.data.repository.CrudRepository;

public interface TestBRepository extends CrudRepository<TestBEntity, Long> {
}