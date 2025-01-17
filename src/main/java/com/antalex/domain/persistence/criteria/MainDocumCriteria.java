package com.antalex.domain.persistence.criteria;

import com.antalex.db.annotation.Criteria;
import com.antalex.db.annotation.CriteriaAttribute;
import com.antalex.db.annotation.Join;
import com.antalex.domain.persistence.entity.shard.app.Account;
import com.antalex.domain.persistence.entity.shard.app.Client;
import com.antalex.domain.persistence.entity.shard.app.MainDocum;
import com.antalex.domain.persistence.entity.shard.app.ClientCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data

@Criteria(
        from = MainDocum.class,
        alias = "md",
        joins = {
                @Join(from = Account.class, alias = "acc_dt", on = "${acc_dt} = ${md.accDt}"),
                @Join(from = Account.class, alias = "acc_ct", on = "${id} = ${md.accCt}"),
                @Join(from = Client.class, alias = "cl_ct", on = "${id} = ${acc_ct.client}"),
                @Join(from = Client.class, alias = "cl_dt", on = "${id} = ${acc_dt.client}"),
                @Join(from = ClientCategory.class, alias = "cl_cat", on = "${id} = ${cl_ct.category}"),
        },
        where = "{md.dateProv} >= ? and ${cl_cat.code} = 'VIP' and {acc_dt.code} like '40702810%3'"
)
public class MainDocumCriteria {
    @CriteriaAttribute("${md.num}")
    private Integer num;
    @CriteriaAttribute("${md.sum}")
    private BigDecimal sum;
    @CriteriaAttribute("${md.date}")
    private Date date;
    @CriteriaAttribute("acc_dt.code")
    private String dtNum;
    @CriteriaAttribute("acc_ct.code")
    private String ctNum;
    @CriteriaAttribute("cl_dt.name")
    private String dtClientName;
    @CriteriaAttribute("cl_ct.name")
    private String ctClientName;
}
