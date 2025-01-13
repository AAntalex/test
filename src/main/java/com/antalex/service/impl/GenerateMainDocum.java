package com.antalex.service.impl;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.app.Account;
import com.antalex.domain.persistence.entity.shard.app.Client;
import com.antalex.domain.persistence.entity.shard.app.ClientCategory;
import com.antalex.domain.persistence.entity.shard.app.MainDocum;
import com.antalex.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class GenerateMainDocum implements GenerateService<MainDocum> {
    private final ShardEntityManager entityManager;

    @Override
    public List<MainDocum> generate(int cnt, int cntClient, int cntAccount) {
        ClientCategory categoryVip =
                Optional.ofNullable(entityManager.find(ClientCategory.class, "${code}=?", "VIP"))
                        .orElseGet(() -> {
                            ClientCategory clientCategory = new ClientCategory();
                            clientCategory.setCode("VIP");
                            return clientCategory;
                        });

        List<Client> clients = entityManager.findAll(Client.class);
        clients.addAll(
                IntStream.rangeClosed(clients.size() + 1, cntClient)
                        .mapToObj(idx ->
                                new Client()
                                        .name("CLIENT" + idx)
                                        .category(idx % 100 == 1 ? categoryVip : null)
                        )
                        .toList()
        );

        List<Account> accounts = entityManager.findAll(Account.class);
        accounts.addAll(
                IntStream.rangeClosed(accounts.size() + 1, cntAccount)
                        .mapToObj(idx ->
                                new Account()
                                        .code(
                                                "40702810X" +
                                                        StringUtils.leftPad(
                                                                String.valueOf(idx),
                                                                11,
                                                                '0')
                                        )
                                        .saldo(BigDecimal.ZERO)
                                        .dateOpen(OffsetDateTime.now())
                                        .client(clients.get(idx % cntClient))
                        )
                        .toList()
        );


        return null;
    }
}
