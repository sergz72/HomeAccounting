package core

import (
	"HomeAccountingDB/src/core/entities"
	"HomeAccountingDB/src/expreval"
	"errors"
	"fmt"
	"math"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"
)

const parserStackSize = 100

type kv struct {
	key   int
	value []entities.FinanceOperation
}

type DB struct {
	path                string
	AccountsArray       []entities.Account
	Accounts            map[int]entities.Account
	lastAccountOperationDateMap map[int]int
	CategoriesArray     []entities.Category
	Categories          map[int]entities.Category
	OperationCodes      map[string]entities.OperationCode
	OperationProperties map[string]entities.OperationProperty
	SubcategoriesArray  []entities.Subcategory
	Subcategories       map[int]entities.Subcategory
	SubcategoryToPropertyCodeMap map[string][]string
	FinanceOperations   map[int][]entities.FinanceOperation
	FinanceTotals       map[int][]entities.FinanceTotal
	CashAccounts        map[string]entities.Account
	NextOperationId     int
	NextSubcategoryId   int
	NextFinOpPropertyId int
	NextPropertyGroupId int
	MinDate int
}

func (d *DB) Init(path string) {
	d.Accounts = make(map[int]entities.Account)
	d.Categories = make(map[int]entities.Category)
	d.OperationCodes = make(map[string]entities.OperationCode)
	d.OperationProperties = make(map[string]entities.OperationProperty)
	d.Subcategories = make(map[int]entities.Subcategory)
	d.FinanceTotals = make(map[int][]entities.FinanceTotal)
	d.FinanceOperations = make(map[int][]entities.FinanceOperation)
	d.CashAccounts = make(map[string]entities.Account)
	d.SubcategoryToPropertyCodeMap = make(map[string][]string)
	d.path = path
	d.NextOperationId = 1
	d.NextSubcategoryId = 1
	d.MinDate = 99999999
	d.lastAccountOperationDateMap = make(map[int]int)
}

func (d *DB) Load(path string, calculateTotals bool) error {
	d.Init(path)
	fmt.Println("accounts")
	var err error
	d.AccountsArray, err = entities.ReadAccountsFromJson(path + "/accounts.json")
	if err != nil {
		return err
	}
	for _, account := range d.AccountsArray {
		d.Accounts[account.Id] = account
	}

	fmt.Println("categories")
	d.CategoriesArray, err = entities.ReadCategoriesFromJson(path + "/categories.json")
	if err != nil {
		return err
	}
	for _, category := range d.CategoriesArray {
		d.Categories[category.Id] = category
	}

	fmt.Println("operationCodes")
	operationCodesArray, err := entities.ReadOperationCodesFromJson(path + "/operationCodes.json")
	if err != nil {
		return err
	}
	for _, code := range operationCodesArray {
		d.OperationCodes[code.Code] = code
	}

	fmt.Println("operationProperties")
	operationPropertiesArray, err := entities.ReadOperationPropertiesFromJson(path + "/operationProperties.json")
	if err != nil {
		return err
	}
	for _, property := range operationPropertiesArray {
		d.OperationProperties[property.Code] = property
	}

	fmt.Println("subcategories")
	d.SubcategoriesArray, err = entities.ReadSubcategoriesFromJson(path + "/subcategories.json")
	if err != nil {
		return err
	}
	for _, subcategory := range d.SubcategoriesArray {
		d.Subcategories[subcategory.Id] = subcategory
		if subcategory.Id >= d.NextSubcategoryId {
			d.NextSubcategoryId = subcategory.Id + 1
		}
	}

	fmt.Println("subcategory_to_property_code_map")
	d.SubcategoryToPropertyCodeMap, err = entities.ReadSubcategoryToPropertyCodeMapFromJson(path + "/subcategory_to_property_code_map.json")
	if err != nil {
		return err
	}

	fmt.Println("financeOperations")
	if !calculateTotals {
		fmt.Println("financeTotals")
	}
	datesPath := path + "/dates"
	dates, err := os.Open(datesPath)
	if err != nil {
		return err
	}
	defer dates.Close()
	fi, err := dates.Readdir(-1)
	if err != nil {
		return err
	}
	for _, fi := range fi {
		if fi.Mode().IsDir() {
			datePath := datesPath + "/" + fi.Name()
			i, err := strconv.Atoi(fi.Name())
			if err != nil {
				return err
			}
			if d.MinDate > i {
				d.MinDate = i
			}
			operations, err := entities.ReadFinanceOperationsFromJson(datePath + "/financeOperations.json")
			if err != nil {
				return err
			}
			for _, op := range operations {
				if op.Id >= d.NextOperationId {
					d.NextOperationId = op.Id + 1
				}
			}
			for _, op := range operations {
				d.updateLastAccountOperationDateMap(op.AccountId, i)
			}
			d.FinanceOperations[i] = operations
			if !calculateTotals {
				totals, err := entities.ReadFinanceTotalsFromJson(datePath + "/financeTotals.json")
				if err != nil {
					return err
				}
				d.FinanceTotals[i] = totals
			}
		}
	}

	for _, account := range d.Accounts {
		if account.IsCash {
			d.CashAccounts[account.ValutaCode] = account
		}
	}

	if calculateTotals {
		fmt.Println("Calculating finance totals...")
		err = d.BuildTotals(d.FinanceTotals, 0)
		if err != nil {
			return err
		}
	}

	return nil
}

func (d *DB) handle(o *entities.FinanceOperation, current map[int]*entities.FinanceTotal) error {
	subcategory := d.Subcategories[o.SubcategoryId]
	op, ok := current[o.AccountId]
	if !ok {
		return fmt.Errorf("Invalid account id: %v", o.AccountId)
	}
	switch subcategory.OperationCodeId {
	case "INCM":
		op.Income(o.Summa)
	case "EXPN":
		op.Expenditure(o.Summa)
	case "SPCL":
		switch subcategory.Code {
		case "INCC": // Пополнение карточного счета наличными
			return d.handleINCC(o, op, current)
		case "EXPC": // Снятие наличных в банкомате
			return d.handleEXPC(o, op, current)
		case "EXCH": // Обмен валюты
			return d.handleEXCH(o, op, current)
		case "TRFR": // Перевод средств между платежными картами
			return d.handleTRFR(o, op, current)
		default:
			return fmt.Errorf("Unknown subcategory code: %v", subcategory.Code)
		}
	default:
		return fmt.Errorf("Unknown operation code: %v", subcategory.OperationCodeId)
	}

	return nil
}

// Пополнение карточного счета наличными
func (d *DB) handleINCC(o *entities.FinanceOperation, op *entities.FinanceTotal, current map[int]*entities.FinanceTotal) error {
	op.Income(o.Summa)
	// cash account for corresponding valuta code
	accountId, err := d.getCashAccountId(o.AccountId)
	if err != nil {
		return err
	}
	op2, ok := current[accountId]
	if !ok {
		return fmt.Errorf("Incorrect account id %v", accountId)
	}
	op2.Expenditure(o.Summa)
	return nil
}

// Снятие наличных в банкомате
func (d *DB) handleEXPC(o *entities.FinanceOperation, op *entities.FinanceTotal, current map[int]*entities.FinanceTotal) error {
	op.Expenditure(o.Summa)
	// cash account for corresponding valuta code
	accountId, err := d.getCashAccountId(o.AccountId)
	if err != nil {
		return err
	}
	op2, ok := current[accountId]
	if !ok {
		return fmt.Errorf("Incorrect account id %v", accountId)
	}
	op2.Income(o.Summa)
	return nil
}

func (d *DB) getCashAccountId(accountId int,) (int, error) {
	account, ok := d.Accounts[accountId]
	if !ok {
    return 0, fmt.Errorf("Invalid account id %v", accountId)
  }
  cashAccount, ok2 := d.CashAccounts[account.ValutaCode]
  if !ok2 {
    return 0, fmt.Errorf("Cash account not found for valuta code %v", account.ValutaCode)
  }
	return cashAccount.Id, nil
}

// Обмен валюты
func (d *DB) handleEXCH(o *entities.FinanceOperation, op *entities.FinanceTotal, current map[int]*entities.FinanceTotal) error {
	op.Expenditure(entities.Decimal(o.Amount.Value / 10))
	if o.FinOpProperies != nil {
		for _, property := range o.FinOpProperies {
			if property.PropertyCode == "SECA" {
				total, ok2 := current[int(property.NumericValue)]
				if !ok2 {
					return fmt.Errorf("SECA property: Incorrect account id %v", int(property.NumericValue))
				}
				total.Income(o.Summa)
				return nil
			}
		}
	}
	return nil
}

// Перевод средств между платежными картами
func (d *DB) handleTRFR(o *entities.FinanceOperation, op *entities.FinanceTotal, current map[int]*entities.FinanceTotal) error {
	op.Expenditure(o.Summa)
	if o.FinOpProperies != nil {
		for _, property := range o.FinOpProperies {
			if property.PropertyCode == "SECA" {
				total, ok2 := current[int(property.NumericValue)]
				if !ok2 {
					return fmt.Errorf("SECA property: Incorrect account id %v", int(property.NumericValue))
				}
				total.Income(o.Summa)
				return nil
			}
		}
	}
	return nil
}

func (d *DB) BuildTotals(result map[int][]entities.FinanceTotal, from int) error {
	current := make(map[int]*entities.FinanceTotal)
	var currentTotal []entities.FinanceTotal
	max := 0

	if from != 0 {
		for k, v := range result {
			if k < from && k > max {
				currentTotal = v
				max = k
			}
		}
	}

	for _, account := range d.Accounts {
		current[account.Id] = initAccount(account.Id, currentTotal)
	}

	var ops []kv
	for k, v := range d.FinanceOperations {
		if k >= from {
			ops = append(ops, kv{k, v})
		}
	}
	sort.Slice(ops, func(i, j int) bool {
		return ops[i].key < ops[j].key
	})
	for _, op := range ops {
		operations := op.value
		for _, operation := range operations {
			err := d.handle(&operation, current)
			if err != nil {
				return err
			}
		}
		var list []entities.FinanceTotal
		for _, total := range current {
			list = append(list, total.Clone())
			total.Reset()
		}
		result[op.key] = list
	}

	return nil
}

func initAccount(accountId int, currentTotal []entities.FinanceTotal) *entities.FinanceTotal {
	if currentTotal != nil {
		for _, total := range currentTotal {
			if total.AccountId == accountId {
				return &entities.FinanceTotal{accountId, 0, 0, total.Balance}
			}
		}
	}

	return &entities.FinanceTotal{accountId, 0, 0, 0}
}

func (d *DB) DeleteOperaton(data *entities.FinanceOperationDelete) error {
	ops, ok := d.FinanceOperations[data.Date.Value]
	if !ok {
		return errors.New("Operations for given date not found.")
	}
	for i, op := range ops {
		if op.Id == data.Id {
			l := len(ops) - 1
			ops[i] = ops[l]
			ops = ops[:l]
			d.FinanceOperations[data.Date.Value] = ops
			err := d.BuildTotals(d.FinanceTotals, data.Date.Value)
			if err != nil {
				return err
			}
			return d.commitFinanceOperations(data.Date.Value)
		}
	}

	return errors.New("Operation not found.")
}

func (d *DB) AddOperaton(data *entities.FinanceOperationAdd) error {
	if !data.Date.IsNotNull {
		return errors.New("NULL date")
	}
	if len(data.Summa) == 0 {
		return errors.New("Empty summa")
	}
	var amount entities.NDecimal
	if len(data.Amount) > 0 {
		amountFloat, err := expreval.Eval(data.Amount, parserStackSize)
		if err != nil {
			return errors.New("Incorrect amount: " + err.Error())
		}
		amount.IsNotNull = true
		amount.Value = int(math.Round(amountFloat*1000))
	} else {
		amount.IsNotNull = false
	}
	summa, err := expreval.Eval(data.Summa, parserStackSize)
	if err != nil {
		return errors.New("Incorrect summa: " + err.Error())
	}

	ops, ok := d.FinanceOperations[data.Date.Value]
	if ok {
		err = checkOperations(ops, 0, data.SubcategoryId, data.AccountId)
		if err != nil {
			return err
		}
	}

	finOpProperies := data.Properties
	if finOpProperies != nil && len(finOpProperies) == 0 {
		finOpProperies = nil
	}
	op := entities.FinanceOperation{
		Id:             d.NextOperationId,
		Amount:         amount,
		Summa:          entities.Decimal(math.Round(summa*100)),
		SubcategoryId:  data.SubcategoryId,
		FinOpProperies: finOpProperies,
		AccountId:      data.AccountId,
	}
	d.updateLastAccountOperationDateMap(op.AccountId, data.Date.Value)
	d.NextOperationId++
	if !ok {
		d.FinanceOperations[data.Date.Value] = []entities.FinanceOperation{op}
	} else {
		d.FinanceOperations[data.Date.Value] = append(ops, op)
	}
	err = d.BuildTotals(d.FinanceTotals, data.Date.Value)
	if err != nil {
		return err
	}

	return d.commitFinanceOperations(data.Date.Value)
}

func (d *DB) ModifyOperaton(data *entities.FinanceOperationModify) error {
	if !data.Date.IsNotNull {
		return errors.New("NULL date")
	}
	if len(data.Summa) == 0 {
		return errors.New("Empty summa")
	}
	var amount entities.NDecimal
	if len(data.Amount) > 0 {
		amountFloat, err := expreval.Eval(data.Amount, parserStackSize)
		if err != nil {
			return errors.New("Incorrect amount: " + err.Error())
		}
		amount.IsNotNull = true
		amount.Value = int(math.Round(amountFloat*1000))
	} else {
		amount.IsNotNull = false
	}
	summa, err := expreval.Eval(data.Summa, parserStackSize)
	if err != nil {
		return errors.New("Incorrect summa: " + err.Error())
	}
	ops, ok := d.FinanceOperations[data.Date.Value]
	if !ok {
		return errors.New("Operations for given date not found.")
	}
	for i := 0; i < len(ops); i++ {
		if ops[i].Id == data.Id {
			err = checkOperations(ops, data.Id, data.SubcategoryId, data.AccountId)
			if err != nil {
				return err
			}
			ops[i].Amount = amount
			ops[i].Summa = entities.Decimal(math.Round(summa*100))
			ops[i].SubcategoryId = data.SubcategoryId
			ops[i].AccountId = data.AccountId
			finOpProperies := data.Properties
			if finOpProperies != nil && len(finOpProperies) == 0 {
				finOpProperies = nil
			}
			ops[i].FinOpProperies = finOpProperies
			err = d.BuildTotals(d.FinanceTotals, data.Date.Value)
			if err != nil {
				return err
			}
			return d.commitFinanceOperations(data.Date.Value)
		}
	}

	return errors.New("Operation not found.")
}

func checkOperations(ops []entities.FinanceOperation, id int, subcategory int, account int) error {
	for _, op := range ops {
		if op.Id != id && op.AccountId == account && op.SubcategoryId == subcategory {
			return fmt.Errorf("duplicate operation")
		}
	}
	return nil
}

func (d *DB) commitFinanceOperations(date int) error {
	ops, ok := d.FinanceOperations[date]
	if !ok {
		return nil
	}

	err := entities.WriteFinanceOperationsToJson(fmt.Sprintf("%v/dates/%v", d.path, date), "/financeOperations.json",
		                                         ops)
	if err != nil {
		return err
	}

	if len(ops) == 0 {
		delete(d.FinanceOperations, date)
		delete(d.FinanceTotals, date)
	}

	return nil
}

func (d *DB) getValidTotals(date int, totals []entities.FinanceTotal) []entities.FinanceTotal {
	var result []entities.FinanceTotal

	for _, account := range d.Accounts {
		if !account.ActiveTo.IsNotNull || account.ActiveTo.Value >= date {
			for _, total := range totals {
				if total.AccountId == account.Id {
					result = append(result, total)
					break
				}
			}
		}
	}

	return result
}

func (d *DB) GetHints() map[string][]string {
	result := make(map[string][]string)
	for _, ops := range d.FinanceOperations {
		for _, op := range ops {
			if op.FinOpProperies != nil {
				for _, property := range op.FinOpProperies {
					if property.StringValue.IsNotNull && len(property.StringValue.Value) > 0 {
						arr, ok := result[property.PropertyCode]
						if !ok {
							arr = []string{property.StringValue.Value}
						} else {
							found := false
							for _, str := range arr {
								if str == property.StringValue.Value {
									found = true
									break
								}
							}
							if !found {
								arr = append(arr, property.StringValue.Value)
							}
						}
						result[property.PropertyCode] = arr
					}
				}
			}
		}
	}
	return result
}

func (d *DB) GetGasOperations(date1 int, date2 int) map[int][]entities.FinanceOperation {
	result := make(map[int][]entities.FinanceOperation)
	for date1 <= date2 {
		ops, ok := d.FinanceOperations[date1]
		if ok {
			var opsResult []entities.FinanceOperation
			for _, op := range ops {
				if op.SubcategoryId == 75 && checkGasProperties(op.FinOpProperies) && op.Summa > 0 && op.Amount.IsNotNull &&
					op.Amount.Value > 0 {
					opsResult = append(opsResult, op)
				}
			}
			if len(opsResult) > 0 {
				result[date1] = opsResult
			}
		}
		date1 = nextDate(date1)
	}
	return result
}

func checkGasProperties(properies []entities.FinOpProperty) bool {
	if properies == nil || len(properies) != 3 {
		return false
	}
	n := 0
	for _, prop := range properies {
		if prop.PropertyCode == "NETW" || prop.PropertyCode == "DIST" || prop.PropertyCode == "TYPE" {
			n++
		}
	}
	return n == 3
}

func (d *DB) GetPrihodRashod(date1 int, date2 int, categoryId int, subcategoryId int, accountId int) ([]entities.PrihodRashodByDay, error) {
	var result []entities.PrihodRashodByDay
	current := make(map[int]*entities.FinanceTotal)
	for _, account := range d.Accounts {
		current[account.Id] = &entities.FinanceTotal{AccountId: account.Id}
	}
	for date1 <= date2 {
		ops, ok := d.FinanceOperations[date1]
		if ok {
			for _, op := range ops {
				subcat, ok := d.Subcategories[op.SubcategoryId]
				if !ok {
					return nil, fmt.Errorf("Incorrect subcategory id %v", op.SubcategoryId)
				}
				if subcategoryId == 0  {
					if subcat.CategoryId == 10 || (subcat.CategoryId == 9 && subcat.OperationCodeId == "SPCL") {
						continue
					}
				}
				if subcategoryId == 0 || op.SubcategoryId == subcategoryId {
					match := true
					if categoryId != 0 {
						match = subcat.CategoryId == categoryId
					}
					if match {
						err := d.handle(&op, current)
						if err != nil {
							return nil, err
						}
						if accountId == 0 {
							for _, account := range d.Accounts {
								if current[account.Id].SummaIncome != 0 || current[account.Id].SummaExpenditure != 0 {
									result = append(result, entities.PrihodRashodByDay{
										Date:          date1,
										CategoryId:    subcat.CategoryId,
										SubcategoryId: op.SubcategoryId,
										AccountId:     op.AccountId,
										ValutaCode:    d.Accounts[op.AccountId].ValutaCode,
										Prihod:        int(current[account.Id].SummaIncome),
										Rashod:        int(current[account.Id].SummaExpenditure),
									})
								}
							}
 						} else if current[accountId].SummaIncome != 0 || current[accountId].SummaExpenditure != 0 {
							result = append(result, entities.PrihodRashodByDay{
								Date:          date1,
								CategoryId:    subcat.CategoryId,
								SubcategoryId: op.SubcategoryId,
								AccountId:     accountId,
								ValutaCode:    d.Accounts[accountId].ValutaCode,
								Prihod:        int(current[accountId].SummaIncome),
								Rashod:        int(current[accountId].SummaExpenditure),
							})
						}
						for _, total := range current {
							total.Reset()
						}
					}
				}
			}
		}
		date1 = nextDate(date1)
	}
	return result, nil
}

func nextDate(date1 int) int {
	t := time.Date(date1 / 10000, time.Month((date1 / 100) % 100), date1 % 100, 0, 0, 0, 0, time.UTC).
		           AddDate(0, 0, 1)
	return t.Year() * 10000 + int(t.Month()) * 100 + t.Day()
}

/*func (a *DB) GetFinanceTotals(date int) []entities.FinanceTotal {
	var result []entities.FinanceTotal
	for date >= a.MinDate {
		var ok bool
		result, ok = a.FinanceTotals[date]
		if ok {
			break
		}
		date--
	}
	if date >= a.MinDate {
		return a.getValidTotals(date, result)
	}
	return []entities.FinanceTotal{}
}*/

func (d *DB) GetFinanceOperationsAndTotals(date int) []entities.FinanceTotalAndOperations {
	ops, opsPresent := d.FinanceOperations[date]
	var totals []entities.FinanceTotal
	for date >= d.MinDate {
		var ok bool
		totals, ok = d.FinanceTotals[date]
		if ok {
			break
		}
		date--
	}
	if date >= d.MinDate {
		var result []entities.FinanceTotalAndOperations
		totals = d.getValidTotals(date, totals)
		for _, total := range d.sortTotals(totals, ops, opsPresent) {
			result = append(result, entities.FinanceTotalAndOperations{
				FinanceTotal:  total,
				Operations: getAccountOps(ops, opsPresent, total.AccountId),
			})
		}
		return result
	}
	return []entities.FinanceTotalAndOperations{}
}

func (d *DB) updateLastAccountOperationDateMap(accountId int, date int) {
	lastDate, ok := d.lastAccountOperationDateMap[accountId]
	if !ok || lastDate < date {
		d.lastAccountOperationDateMap[accountId] = date
	}
}

func (d *DB) sortTotals(totals []entities.FinanceTotal, ops []entities.FinanceOperation, opsPresent bool) []entities.FinanceTotal {
    sort.Slice(totals, func(i, j int) bool { return d.compareAccounts(totals[i].AccountId, totals[j].AccountId, ops, opsPresent) })
	return totals
}

func (d *DB) compareAccounts(accountId1 int, accountId2 int, ops []entities.FinanceOperation, opsPresent bool) bool {
    cnt1 := len(getAccountOps(ops, opsPresent, accountId1))
	cnt2 := len(getAccountOps(ops, opsPresent, accountId2))
	if cnt1 != 0 || cnt2 != 0 {
		if cnt1 == cnt2 {
			return strings.Compare(d.Accounts[accountId1].Name, d.Accounts[accountId2].Name) < 0
		}
		return cnt1 > cnt2
	} else {
		date1, ok := d.lastAccountOperationDateMap[accountId1]
		if !ok {
			date1 = 0
		}
		date2, ok := d.lastAccountOperationDateMap[accountId2]
		if !ok {
			date2 = 0
		}
		if date1 == date2 {
			return strings.Compare(d.Accounts[accountId1].Name, d.Accounts[accountId2].Name) < 0
		}
		return date1 > date2
	}
}

func getAccountOps(ops []entities.FinanceOperation, opsPresent bool, accountId int) []entities.FinanceOperation {
	var result []entities.FinanceOperation

	if opsPresent {
		for _, op := range ops {
			if op.AccountId == accountId {
				result = append(result, op)
			}
		}
	}

	return result
}
