drop table suboperations;
create table suboperations (
  id integer not null default NEXTVAL('finance_operation_id_seq') primary key,
  operation_id integer not null references finance_operations(id),
  subcategory_id integer not null references subcategories(id),
  amount numeric(15,3),
  summa numeric(15,2) not null,

  constraint suboperations_uk unique(operation_id, subcategory_id)
);

create index suboperations_operation_id_idx on suboperations(operation_id);
