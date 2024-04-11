package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;

import javax.persistence.EntityTransaction;
import java.util.List;

public interface DomainEntityManager {
/*
    <T extends Domain> T save(T domain);
    <T extends Domain> Iterable<T> saveAll(Iterable<T> domains);
    <T extends Domain> T update(T domain);
    <T extends Domain> Iterable<T> updateAll(Iterable<T> domains);
    <T extends Domain> boolean lock(T domain);
*/

    <T extends Domain> T newDomain(Class<T> clazz);
    <T extends Domain, M extends ShardInstance> T map(Class<T> clazz, M entity);
    <T extends Domain> T find(Class<T> clazz, Long id);


/*
    <T extends Domain> T find(T domain);
    <T extends Domain> List<T> findAll(Class<T> clazz, String condition, Object... binds);
    <T extends Domain> List<T> findAll(Class<T> clazz, ShardInstance parent, String condition, Object... binds);

    EntityTransaction getTransaction();
    String getTransactionUUID();
    void setAutonomousTransaction();
    void addParallel();

    default  <T extends Domain> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null);
    }
*/
}
