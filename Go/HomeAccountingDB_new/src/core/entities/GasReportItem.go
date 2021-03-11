package entities

type GasReportItem struct {
	Date NDate
	Network string
	Type string
	Amount int
	PricePerLiter int
	AbsoluteDistance int
	RelativeDistance int
	LitersPer100km int
}
