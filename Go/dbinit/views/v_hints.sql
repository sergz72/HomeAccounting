create or replace view v_hints as
select distinct b.property_code, b.string_value
  from operation_properties a, fin_op_properties b
 where a.code = b.property_code and a.data_type = 'string';
