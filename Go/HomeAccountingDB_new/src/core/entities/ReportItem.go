package entities

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
