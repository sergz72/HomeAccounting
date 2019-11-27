drop table valuta;
create table valuta (
  code char(3) not null primary key,
  name varchar(20) not null unique
);
