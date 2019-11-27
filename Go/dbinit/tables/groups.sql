drop table groups;
create table groups (
  id smallint not null primary key,
  name varchar(50) not null unique
);
