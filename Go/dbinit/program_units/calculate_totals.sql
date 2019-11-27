CREATE OR REPLACE FUNCTION calculate_totals(data character varying)
  RETURNS integer AS
$BODY$
DECLARE
  event_date_in date;
  max_date date;
BEGIN
  event_date_in := to_date(data, 'dd.mm.yyyy');
  select max(event_date) into max_date from finance_operations;
  delete from finance_totals where event_date >= event_date_in;
  loop
    insert into finance_totals(event_date, account_id, summa_income, summa_expenditure, balance)
    select a.event_date, a.account_id, a.prihod, a.rashod, coalesce(b.balance, 0) + a.prihod - a.rashod balance
      from (select event_date, account_id, sum(prihod) prihod, sum(rashod) rashod from v_prihod_rashod
             where event_date = event_date_in group by account_id, event_date) a
           left outer join (
             select account_id, balance from finance_totals where event_date = (
               select max(event_date) from finance_totals where event_date < event_date_in)) b
           on a.account_id = b.account_id;

    if max_date <= event_date_in
    then
      exit;
    end if;
    event_date_in := event_date_in + 1;
  end loop;
  RETURN 0;
END;
$BODY$
  LANGUAGE plpgsql;
