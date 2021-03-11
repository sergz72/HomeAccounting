drop table fin_op_properties_to_groups_map;
create table fin_op_properties_to_groups_map (
  group_id integer not null references fin_op_properties_groups(id),
  property_id integer not null references fin_op_properties(id),

  constraint fin_op_properties_to_groups_map_pk primary key(group_id, property_id)
);
