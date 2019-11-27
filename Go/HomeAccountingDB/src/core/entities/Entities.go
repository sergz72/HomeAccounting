package entities

import "time"

type Account struct {
	Id int
	Name string
	IsCash bool
	ActiveTo NDate
	ValutaCode string
}

type Category struct {
	Id int
	Name string
}

type FinanceOperation struct {
	Id                    int
	Amount                NInt
	Summa                 int
	SubcategoryId         int
	FinOpProperiesGroupId NInt
	AccountId             int
}

type PrihodRashodByDay struct {
	Date                  time.Time
	CategoryId            int
	SubcategoryId         int
	AccountId             int
	ValutaCode            string
	Prihod                int
	Rashod                int
}

type FinanceOperationDelete struct {
	Id   int
	Date NDate
}

type FinanceOperationAdd struct {
	Amount          string
	Summa           string
	SubcategoryId   int
	AccountId       int
	Date            NDate
	Properties      []FinOpProperty
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

type FinanceTotal struct {
	AccountId int
	SummaExpenditure int
	SummaIncome int
	Balance int
}

type FinOpProperty struct {
	NumericValue NFloat
	Id           int
	StringValue  NString
	DateValue    NTime
	PropertyCode string
}

type PropertyGroup struct {
	GroupId    int
	PropertyId int
}

type Subcategory struct {
	Id int
	Code NString
	Name string
	OperationCodeId string
	CategoryId int
}

type SubcategoryToPropertyCodeMap struct {
	SubcategoryCode string
	PropertyCode    string
}

type ReportItem struct {
	Date1 NDate
	Date2 NDate
	AccountId   int
	CategoryId   int
	SubcategoryId   int
	ValutaCode string
	SummaExpenditure int
	SummaIncome int
}
