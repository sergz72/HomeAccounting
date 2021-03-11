drop table people;
create table people (
  id smallint not null default NEXTVAL('people_id_seq') primary key,
  name varchar(50) not null unique
);
