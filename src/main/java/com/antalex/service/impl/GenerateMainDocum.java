package com.antalex.service.impl;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.app.AccountEntity;
import com.antalex.domain.persistence.entity.shard.app.ClientEntity;
import com.antalex.domain.persistence.entity.shard.app.ClientCategoryEntity;
import com.antalex.domain.persistence.entity.shard.app.MainDocumEntity;
import com.antalex.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GenerateMainDocum implements GenerateService<MainDocumEntity> {
    private final ShardEntityManager entityManager;

    @Override
    public List<MainDocumEntity> generate(String accountPrefix, int cnt, int cntAccount, int cntClient) {
        ClientCategoryEntity categoryVip =
                Optional.ofNullable(entityManager.find(ClientCategoryEntity.class, "${code}=?", "VIP"))
                        .orElseGet(() -> {
                            ClientCategoryEntity clientCategoryEntity = entityManager.newEntity(ClientCategoryEntity.class);
                            clientCategoryEntity.setCode("VIP");
                            return clientCategoryEntity;
                        });

        List<ClientEntity> clientEntities = entityManager.findAll(ClientEntity.class);
        clientEntities.addAll(
                IntStream.rangeClosed(clientEntities.size() + 1, cntClient)
                        .mapToObj(idx ->
                                entityManager.newEntity(ClientEntity.class)
                                        .name("CLIENT" + idx)
                                        .category(idx % 100 == 1 ? categoryVip : null)
                        )
                        .toList()
        );
        entityManager.updateAll(clientEntities);
        List<ClientEntity> sortClientEntities = clientEntities.stream().sorted(Comparator.comparing(ClientEntity::name)).toList();

        List<AccountEntity> accounts = entityManager.findAll(AccountEntity.class);
        accounts.addAll(
                IntStream.rangeClosed(accounts.size() + 1, cntAccount)
                        .mapToObj(idx ->
                                entityManager.newEntity(AccountEntity.class)
                                        .code(
                                                accountPrefix +
                                                        StringUtils.leftPad(
                                                                String.valueOf(idx),
                                                                20 - accountPrefix.length(),
                                                                '0')
                                        )
                                        .saldo(BigDecimal.ZERO)
                                        .dateOpen(OffsetDateTime.now())
                                        .clientEntity(sortClientEntities.get(idx % cntClient))
                        )
                        .toList()
        );
        entityManager.updateAll(accounts);
        List<AccountEntity> sortAccounts = accounts.stream().sorted(Comparator.comparing(AccountEntity::code)).toList();

        List<MainDocumEntity> documents = entityManager.findAll(MainDocumEntity.class);
        documents.addAll(
                IntStream.rangeClosed(documents.size() + 1, cnt)
                        .mapToObj(idx ->
                                entityManager.newEntity(MainDocumEntity.class)
                                        .num(idx)
                                        .sum(new BigDecimal("1.01"))
                                        .date(new Date())
                                        .dateProv(OffsetDateTime.now())
                                        .accDt(sortAccounts.get(idx % cntAccount))
                                        .accCt(sortAccounts.get((idx + 1) % cntAccount))
                        )
                        .toList()
        );
        return documents;
    }
}
