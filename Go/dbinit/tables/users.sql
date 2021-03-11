drop table users;
create table users (
  id integer not null default NEXTVAL('user_id_seq') primary key,
  name varchar(100) not null unique,
  username varchar(50) not null unique,
  password varchar(50) not null,
  enabled bool not null default true
);
