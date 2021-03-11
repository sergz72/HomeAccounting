package core

import (
	"core/entities"
	"reflect"
	"testing"
)

func TestGasReport(t *testing.T) {
	db := DB{}
	db.Init("")

	db.FinanceOperations[20180101] = []entities.FinanceOperation{
		{Id: 1, Amount: entities.NDecimal{Value: 10000, IsNotNull: true}, Summa: 40000, SubcategoryId: 75, AccountId: 1,
			FinOpProperies: []entities.FinOpProperty{
				{PropertyCode: "DIST", NumericValue: 1000, StringValue: entities.NString{IsNotNull: false, Value: ""}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "NETW", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "Okko"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "TYPE", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "95Puls"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
			}},
	}
	db.FinanceOperations[20180102] = []entities.FinanceOperation{
		{Id: 2, Amount: entities.NDecimal{Value: 20000, IsNotNull: true}, Summa: 50000, SubcategoryId: 75, AccountId: 1,
			FinOpProperies: []entities.FinOpProperty{
				{PropertyCode: "DIST", NumericValue: 1500, StringValue: entities.NString{IsNotNull: false, Value: ""}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "NETW", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "KLO"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "TYPE", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "95Ventus"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
			}},
	}
	db.FinanceOperations[20180103] = []entities.FinanceOperation{
		{Id: 2, Amount: entities.NDecimal{Value: 14000, IsNotNull: true}, Summa: 45000, SubcategoryId: 75, AccountId: 1,
			FinOpProperies: []entities.FinOpProperty{
				{PropertyCode: "DIST", NumericValue: 1900, StringValue: entities.NString{IsNotNull: false, Value: ""}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "NETW", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "Shell"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
				{PropertyCode: "TYPE", NumericValue: 0, StringValue: entities.NString{IsNotNull: true, Value: "95"}, DateValue: entities.NDate{IsNotNull: false, Value: 0}},
			}},
	}

	ops, err := BuildGasReportFromOperations(db.GetGasOperations(20180101, 20180103))
	if err != nil {
		t.Errorf("BuildGasReportFromOperations error: %v", err.Error())
		return
	}

	expectedOps := []entities.GasReportItem{
		{
			Date:             entities.NDate{
				IsNotNull: true,
				Value:     20180102,
			},
			Network:          "KLO",
			Type:             "95Ventus",
			Amount:           20000,
			PricePerLiter:    2500,
			AbsoluteDistance: 1500,
			RelativeDistance: 400,
			LitersPer100km:   35,
		},
		{
			Date:             entities.NDate{
				IsNotNull: true,
				Value:     20180101,
			},
			Network:          "Okko",
			Type:             "95Puls",
			Amount:           10000,
			PricePerLiter:    4000,
			AbsoluteDistance: 1000,
			RelativeDistance: 500,
			LitersPer100km:   40,
		},
	}

	if !reflect.DeepEqual(ops, expectedOps) {
		t.Errorf("Report items are not equal: %v and %v", ops, expectedOps)
		return
	}
}

