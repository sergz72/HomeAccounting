drop view v_prihod_rashod_details;
create or replace view v_prihod_rashod_details
as
select a.id, a.event_date, a.category_id, a.category, a.subcategory_id, a.subcategory, a.group_id, a.group_name, a.account_id operation_account_id,
       a.account operation_acount, a.summa, b.id account_id, b.name account, b.valuta_code, amount,
       calculate_prihod(a.id, a.subcategory_id, a.account_id, a.summa, b.id) prihod,
       calculate_rashod(a.id, a.subcategory_id, a.account_id, a.summa, b.id) rashod
  from v_details a, accounts b
;
