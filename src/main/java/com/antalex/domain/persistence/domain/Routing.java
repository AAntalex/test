package com.antalex.domain.persistence.domain;

import com.antalex.db.domain.abstraction.BaseDomain;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Routing extends BaseDomain {
    private String name;
    private LocalDateTime executeTime;
}
