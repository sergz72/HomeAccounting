drop table flats;
create table flats (
  id smallint not null default NEXTVAL('flat_id_seq') primary key,
  address varchar(100) not null unique,
  is_default bool not null default false
);
