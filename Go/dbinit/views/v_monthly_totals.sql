drop view v_monthly_totals;
create or replace view v_monthly_totals
as
select date_trunc('month', a.event_date) month_, a.category_id, a.category, a.subcategory_id, a.subcategory,
       a.account_id, a.account, a.valuta_code, sum(a.prihod) prihod, sum(a.rashod) rashod
  from v_prihod_rashod a
group by date_trunc('month', a.event_date), a.category_id, a.category, a.subcategory_id, a.subcategory,
         a.account_id, a.account, a.valuta_code
;
