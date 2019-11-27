DROP VIEW v_operation_property_lists;
CREATE VIEW v_operation_property_lists
AS
  SELECT CAST('get_accounts' as TEXT) list_name, id, name, id as position FROM accounts
  UNION ALL
  SELECT CAST('get_flats' as TEXT) list_name, id, address as name, id as position FROM flats
  UNION ALL
  SELECT CAST('get_people' as TEXT) list_name, id, name, row_number() over(order by name) as position FROM people
;
