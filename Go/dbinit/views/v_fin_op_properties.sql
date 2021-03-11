drop view v_fin_op_properties;
create view v_fin_op_properties
as
select a.id, a.property_code, b.name property,
       get_fin_op_value(b.data_type, a.numeric_value, a.string_value, a.date_value) op_value,
       c.group_id, d.event_date, d.id operation_id
  from fin_op_properties a
       inner join operation_properties b on a.property_code = b.code
       left join fin_op_properties_to_groups_map c on a.id = c.property_id
       left join finance_operations d on c.group_id = d.fin_op_properies_group_id
;
