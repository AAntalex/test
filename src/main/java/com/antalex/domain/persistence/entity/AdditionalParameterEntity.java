package com.antalex.domain.persistence.entity;

import lombok.Data;
import javax.persistence.*;

@Table(name = "Z#VND_ADD_PARAMS")
@Data
@Entity
public class AdditionalParameterEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName = "test_seq_id", schema = "pmts_integr", allocationSize = 1000000)
    private Long id;
    @Column(name = "C_PARENT_ID")
    private String parentId;
    @Column(name = "C_CODE")
    private String code;
    @Column(name = "C_VALUE")
    private String value;
}
