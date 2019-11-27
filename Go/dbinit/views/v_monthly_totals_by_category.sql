drop view v_monthly_totals_by_category;
create or replace view v_monthly_totals_by_category
as
select month_, category, valuta_code, sum(prihod) prihod, sum(rashod) rashod
  from v_monthly_totals
group by month_, category, valuta_code
having sum(prihod) != 0 or sum(rashod) != 0
;
