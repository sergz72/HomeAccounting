drop table categories;
create table categories (
  id smallint not null primary key,
  name varchar(100) not null unique
);
