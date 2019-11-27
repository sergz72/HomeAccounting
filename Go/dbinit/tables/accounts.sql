drop table accounts;
create table accounts (
  id smallint not null default NEXTVAL('account_id_seq') primary key,
  valuta_code char(3) not null references valuta(code),
  name varchar(100) not null unique,
  is_cash bool not null,
  active_to date
);
