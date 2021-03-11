drop table operation_properties;
create table operation_properties (
  code char(4) not null primary key,
  name varchar(50) not null unique,
  data_type varchar(100),
  need_recalc bool not null default false
);
