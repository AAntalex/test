package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Table;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Table(name = "TEST_OTHER")
@Accessors(chain = true)
@Data
@ShardEntity
public class TestOtherShardEntity extends BaseShardEntity {
    private Date executeTime;
    private Byte aByte;
    private Boolean aBoolean;
    private Short aShort;
    private Integer integer;
    private Float aFloat;
    private Double aDouble;
    private BigDecimal bigDecimal;
    private Time time;
    private Timestamp timestamp;
    private Blob blob;
    private Clob clob;
    private URL url;
    private RowId rowId;
    private SQLXML sqlxml;
    private LocalDateTime localDateTime;
    private TestStatus status;
    private UUID uid;
}
