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


SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where md.C_DATE_PROV >= '2025-01-14 00:00'::timestamp
  and cl_cat.C_CODE = 'VIP'
  and acc_dt.C_CODE like '40702810%9'

explain analyze
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where cl_cat.C_CODE = 'VIP'
   or acc_dt.C_CODE = '40702810X00000000002'

(p1 or p2 and p3), p2 = TRUE  (true or false and true) = (false or true and false) == false = false == true
((p1 or p2) and p3), p2 = FALSE ((true or false) and true) = ((false or true) and false) == true = false == false


SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where (acc_dt.C_CODE like '40702810%' or acc_dt.C_CODE like '40701810%') and acc_ct.C_CODE = '30102%'

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_dt.C_CODE like '40702810%' or acc_ct.C_CODE = '30102%'

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_dt.C_CODE like '40702810%' and acc_ct.C_CODE = '30102%' or acc_ct.C_CODE = '30102%' and acc_dt.C_CODE like '40701810%'

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where (acc_dt.C_CODE like '40702810%' or cl_cat.C_CODE = 'VIP') and acc_ct.C_CODE = '30102%'







===========================================================
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where (acc_dt.C_CODE like '40702810%' or acc_dt.C_CODE like '40701810%') and acc_ct.C_CODE = '30102%'

--------------------------
+++++++++++++++++++++++++++

/*V1*/ SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt, md.C_ACC_CT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where (acc_dt.C_CODE like '40702810%' or acc_dt.C_CODE like '40701810%')
    ^
SELECT acc_ct.ID /*key*/, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct
FROM T_ACCOUNT acc_ct
         JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
         LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%' and acc_ct.ID in (:IDS /*key*/)
    ||
/*V2*/ SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct, md.C_ACC_DT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%'
    ^
SELECT acc_dt.ID /*key*/, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt
FROM T_ACCOUNT acc_dt
         JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where (acc_dt.C_CODE like '40702810%' or acc_dt.C_CODE like '40701810%') and acc_dt.ID in (:IDS /*key*/)




===========================================================
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_dt.C_CODE like '40702810%' and acc_ct.C_CODE = '30102%' or '30102%' = acc_ct.C_CODE and acc_dt.C_CODE like '40701810%'

--------------------------

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_dt.C_CODE like '40702810%' and acc_ct.C_CODE = '30102%'

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where '30102%' = acc_ct.C_CODE and acc_dt.C_CODE like '40701810%'

+++++++++++++++++++++++++++

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt,     md.ID, md.C_ACC_DT, md.C_ACC_CT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE like '40702810%'
^
SELECT acc_ct.ID /*key*/, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct
FROM T_ACCOUNT acc_ct
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%' and acc_ct.ID in (:IDS /*key*/)
||
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct,     md.ID, md.C_ACC_CT, md.C_ACC_DT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%'
^
SELECT acc_dt.ID /*key*/, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt
FROM T_ACCOUNT acc_dt
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE like '40702810%' and acc_dt.ID in (:IDS /*key*/)

UNION

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt, md.C_ACC_CT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE like '40701810%'
    ^
SELECT acc_ct.ID /*key*/, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
         JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
         JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
         LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%' and acc_ct.ID in (:IDS /*key*/)
||
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct, md.C_ACC_DT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.C_CODE = '30102%'
    ^
SELECT acc_dt.ID /*key*/, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt
FROM T_MAIN_DOCUM md
         JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
         JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE like '40701810%' and acc_dt.ID in (:IDS /*key*/)

===========================================================
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where cl_cat.C_CODE = 'VIP'
   or acc_dt.C_CODE = '40702810X00000000002'

--------------------------

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where cl_cat.C_CODE = 'VIP'

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, acc_ct.C_CODE acc_ct, cl_dt.C_NAME client_dt, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_dt.C_CODE = '40702810X00000000002'

+++++++++++++++++++++++++++

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct, md.C_ACC_DT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where cl_cat.C_CODE = 'VIP'
^
SELECT acc_dt.ID /*key*/, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt
FROM T_ACCOUNT acc_dt
         JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.ID in (:IDS /*key*/)
||
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt, md.C_ACC_CT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where 1=1
^
SELECT acc_ct.ID /*key*/, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct
FROM T_ACCOUNT acc_ct
         JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
         LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where cl_cat.C_CODE = 'VIP' and acc_ct.ID in (:IDS /*key*/)

UNION

SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt, md.C_ACC_CT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
    JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE = '40702810X00000000002'
^
SELECT acc_ct.ID /*key*/, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct
FROM T_MAIN_DOCUM md
         JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
         JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
         LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where acc_ct.ID in (:IDS /*key*/)
||
SELECT md.C_NUM num, md.C_SUM sum, md.C_DATE date, acc_ct.C_CODE acc_ct, cl_ct.C_NAME client_ct, md.C_ACC_DT /*key*/
FROM T_MAIN_DOCUM md
    JOIN T_ACCOUNT acc_ct on acc_ct.ID = md.C_ACC_CT
    JOIN T_CLIENT cl_ct on cl_ct.ID = acc_ct.C_CLIENT
    LEFT JOIN T_CLIENT_CATEGORY cl_cat on cl_cat.ID = cl_ct.C_CATEGORY
where 1=1
^
SELECT acc_dt.ID /*key*/, acc_dt.C_CODE acc_dt, cl_dt.C_NAME client_dt
FROM T_MAIN_DOCUM md
         JOIN T_ACCOUNT acc_dt on acc_dt.ID = md.C_ACC_DT
         JOIN T_CLIENT cl_dt on cl_dt.ID = acc_dt.C_CLIENT
where acc_dt.C_CODE = '40702810X00000000002' and acc_dt.ID in (:IDS /*key*/)



