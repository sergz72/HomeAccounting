drop view v_details;
create view v_details
as
select a.id, a.event_date, a.category_id, a.category, a.subcategory_id, a.subcategory, a.group_id, b.name group_name, a.account_id, a.account, a.amount, a.summa
  from v_finance_operations a left join subcategory_groups b on a.group_id = b.id
 where a.id not in (select distinct operation_id from suboperations)
union all
select a.id, a.event_date, a.category_id, a.category, a.subcategory_id, a.subcategory, a.group_id, b.name group_name, a.account_id, a.account, a.amount, a.summa
  from v_suboperations a left join subcategory_groups b on a.group_id = b.id
;
