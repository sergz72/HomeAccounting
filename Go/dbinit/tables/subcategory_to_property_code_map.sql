drop table subcategory_to_property_code_map;
create table subcategory_to_property_code_map (
  subcategory_code char(4) not null,
  property_code char(4) not null references operation_properties(code),

  constraint subcategory_to_property_code_map_pk primary key(subcategory_code, property_code)
);
