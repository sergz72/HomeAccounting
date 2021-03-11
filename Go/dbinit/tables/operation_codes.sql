drop table operation_codes;
create table operation_codes (
  code char(4) not null primary key,
  name varchar(50) not null unique,
  sign smallint not null
);
