drop table finance_operations;
create table finance_operations (
  id integer not null default NEXTVAL('finance_operation_id_seq') primary key,
  event_date date not null,
  subcategory_id integer not null references subcategories(id),
  account_id smallint not null references accounts(id),
  amount numeric(15,3),
  summa numeric(15,2) not null,
  fin_op_properies_group_id integer references fin_op_properties_groups(id)
);

create index finance_ops_event_date_idx on finance_operations(event_date);

create unique index finance_operations_ux on finance_operations(event_date, subcategory_id, account_id,
                                                                coalesce(fin_op_properies_group_id, 0));
