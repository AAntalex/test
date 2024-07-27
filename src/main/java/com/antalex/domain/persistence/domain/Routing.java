package com.antalex.domain.persistence.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Routing {
    private String name;
    private LocalDateTime executeTime;
}
