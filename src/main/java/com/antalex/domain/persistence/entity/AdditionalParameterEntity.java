package com.antalex.domain.persistence.entity;

import lombok.Data;
import javax.persistence.*;

@Table(name = "Z#VND_ADD_PARAMS")
@Data
@Entity
public class AdditionalParameterEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_id")
    @SequenceGenerator(name = "seq_id", sequenceName = "SEQ_ID")
    private Long id;
    @Column(name = "C_PARENT_ID")
    private String parentId;
    @Column(name = "C_CODE")
    private String code;
    @Column(name = "C_VALUE")
    private String value;
}
