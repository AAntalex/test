package com.antalex.service.impl;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.app.Account;
import com.antalex.domain.persistence.entity.shard.app.Client;
import com.antalex.domain.persistence.entity.shard.app.MainDocum;
import com.antalex.domain.persistence.entity.shard.app.ClientCategory;
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
public class GenerateMainDocum implements GenerateService<MainDocum> {
    private final ShardEntityManager entityManager;

    @Override
    public List<MainDocum> generate(String accountPrefix, int cnt, int cntAccount, int cntClient) {
        ClientCategory categoryVip =
                Optional.ofNullable(entityManager.find(ClientCategory.class, "${code}=?", "VIP"))
                        .orElseGet(() -> {
                            ClientCategory clientCategoryEntity = entityManager.newEntity(ClientCategory.class);
                            clientCategoryEntity.setCode("VIP");
                            return clientCategoryEntity;
                        });

        List<Client> clientEntities = entityManager.findAll(Client.class);
        clientEntities.addAll(
                IntStream.rangeClosed(clientEntities.size() + 1, cntClient)
                        .mapToObj(idx ->
                                entityManager.newEntity(Client.class)
                                        .name("CLIENT" + idx)
                                        .category(idx % 100 == 1 ? categoryVip : null)
                        )
                        .toList()
        );
        entityManager.updateAll(clientEntities);
        List<Client> sortClientEntities = clientEntities.stream().sorted(Comparator.comparing(Client::name)).toList();

        List<Account> accounts = entityManager.findAll(Account.class);
        accounts.addAll(
                IntStream.rangeClosed(accounts.size() + 1, cntAccount)
                        .mapToObj(idx ->
                                entityManager.newEntity(Account.class)
                                        .code(
                                                accountPrefix +
                                                        StringUtils.leftPad(
                                                                String.valueOf(idx),
                                                                20 - accountPrefix.length(),
                                                                '0')
                                        )
                                        .saldo(BigDecimal.ZERO)
                                        .dateOpen(OffsetDateTime.now())
                                        .client(sortClientEntities.get(idx % cntClient))
                        )
                        .toList()
        );
        entityManager.updateAll(accounts);
        List<Account> sortAccounts = accounts.stream().sorted(Comparator.comparing(Account::code)).toList();

        List<MainDocum> documents = entityManager.findAll(MainDocum.class);
        documents.addAll(
                IntStream.rangeClosed(documents.size() + 1, cnt)
                        .mapToObj(idx ->
                                entityManager.newEntity(MainDocum.class)
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
