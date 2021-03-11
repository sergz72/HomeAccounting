drop table fin_op_properties;
create table fin_op_properties (
  id integer not null default NEXTVAL('fin_op_properties_id_seq') primary key,
  property_code char(4) not null references operation_properties(code),
  numeric_value numeric,
  string_value varchar(500),
  date_value timestamp
);

create unique index fin_op_properties_ux on fin_op_properties(property_code, property_value_to_char(numeric_value, string_value, date_value));
