CREATE OR REPLACE FUNCTION property_value_to_char(numeric_value numeric, string_value varchar, date_value timestamp)
  RETURNS text
AS
$BODY$
    select coalesce(cast(numeric_value as varchar), string_value, to_char(date_value, 'dd.mm.yyyy'));
$BODY$
LANGUAGE sql
IMMUTABLE;
