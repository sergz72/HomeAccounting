package core

import (
	"core/entities"
	"os"
	"reflect"
	"testing"
)

var tempPath = os.Getenv("TEMP")
var folderName1 = tempPath + "/dates"
var folderName2 = folderName1 + "/20180102"

func initDB(t *testing.T) *DB {
	if _, err := os.Stat(folderName1); err == nil {
		if !destroyDB(t) {
			return nil
		}
	}
	err := os.Mkdir(folderName1, 0644)
	if err != nil {
		t.Error("directory1 creation failure: " + err.Error())
		return nil
	}

	err = os.Mkdir(folderName2, 0644)
	if err != nil {
		t.Error("directory2 creation failure: " + err.Error())
		return nil
	}

	db := DB{}
	db.Init(tempPath)

	db.Accounts[1] = entities.Account{Id: 1, Name: "test", IsCash: true, ActiveTo: entities.NDate{IsNotNull: false, Value: 0}, ValutaCode: "UAH"}
	db.Accounts[2] = entities.Account{Id: 2, Name: "testCard", IsCash: false, ActiveTo: entities.NDate{IsNotNull: false, Value: 0}, ValutaCode: "UAH"}
	db.Accounts[3] = entities.Account{Id: 3, Name: "testUSD", IsCash: true, ActiveTo: entities.NDate{IsNotNull: false, Value: 0}, ValutaCode: "USD"}
	db.Accounts[4] = entities.Account{Id: 4, Name: "testCard2", IsCash: false, ActiveTo: entities.NDate{IsNotNull: false, Value: 0}, ValutaCode: "UAH"}

	db.Subcategories[1] = entities.Subcategory{Id: 1, Name: "Salary", CategoryId: 1, OperationCodeId: "INCM"}
	db.Subcategories[2] = entities.Subcategory{Id: 2, Name: "Other", CategoryId: 2, OperationCodeId: "EXPN"}
	db.Subcategories[3] = entities.Subcategory{Id: 3, Name: "Gift", CategoryId: 1, OperationCodeId: "INCM"}
	db.Subcategories[4] = entities.Subcategory{Id: 4, Name: "Bazar", CategoryId: 2, OperationCodeId: "EXPN"}
	db.Subcategories[5] = entities.Subcategory{Id: 5, Name: "Currency exchange", CategoryId: 3, OperationCodeId: "SPCL", Code: "EXCH"}
	db.Subcategories[6] = entities.Subcategory{Id: 6, Name: "Money transfer", CategoryId: 3, OperationCodeId: "SPCL", Code: "TRFR"}
	db.NextSubcategoryId = 7

	db.FinanceOperations[20180101] = []entities.FinanceOperation{
		{Id: 1, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 400, SubcategoryId: 1, FinOpProperies: nil, AccountId: 1},
		{Id: 2, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 500, SubcategoryId: 3, FinOpProperies: nil, AccountId: 1},
		{Id: 3, Amount: entities.NDecimal{Value: 5, IsNotNull: true}, Summa: 300, SubcategoryId: 1, FinOpProperies: nil, AccountId: 2},
		{Id: 4, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 100, SubcategoryId: 1, FinOpProperies: nil, AccountId: 3},
	}
	db.FinanceOperations[20180102] = []entities.FinanceOperation{
		{Id: 11, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 400, SubcategoryId: 2, FinOpProperies: nil, AccountId: 1},
		{Id: 12, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 300, SubcategoryId: 4, FinOpProperies: nil, AccountId: 1},
		{Id: 13, Amount: entities.NDecimal{Value: 5, IsNotNull: true}, Summa: 200, SubcategoryId: 2, FinOpProperies: nil, AccountId: 2},
	}
	db.FinanceOperations[20180103] = []entities.FinanceOperation{
		{Id: 21, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 100, SubcategoryId: 1, FinOpProperies: nil, AccountId: 1},
		{Id: 22, Amount: entities.NDecimal{Value: 0, IsNotNull: false}, Summa: 50, SubcategoryId: 2, FinOpProperies: nil, AccountId: 1},
		{Id: 23, Amount: entities.NDecimal{Value: 5, IsNotNull: true}, Summa: 70, SubcategoryId: 2, FinOpProperies: nil, AccountId: 2},
	}
	db.NextOperationId = 24
	err = db.BuildTotals(db.FinanceTotals, 0)
	if err != nil {
		t.Error("BuildTotals error: " + err.Error())
		return nil
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 700, SummaIncome: 0, Balance: 200},
		{AccountId: 2, SummaExpenditure: 200, SummaIncome: 0, Balance: 100},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 250},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 00, Balance: 30},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return nil
	}

	return &db
}

func compareTotals(sample map[int][]entities.FinanceTotal, fact map[int][]entities.FinanceTotal, t *testing.T) bool {
	if len(sample) != len(fact) {
		t.Errorf("Totals maps are not equal(length): %v and %v", sample, fact)
		return false
	}

	for key, value := range sample {
		valuef, ok := fact[key]
		if !ok {
			t.Errorf("Totals maps are not equal(key %v is missing): %v and %v", key, sample, fact)
			return false
		}
		for _, total := range value {
			found := false
			for _, totalf := range valuef {
				if total.AccountId == totalf.AccountId {
					found = true
					if total.SummaExpenditure != totalf.SummaExpenditure {
						t.Errorf("Totals maps are not equal(account %v SummaExpenditure mismatch for key %v): %v and %v", total.AccountId, key, sample, fact)
						return false
					}
					if total.SummaIncome != totalf.SummaIncome {
						t.Errorf("Totals maps are not equal(account %v SummaIncome mismatch for key %v): %v and %v", total.AccountId, key, sample, fact)
						return false
					}
					if total.Balance != totalf.Balance {
						t.Errorf("Totals maps are not equal(account %v Balance mismatch for key %v): %v and %v", total.AccountId, key, sample, fact)
						return false
					}
				}
			}
			if !found {
				t.Errorf("Totals maps are not equal(account %v is missing for key %v): %v and %v", total.AccountId, key, sample, fact)
				return false
			}
		}
	}

	return true
}

func destroyDB(t *testing.T) bool {
	err := os.RemoveAll(folderName1)
	if err != nil {
		t.Error("directory1 removal failure: " + err.Error())
		return false
	}

	return true
}

func TestDeleteOperation(t *testing.T) {
	db := initDB(t)
	if db == nil {
		destroyDB(t)
		return
	}
	opDelete := entities.FinanceOperationDelete{Id: 11, Date: entities.NDate{IsNotNull: true, Value: 20180102}}
	err := db.DeleteOperaton(&opDelete)
	if err != nil {
		t.Error(err.Error())
		return
	}
	if len(db.FinanceOperations[20180102]) != 2 {
		t.Errorf("wrong FinanceOperations count: %v, expected: %v", len(db.FinanceOperations), 2)
		return
	}
	ops, err := entities.ReadFinanceOperationsFromJson(folderName2 + "/financeOperations.json")
	if err != nil {
		t.Error(err.Error())
		return
	}
	if !reflect.DeepEqual(db.FinanceOperations[20180102], ops) {
		t.Errorf("Operation lists are not equal: %v and %v", db.FinanceOperations[20180102], ops)
		return
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 300, SummaIncome: 0, Balance: 600},
		{AccountId: 2, SummaExpenditure: 200, SummaIncome: 0, Balance: 100},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 650},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 0, Balance: 30},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return
	}

	destroyDB(t)
}

func TestAddOperation(t *testing.T) {
	db := initDB(t)
	if db == nil {
		destroyDB(t)
		return
	}
	opAdd := entities.FinanceOperationAdd{
	    Amount: "",
	    Summa: "1",
	    SubcategoryId: 1,
		AccountId: 2,
		Date: entities.NDate{IsNotNull: true, Value: 20180102},
	    Properties: nil,
	}
	err := db.AddOperaton(&opAdd)
	if err != nil {
		t.Error(err.Error())
		return
	}
	if len(db.FinanceOperations[20180102]) != 4 {
		t.Errorf("wrong FinanceOperations count: %v, expected: %v", len(db.FinanceOperations), 4)
		return
	}
	ops, err := entities.ReadFinanceOperationsFromJson(folderName2 + "/financeOperations.json")
	if err != nil {
		t.Error(err.Error())
		return
	}
	if !reflect.DeepEqual(db.FinanceOperations[20180102], ops) {
		t.Errorf("Operation lists are not equal: %v and %v", db.FinanceOperations[20180102], ops)
		return
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 700, SummaIncome: 0, Balance: 200},
		{AccountId: 2, SummaExpenditure: 200, SummaIncome: 100, Balance: 200},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 250},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 0, Balance: 130},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return
	}

	destroyDB(t)
}

func TestAddCurrencyExchangeOperation(t *testing.T) {
	db := initDB(t)
	if db == nil {
		destroyDB(t)
		return
	}
	opAdd := entities.FinanceOperationAdd{
		Date: entities.NDate{IsNotNull: true, Value: 20180102},
		Amount: "1",
		Summa: "26",
		SubcategoryId: 5,
		AccountId: 3,
		Properties: []entities.FinOpProperty{
			{PropertyCode: "SECA", NumericValue: 1, StringValue: entities.NString{IsNotNull: false, Value: ""}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
		},
	}
	err := db.AddOperaton(&opAdd)
	if err != nil {
		t.Errorf("Error: %v, financeOperations[20180102]: %v", err.Error(), db.FinanceOperations[20180102])
		return
	}
	if len(db.FinanceOperations[20180102]) != 4 {
		t.Errorf("wrong FinanceOperations count: %v, expected: %v", len(db.FinanceOperations), 4)
		return
	}
	ops, err := entities.ReadFinanceOperationsFromJson(folderName2 + "/financeOperations.json")
	if err != nil {
		t.Error(err.Error())
		return
	}
	if !reflect.DeepEqual(db.FinanceOperations[20180102], ops) {
		t.Errorf("Operation lists are not equal: %v and %v", db.FinanceOperations[20180102], ops)
		return
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 700, SummaIncome: 2600, Balance: 2800},
		{AccountId: 2, SummaExpenditure: 200, SummaIncome: 0, Balance: 100},
		{AccountId: 3, SummaExpenditure: 100, SummaIncome: 0, Balance: 0},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 2850},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 0, Balance: 30},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 0},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return
	}

	destroyDB(t)
}

func TestAddTransferOperation(t *testing.T) {
	db := initDB(t)
	if db == nil {
		destroyDB(t)
		return
	}
	opAdd := entities.FinanceOperationAdd{
		Date: entities.NDate{IsNotNull: true, Value: 20180102},
		Amount: "",
		Summa: "0.3",
		SubcategoryId: 6,
		AccountId: 2,
		Properties: []entities.FinOpProperty{
			{PropertyCode: "SECA", NumericValue: 4, StringValue: entities.NString{IsNotNull: false, Value: ""}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
		},
	}
	err := db.AddOperaton(&opAdd)
	if err != nil {
		t.Errorf("Error: %v, financeOperations[20180102]: %v", err.Error(), db.FinanceOperations[20180102])
		return
	}
	if len(db.FinanceOperations[20180102]) != 4 {
		t.Errorf("wrong FinanceOperations count: %v, expected: %v", len(db.FinanceOperations), 4)
		return
	}
	ops, err := entities.ReadFinanceOperationsFromJson(folderName2 + "/financeOperations.json")
	if err != nil {
		t.Error(err.Error())
		return
	}
	if !reflect.DeepEqual(db.FinanceOperations[20180102], ops) {
		t.Errorf("Operation lists are not equal: %v and %v", db.FinanceOperations[20180102], ops)
		return
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 700, SummaIncome: 0, Balance: 200},
		{AccountId: 2, SummaExpenditure: 230, SummaIncome: 0, Balance: 70},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
		{AccountId: 4, SummaExpenditure: 0, SummaIncome: 30, Balance: 30},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 250},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 00, Balance: 0},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
		{AccountId: 4, SummaExpenditure: 0, SummaIncome: 0, Balance: 30},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return
	}

	destroyDB(t)
}

func TestModifyOperation(t *testing.T) {
	db := initDB(t)
	if db == nil {
		destroyDB(t)
		return
	}
	opModify := entities.FinanceOperationModify{
		Id: 11,
		Amount: "",
		Summa: "3",
		SubcategoryId: 2,
		AccountId: 1,
		Date: entities.NDate{IsNotNull: true, Value: 20180102},
		Properties: nil,
	}
	err := db.ModifyOperaton(&opModify)
	if err != nil {
		t.Error(err.Error())
		return
	}
	if len(db.FinanceOperations[20180102]) != 3 {
		t.Errorf("wrong FinanceOperations count: %v, expected: %v", len(db.FinanceOperations), 4)
		return
	}
	ops, err := entities.ReadFinanceOperationsFromJson(folderName2 + "/financeOperations.json")
	if err != nil {
		t.Error(err.Error())
		return
	}
	if !reflect.DeepEqual(db.FinanceOperations[20180102], ops) {
		t.Errorf("Operation lists are not equal: %v and %v", db.FinanceOperations[20180102], ops)
		return
	}

	tot := make(map[int][]entities.FinanceTotal)

	tot[20180101] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 0, SummaIncome: 900, Balance: 900},
		{AccountId: 2, SummaExpenditure: 0, SummaIncome: 300, Balance: 300},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 100, Balance: 100},
	}

	tot[20180102] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 600, SummaIncome: 0, Balance: 300},
		{AccountId: 2, SummaExpenditure: 200, SummaIncome: 0, Balance: 100},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	tot[20180103] = []entities.FinanceTotal{
		{AccountId: 1, SummaExpenditure: 50, SummaIncome: 100, Balance: 350},
		{AccountId: 2, SummaExpenditure: 70, SummaIncome: 00, Balance: 30},
		{AccountId: 3, SummaExpenditure: 0, SummaIncome: 0, Balance: 100},
	}

	if !compareTotals(tot, db.FinanceTotals, t) {
		return
	}

	destroyDB(t)
}
