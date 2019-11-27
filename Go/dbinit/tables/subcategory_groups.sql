drop table subcategory_groups;
create table subcategory_groups (
  id smallint not null primary key,
  category_id smallint not null references categories(id),
  name varchar(100) not null,

  constraint subcategory_groups_uk unique(category_id, name)
);
