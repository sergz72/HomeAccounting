create or replace view v_finance_totals
as
select a.id, a.event_date, b.name account, a.summa_income, a.summa_expenditure, a.balance
  from finance_totals a, accounts b
 where a.account_id = b.id and (b.active_to is null or b.active_to >= a.event_date);
