drop view v_finance_operations;
create view v_finance_operations
as
select a.id, a.event_date, c.category_id, b.name category, a.subcategory_id, c.name subcategory, c.group_id,
       a.account_id, d.name account, a.amount, a.summa
  from finance_operations a, categories b, subcategories c, accounts d
 where a.subcategory_id = c.id and c.category_id = b.id and a.account_id = d.id
;
