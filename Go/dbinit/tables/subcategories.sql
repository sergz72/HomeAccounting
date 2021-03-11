drop table subcategories;
create table subcategories (
  id integer not null default NEXTVAL('subcategory_id_seq') primary key,
  code char(4),
  category_id smallint not null references categories(id),
  name varchar(100) not null,
  operation_code_id char(4) not null references operation_codes(code),
  group_id smallint references subcategoty_groups(id),

  constraint subcategories_uk unique(category_id, name)
);
