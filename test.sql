select drc.C_ELDOCNUM
     , att.C_PRIORITY
	 , md.C_NUM_DOC
	 , acc_dt.C_MAIN_V_ID
	 , acc_kt.C_MAN_V_ID
	 , cl_t_dt.C_NAME
	 , cl_t_ct.C_NAME
from Z#DOCUM_RC drc
	join Z#MAIN_DOCUM md on drc.C_MAIN_DOC = md.ID
	join Z#AC_FIN acc_dt on md.C_ACC_DT = acc_dt.ID
	join Z#AC_FIN acc_dt on md.C_ACC_CT = acc_ct.ID
	join Z#CLIENT cl_dt on acc_dt.C_CLIENT = cl_dt.ID
	join Z#CLIENT cl_ct on acc_ct.C_CLIENT = cl_ct.ID
	join Z#PRIORITY att on att.C_DOC_RC = drc.ID
	left join Z#CATEGORY_CLIENT cl_t_dt on cl_dt.C_CATEGORY = cl_t_dt.ID
	left join Z#CATEGORY_CLIENT cl_t_ct on cl_ct.C_CATEGORY = cl_t_ct.ID
where 
		(cl_t_dt.C_CODE = 'VIP' or cl_t_ct.C_CODE = 'VIP')
	and drc.C_CR_DATE >= :start_date and drc.C_CR_DATE >= :end_date
	and	drc.C_ELDOCNUM like :eldoc_num
	and md.C_DATE_PROV >= :start_date and md.C_DATE_PROV >= :end_date


1. Z#PRIORITY(C_DOC_RC) [1] -> Z#DOCUM_RC(C_MAIN_DOC) [1] -> Z#MAIN_DOCUM(C_ACC_DT) [1,2] -> Z#AC_FIN(C_CLIENT) [1] -> Z#CLIENT(C_CATEGORY) [1] -> Z#CATEGORY_CLIENT [ALL]
	a) Z#PRIORITY(C_DOC_RC) [1] -> Z#DOCUM_RC(C_MAIN_DOC) [1] -> Z#MAIN_DOCUM(C_ACC_DT) [1];
	b) Z#MAIN_DOCUM(C_ACC_DT) [1,2] -> Z#AC_FIN(C_CLIENT) [1] -> Z#CLIENT(C_CATEGORY) [1] -> Z#CATEGORY_CLIENT [ALL]
2. Z#PRIORITY(C_DOC_RC) [1] -> Z#DOCUM_RC(C_MAIN_DOC) [1] -> Z#MAIN_DOCUM(C_ACC_CT) [1,2] -> Z#AC_FIN(C_CLIENT) [2] -> Z#CLIENT(C_CATEGORY) [2] -> Z#CATEGORY_CLIENT [ALL]


	a) Z#PRIORITY(C_DOC_RC) -> Z#DOCUM_RC(C_MAIN_DOC) -> Z#MAIN_DOCUM(C_ACC_DT, C_ACC_CT);
	b) Z#MAIN_DOCUM(C_ACC_DT) -> Z#AC_FIN(C_CLIENT) -> Z#CLIENT(C_CATEGORY) -> Z#CATEGORY_CLIENT
	c) Z#MAIN_DOCUM(C_ACC_CT) -> Z#AC_FIN(C_CLIENT) -> Z#CLIENT(C_CATEGORY) -> Z#CATEGORY_CLIENT


a)
	 L1
select drc.C_ELDOCNUM, pr.C_PRIORITY, md.C_NUM_DOC
    ,  md.C_ACC_DT, md.C_ACC_CT                  -- KEYS
from Z#DOCUM_RC drc
         join Z#MAIN_DOCUM md on drc.C_MAIN_DOC = md.ID
         join Z#PRIORITY att on att.C_DOC_RC = drc.ID
where
      drc.C_CR_DATE >= :start_date and drc.C_CR_DATE >= :end_date
  and drc.C_ELDOCNUM like :eldoc_num
  and md.C_DATE_PROV >= :start_date and md.C_DATE_PROV >= :end_date

->

    L2
select acc_dt.ID  -- KEYS
     , acc_dt.C_MAIN_V_ID, cl_t_dt.C_NAME
from Z#AC_FIN acc_dt
    join Z#CLIENT cl_dt on acc_dt.C_CLIENT = cl_dt.ID
    left join Z#CATEGORY_CLIENT cl_t_dt on cl_dt.C_CATEGORY = cl_t_dt.ID
where
      cl_t_dt.C_CODE = 'VIP'
  and acc_dt.ID in (<IDS>)

||
    L2
select acc_ct.ID  -- KEYS
     , acc_ct.C_MAIN_V_ID, cl_t_ct.C_NAME
from Z#AC_FIN acc_ct
    join Z#CLIENT cl_dt on acc_ct.C_CLIENT = cl_ct.ID
    left join Z#CATEGORY_CLIENT cl_t_ct on cl_ct.C_CATEGORY = cl_t_ct.ID
where
    cl_t_ct.C_CODE = 'VIP'
  and acc_ct.ID in (<IDS>)


b)
select drc.C_ELDOCNUM
     , att.C_PRIORITY
     , md.C_NUM_DOC
     , acc_dt.C_MAIN_V_ID
     , acc_kt.C_MAN_V_ID
     , cl_t_dt.C_NAME
     , cl_t_ct.C_NAME
from Z#DOCUM_RC drc
         join Z#MAIN_DOCUM md on drc.C_MAIN_DOC = md.ID
         join Z#AC_FIN acc_dt on md.C_ACC_DT = acc_dt.ID
         join Z#AC_FIN acc_dt on md.C_ACC_CT = acc_ct.ID
         join Z#CLIENT cl_dt on acc_dt.C_CLIENT = cl_dt.ID
         join Z#CLIENT cl_ct on acc_ct.C_CLIENT = cl_ct.ID
         join Z#PRIORITY att on att.C_DOC_RC = drc.ID
         left join Z#CATEGORY_CLIENT cl_t_dt on cl_dt.C_CATEGORY = cl_t_dt.ID
         left join Z#CATEGORY_CLIENT cl_t_ct on cl_ct.C_CATEGORY = cl_t_ct.ID
where
    (cl_t_dt.C_CODE = 'VIP' or cl_t_ct.C_CODE = 'VIP')
  and drc.C_CR_DATE >= :start_date and drc.C_CR_DATE >= :end_date
  and	drc.C_ELDOCNUM like :eldoc_num
  and md.C_DATE_PROV >= :start_date and md.C_DATE_PROV >= :end_date