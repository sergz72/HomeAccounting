drop view v_prihod_rashod_details_by_day;
create or replace view v_prihod_rashod_details_by_day
as
select a.event_date, a.category_id, a.category, a.subcategory_id, a.subcategory, a.group_id, a.group_name,
       a.account_id, a.account, a.valuta_code, sum(a.amount) amount, sum(a.prihod) prihod, sum(a.rashod) rashod
  from v_prihod_rashod_details a
group by a.event_date, a.category_id, a.category, a.subcategory_id, a.subcategory, a.group_id, a.group_name,
         a.account_id, a.account, a.valuta_code
having sum(a.prihod) != 0 or sum(a.rashod) != 0
;
