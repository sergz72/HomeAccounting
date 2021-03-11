CREATE OR REPLACE FUNCTION date_to_int(d date)
  RETURNS int
AS
$BODY$
    select cast(date_part('year', d) * 10000 + date_part('month', d) * 100 + date_part('day', d) as int)
$BODY$
LANGUAGE sql
IMMUTABLE;
