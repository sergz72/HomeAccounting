CREATE OR REPLACE FUNCTION get_fin_op_value (
  data_type_in varchar,
  numeric_value_in numeric,
  string_value_in varchar,
  date_value_in timestamp
)
RETURNS varchar
AS
$$
DECLARE
  parameters varchar[];
  result varchar;
BEGIN
  parameters = regexp_split_to_array(data_type_in, ':');
  case parameters[1]
    when 'dropdown' then
      select name into result from v_operation_property_lists where list_name = parameters[2] and id = numeric_value_in;
      return result;
    when 'numeric' then return cast(numeric_value_in as varchar);
    when 'string' then return string_value_in;
    when 'date' then return to_char(date_value_in, 'dd.mm.yyyy');
    when 'datetime' then return to_char(date_value_in, 'dd.mm.yyyy hh.mi.ss');
    when 'time' then return to_char(date_value_in, 'hh.mi.ss');
    else return null;
  end case;
  RETURN null;
END;
$$
LANGUAGE plpgsql;
