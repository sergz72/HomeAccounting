package core

import (
	"core/entities"
	"database/sql"
	"errors"
	"expreval"
	"fmt"
	_ "github.com/lib/pq"
	"sort"
	"strconv"
	"strings"
	"time"
)

const PARSER_STACK_SIZE = 100

type DBDATA struct {
	Db *sql.DB
}

func (d *DBDATA) GetAccounts() ([]entities.Account, error) {
	rows, err := d.Db.Query("SELECT id, name, active_to, is_cash, valuta_code FROM accounts")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.Account, 0)
	for rows.Next() {
		account := entities.Account{}
		err = rows.Scan(&account.Id, &account.Name, &account.ActiveTo.Value, &account.IsCash, &account.ValutaCode)
		if err != nil {
			return nil, err
		}
		result = append(result, account)
	}

	return result, nil
}

func (d *DBDATA) GetCategories() ([]entities.Category, error) {
	rows, err := d.Db.Query("SELECT id, name FROM categories")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.Category, 0)
	for rows.Next() {
		category := entities.Category{}
		err = rows.Scan(&category.Id, &category.Name)
		if err != nil {
			return nil, err
		}
		result = append(result, category)
	}

	return result, nil
}

func (d *DBDATA) GetSubcategories() ([]entities.Subcategory, error) {
	rows, err := d.Db.Query("SELECT id, name, code, operation_code_id, category_id FROM subcategories")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.Subcategory, 0)
	for rows.Next() {
		subcategory := entities.Subcategory{}
		err = rows.Scan(&subcategory.Id, &subcategory.Name, &subcategory.Code.Value, &subcategory.OperationCodeId, &subcategory.CategoryId)
		if err != nil {
			return nil, err
		}
		result = append(result, subcategory)
	}

	return result, nil
}

func toDate(date int) time.Time {
	return time.Date(date / 10000, time.Month((date / 100) % 100), date % 100, 0, 0, 0, 0, time.UTC)
}

func (d *DBDATA) GetFinanceOperations(date int) ([]entities.FinanceOperation, error) {
	rows, err := d.Db.Query("SELECT id, cast(amount * 1000 as int), cast(summa * 100 as int), subcategory_id, fin_op_properies_group_id, account_id FROM finance_operations WHERE event_date = $1",
		                      toDate(date))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.FinanceOperation, 0)
	for rows.Next() {
		op := entities.FinanceOperation{}
		err = rows.Scan(&op.Id, &op.Amount.Value, &op.Summa, &op.SubcategoryId, &op.FinOpProperiesGroupId.Value, &op.AccountId)
		if err != nil {
			return nil, err
		}
		result = append(result, op)
	}

	return result, nil
}

func buildQuery(baseQuery string, addition string) string {
	return baseQuery +
		     "AND subcategory_id NOT IN (SELECT id FROM subcategories WHERE category_id = 9 AND operation_code_id = 'SPCL') " +
		     addition;
}

func (d *DBDATA) GetPrihodRashod(date1 int, date2 int, category int, subcategory int, account int) ([]entities.PrihodRashodByDay, error) {
	query := "SELECT event_date, category_id, subcategory_id, account_id, valuta_code, CAST(prihod * 100 AS INT), CAST(rashod * 100 AS INT) " +
		       "FROM v_prihod_rashod_details_by_day WHERE event_date BETWEEN $1 AND $2 "
	var rows *sql.Rows
	var err error
	if account > 0 {
		query += "AND account_id = " + strconv.Itoa(account) + " "
	}
	if subcategory > 0 {
		rows, err = d.Db.Query(query + "AND subcategory_id = $3", toDate(date1), toDate(date2), subcategory)
	} else if category > 0 {
		rows, err = d.Db.Query(buildQuery(query, "AND category_id = $3"), toDate(date1), toDate(date2), category)
	} else {
		rows, err = d.Db.Query(buildQuery(query, ""), toDate(date1), toDate(date2))
	}
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.PrihodRashodByDay, 0)
	for rows.Next() {
		op := entities.PrihodRashodByDay{}
		err = rows.Scan(&op.Date, &op.CategoryId, &op.SubcategoryId, &op.AccountId, &op.ValutaCode, &op.Prihod, &op.Rashod)
		if err != nil {
			return nil, err
		}
		result = append(result, op)
	}

	return result, nil
}

func (d *DBDATA) GetFinanceTotals(date int) ([]entities.FinanceTotal, error) {
	rows, err := d.Db.Query("SELECT account_id, cast(summa_expenditure * 100 as int), cast(summa_income * 100 as int), cast(balance * 100 as int)" +
	                        "FROM finance_totals WHERE event_date = (SELECT max(event_date) FROM finance_totals WHERE event_date <= $1)" +
		                      " AND account_id IN (SELECT id FROM accounts WHERE active_to IS NULL OR active_to >= $1)",
		                      toDate(date))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make([]entities.FinanceTotal, 0)
	for rows.Next() {
		op := entities.FinanceTotal{}
		err = rows.Scan(&op.AccountId, &op.SummaExpenditure, &op.SummaIncome, &op.Balance)
		if err != nil {
			return nil, err
		}
		result = append(result, op)
	}

	return result, nil
}

func (d *DBDATA) GetFinOpProperties(operations []entities.FinanceOperation) (map[string][]entities.FinOpProperty, error) {
	groupIds := make([]string, 0)
	for _, op := range operations {
		if op.FinOpProperiesGroupId.Value.Valid {
			groupIds = append(groupIds, strconv.Itoa(int(op.FinOpProperiesGroupId.Value.Int64)))
		}
	}
	if len(groupIds) == 0 {
		return nil, nil
	}
	inValues := strings.Join(groupIds, ",")
	rows, err := d.Db.Query(fmt.Sprintf("SELECT DISTINCT b.id, b.property_code, b.numeric_value, b.date_value, b.string_value " +
		                      "FROM fin_op_properties_to_groups_map a, fin_op_properties b WHERE a.property_id = b.id AND a.group_id in (%v)", inValues))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	props := make(map[int]entities.FinOpProperty, 0)
	for rows.Next() {
		p := entities.FinOpProperty{}
		err = rows.Scan(&p.Id, &p.PropertyCode, &p.NumericValue.Value, &p.DateValue.Value, &p.StringValue.Value)
		if err != nil {
			return nil, err
		}
		props[p.Id] = p
	}

	rows, err = d.Db.Query(fmt.Sprintf("SELECT group_id, property_id FROM fin_op_properties_to_groups_map WHERE group_id in (%v) ORDER BY 1, 2", inValues))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make(map[string][]entities.FinOpProperty, 0)
	for rows.Next() {
		p := entities.PropertyGroup{}
		err = rows.Scan(&p.GroupId, &p.PropertyId)
		if err != nil {
			return nil, err
		}
		prop, ok := props[p.PropertyId]
		if !ok {
			return nil, fmt.Errorf("Incorrect property id: %v", p.PropertyId)
		}
		key := strconv.Itoa(p.GroupId)
		properties, ok2 := result[key]
		if ok2 {
			properties = append(properties, prop)
			result[key] = properties
		} else {
			result[key] = []entities.FinOpProperty{prop}
		}
	}

	return result, nil
}

func (d *DBDATA) getFinOpPropertyGroups() (map[int][]int, error) {
	rows, err := d.Db.Query("SELECT group_id, property_id FROM fin_op_properties_to_groups_map")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	props := make(map[int][]int, 0)
	for rows.Next() {
		p := entities.PropertyGroup{}
		err = rows.Scan(&p.GroupId, &p.PropertyId)
		if err != nil {
			return nil, err
		}
		v, ok := props[p.GroupId]
		if ok {
			v = append(v, p.PropertyId)
			props[p.GroupId] = v
		} else {
			props[p.GroupId] = []int{p.PropertyId}
		}
	}
	return props, nil
}

func (d *DBDATA) getAllFinOpProperties() ([]entities.FinOpProperty, error) {
	rows, err := d.Db.Query("SELECT id, property_code, numeric_value, date_value, string_value " +
		                      "FROM fin_op_properties ORDER by id")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	props := make([]entities.FinOpProperty, 0)
	for rows.Next() {
		p := entities.FinOpProperty{}
		err = rows.Scan(&p.Id, &p.PropertyCode, &p.NumericValue.Value, &p.DateValue.Value, &p.StringValue.Value)
		if err != nil {
			return nil, err
		}
		props = append(props, p)
	}

	return props, nil
}

func (d *DBDATA) AddOperaton(data *entities.FinanceOperationAdd) error {
	if !data.Date.Value.Valid {
		return errors.New("NULL date")
	}
	if len(data.Summa) == 0 {
		return errors.New("Empty summa")
	}
	var amount sql.NullFloat64
	var err error
	if (len(data.Amount) > 0) {
		amount.Float64, err = expreval.Eval(data.Amount, PARSER_STACK_SIZE)
		if err != nil {
			return errors.New("Incorrect amount: " + err.Error())
		}
		amount.Valid = true
	}
	var summa float64
	summa, err = expreval.Eval(data.Summa, PARSER_STACK_SIZE)
	if err != nil {
		return errors.New("Incorrect summa: " + err.Error())
	}
	tx, errt := d.Db.Begin()
	if errt != nil {
		return errt
	}
	finOpProperiesGroupId, err := d.getFinOpPropertiesGroupId(tx, data.Properties)
	if err != nil {
		tx.Rollback()
		return err
	}
	_, err = tx.Exec("INSERT INTO finance_operations(event_date, amount, summa, subcategory_id, fin_op_properies_group_id, account_id) " +
		               "VALUES($1, $2, $3, $4, $5, $6)",
		                data.Date.Value, amount, summa, data.SubcategoryId, finOpProperiesGroupId, data.AccountId)
	if err == nil {
		return calculateTotals(tx, data.Date.Value.Time)
	}
	tx.Rollback()
	return err
}

func (d *DBDATA) getFinOpPropertiesGroupId(tx *sql.Tx, data []entities.FinOpProperty) (sql.NullInt64, error) {
	var finOpProperiesGroupId sql.NullInt64
	if data != nil && len(data) > 0 {
		value, err := d.addFinOpProperties(tx, data)
		if err != nil {
			return finOpProperiesGroupId, err
		}
		finOpProperiesGroupId.Valid = true
		finOpProperiesGroupId.Int64 = int64(value)
	}
  return finOpProperiesGroupId, nil
}

func (d *DBDATA) addFinOpProperties(tx *sql.Tx, properties []entities.FinOpProperty) (int, error) {
	propertyIds := make([]int, len(properties))

	allProperties, err := d.getAllFinOpProperties()
	if err != nil {
		return 0, err
	}
	finOpPropertiesModified := false
	for i, property := range properties {
		found := false
		for _, op := range allProperties {
			if op.PropertyCode == property.PropertyCode && op.NumericValue == property.NumericValue &&
				op.StringValue == property.StringValue && op.DateValue == property.DateValue {
				propertyIds[i] = op.Id
				found = true
				break
			}
		}
		if !found {
			var id int
			erre := tx.QueryRow("INSERT INTO fin_op_properties(property_code, numeric_value, date_value, string_value) VALUES($1, $2, $3, $4) RETURNING id",
				                  property.PropertyCode, property.NumericValue.Value, property.DateValue.Value, property.StringValue.Value).Scan(&id)
			if erre != nil {
				return 0, erre
			}
			propertyIds[i] = id
			finOpPropertiesModified = true
		}
	}
	sort.Ints(propertyIds)
	groupId := -1
	if !finOpPropertiesModified {
		groups, errg := d.getFinOpPropertyGroups()
		if errg != nil {
			return 0, errg
		}
		for k, v := range groups {
			if intArrayEquals(v, propertyIds) {
				groupId = k
				break
			}
		}
	}
	if groupId == -1 {
		err = d.Db.QueryRow("SELECT NEXTVAL('fin_op_properties_id_seq')").Scan(&groupId)
		if err != nil {
			return 0, err
		}
		_, err = tx.Exec("INSERT INTO fin_op_properties_groups(id) VALUES($1)", groupId);
		if err != nil {
			return 0, err
		}
		for i := 0; i < len(properties); i++ {
			_, erre := tx.Exec("INSERT INTO fin_op_properties_to_groups_map(group_id, property_id) VALUES($1, $2)",
				                      groupId, propertyIds[i])
			if erre != nil {
				return 0, erre
			}
		}
	}
	return groupId, nil
}

func intArrayEquals(a []int, b []int) bool {
	if len(a) != len(b) {
		return false
	}
	for i, v := range a {
		if v != b[i] {
			return false
		}
	}
	return true
}

func (d *DBDATA) ModifyOperaton(data *entities.FinanceOperationModify) error {
	if !data.Date.Value.Valid {
		return errors.New("NULL date")
	}
	if len(data.Summa) == 0 {
		return errors.New("Empty summa")
	}
	var amount sql.NullFloat64
	var err error
	if (len(data.Amount) > 0) {
		amount.Float64, err = expreval.Eval(data.Amount, PARSER_STACK_SIZE)
		if err != nil {
			return errors.New("Incorrect amount: " + err.Error())
		}
		amount.Valid = true
	}
	var summa float64
	summa, err = expreval.Eval(data.Summa, PARSER_STACK_SIZE)
	if err != nil {
		return errors.New("Incorrect summa: " + err.Error())
	}
	tx, errt := d.Db.Begin()
	if errt != nil {
		return errt
	}
	finOpProperiesGroupId, err := d.getFinOpPropertiesGroupId(tx, data.Properties)
	if err != nil {
		tx.Rollback()
		return err
	}
	result, erre := tx.Exec("UPDATE finance_operations SET amount=$1, summa=$2, subcategory_id=$3, fin_op_properies_group_id=$4, account_id=$5 WHERE id=$6",
		amount, summa, data.SubcategoryId, finOpProperiesGroupId, data.AccountId, data.Id)
	if erre == nil {
		numRows, erra := result.RowsAffected()
		if erra != nil {
			tx.Rollback()
			return erra
		}
		if numRows == 0 {
			tx.Rollback()
			return fmt.Errorf("Operation id not found: %v", data.Id)
		}
		return calculateTotals(tx, data.Date.Value.Time)
	}
	tx.Rollback()
	return erre
}

func (d *DBDATA) DeleteOperaton(data *entities.FinanceOperationDelete) error {
	if !data.Date.Value.Valid {
		return errors.New("NULL date")
	}
	tx, errt := d.Db.Begin()
	if errt != nil {
		return errt
	}
	result, err := tx.Exec("DELETE FROM finance_operations WHERE id = $1", data.Id)
	if err == nil {
		numRows, erra := result.RowsAffected()
		if erra != nil {
			tx.Rollback()
			return erra
		}
		if numRows == 0 {
			tx.Rollback()
			return fmt.Errorf("Operation id not found: %v", data.Id)
		}
		return calculateTotals(tx, data.Date.Value.Time)
	}
	tx.Rollback()
	return err
}

func calculateTotals(tx *sql.Tx, date time.Time) error {
	_, err := tx.Query("SELECT calculate_totals($1)", date.Format("02.01.2006"))
	if err == nil {
		tx.Commit()
	} else {
		tx.Rollback()
	}
	return err
}

func (d *DBDATA) GetSubcategoryToPropertyCodeMap() (map[string][]string, error) {
	rows, err := d.Db.Query("SELECT subcategory_code, property_code FROM subcategory_to_property_code_map")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make(map[string][]string, 0)
	for rows.Next() {
		m := entities.SubcategoryToPropertyCodeMap{}
		err = rows.Scan(&m.SubcategoryCode, &m.PropertyCode)
		if err != nil {
			return nil, err
		}
		v, ok := result[m.SubcategoryCode]
		if ok {
			v = append(v, m.PropertyCode)
			result[m.SubcategoryCode] = v
		} else {
			result[m.SubcategoryCode] = []string{m.PropertyCode}
		}
	}
	return result, nil
}

func (d *DBDATA) GetHints() (map[string][]string, error) {
	rows, err := d.Db.Query("SELECT property_code, string_value FROM v_hints")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make(map[string][]string, 0)
	for rows.Next() {
		m := entities.SubcategoryToPropertyCodeMap{}
		err = rows.Scan(&m.SubcategoryCode, &m.PropertyCode)
		if err != nil {
			return nil, err
		}
		v, ok := result[m.SubcategoryCode]
		if ok {
			v = append(v, m.PropertyCode)
			result[m.SubcategoryCode] = v
		} else {
			result[m.SubcategoryCode] = []string{m.PropertyCode}
		}
	}
	return result, nil
}
