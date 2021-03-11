CREATE OR REPLACE FUNCTION calculate_prihod (
  operation_id_in integer,
  subcategory_id_in integer,
  account_id1 smallint,
  summa_in numeric,
  account_id2 smallint
)
RETURNS numeric
AS
$$
DECLARE
  subcategories_code char(4);
  subcategories_opcode char(4);
  cash_account_id smallint;
BEGIN
  select code, operation_code_id into subcategories_code, subcategories_opcode
    from subcategories where id = subcategory_id_in;
  case subcategories_opcode
    when 'INCM' then if account_id1 = account_id2 then return summa_in; else return 0; end if;
    when 'EXPN' then return 0;
    when 'SPCL' then
      case subcategories_code
        -- Пополнение карточного счета наличными
        when 'INCC'
        then
          if account_id1 = account_id2
          then
            return summa_in;
          else
            return 0;
          end if;
        -- Снятие наличных в банкомате
        when 'EXPC'
        then
          -- Ищем наличный счет с той же валютой что и карточный.
          select id into cash_account_id from accounts where is_cash = true and valuta_code = (
            select valuta_code from accounts where id = account_id1
          );
          if account_id2 = cash_account_id
          then
            return summa_in;
          else
            return 0;
          end if;
        -- Обмен валюты
        when 'EXCH', 'TRFR'
        then
          select max(b.numeric_value) into cash_account_id
            from finance_operations a,
                 fin_op_properties b,
                 fin_op_properties_to_groups_map c
           where a.id = operation_id_in
             and b.property_code = 'SECA'
             and c.group_id = a.fin_op_properies_group_id
             and b.id = c.property_id;
          if account_id2 = cash_account_id
          then
            return summa_in;
          else
            return 0;
          end if;
      end case;
  end case;
  RETURN 0;
END;
$$
LANGUAGE plpgsql;
