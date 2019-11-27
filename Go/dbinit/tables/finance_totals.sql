drop table finance_totals;
create table finance_totals (
  id integer not null default NEXTVAL('finance_totals_id_seq') primary key,
  event_date date not null,
  account_id smallint not null references accounts(id),
  summa_income numeric(15,2) not null,
  summa_expenditure numeric(15,2) not null,
  balance numeric(15,2) not null,

  constraint finance_totals_uk unique(event_date, account_id)
);

create index finance_tot_event_date_idx on finance_totals(event_date);
