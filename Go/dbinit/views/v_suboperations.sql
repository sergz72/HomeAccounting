drop view v_suboperations;
create view v_suboperations
as
select a.id, a.operation_id, c.category_id, b.name category, a.subcategory_id, c.name subcategory, c.group_id, a.amount, a.summa,
       d.event_date, d.account_id, d.account
  from suboperations a, categories b, subcategories c, v_finance_operations d
 where a.subcategory_id = c.id and c.category_id = b.id and a.operation_id = d.id
;
