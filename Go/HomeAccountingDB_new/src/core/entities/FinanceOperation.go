package entities

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"os"
)

type FinOpProperty struct {
	NumericValue float64
	StringValue  NString
	DateValue    NDate
	PropertyCode string
}

type FinanceOperation struct {
	Id                    int
	Amount                NDecimal
	Summa                 Decimal
	SubcategoryId         int
	FinOpProperies        []FinOpProperty
	AccountId             int
}

func ReadFinanceOperationsFromJson(path string) ([]FinanceOperation, error) {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return nil, nil
	}
	var operations []FinanceOperation
	dat, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal(dat, &operations); err != nil {
		return nil, err
	}

	return operations, nil
}

func WriteFinanceOperationsToJson(path string, fileName string, operations []FinanceOperation) error {
	if len(operations) > 0 {
		if _, err := os.Stat(path); os.IsNotExist(err) {
			err = os.Mkdir(path, 0775)
			if err != nil {
				return err
			}
		}

		bytess, errm := json.Marshal(operations)
		if errm != nil {
			return errm
		}
		var out bytes.Buffer
		errm = json.Indent(&out, bytess, "", "  ")

		return ioutil.WriteFile(path + fileName, out.Bytes(), 0644)
	}
	return os.RemoveAll(path)
}

type FinanceOperationAdd struct {
	Amount          string
	Summa           string
	SubcategoryId   int
	AccountId       int
	Date            NDate
	Properties      []FinOpProperty
}

type FinanceOperationDelete struct {
	Id   int
	Date NDate
}

type FinanceOperationModify struct {
	Id              int
	Amount          string
	Summa           string
	SubcategoryId   int
	AccountId       int
	Date            NDate
	Properties      []FinOpProperty
}
