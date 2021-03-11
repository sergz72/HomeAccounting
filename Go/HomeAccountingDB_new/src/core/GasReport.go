package core

import (
	"core/entities"
	"fmt"
	"sort"
)

func BuildGasReportFromOperations(opsMap map[int][]entities.FinanceOperation) ([]entities.GasReportItem, error) {
	var keys []int
	for k, _ := range opsMap {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool {
		return keys[i] > keys[j]
	})
	var result []entities.GasReportItem
	nextDistance := 0
	nextAmount := 0
	for _, k := range keys {
		ops := opsMap[k]
		if len(ops) > 1 {
			sort.Slice(ops, func(i, j int) bool {
				return getDistance(ops[i]) > getDistance(ops[j])
			})
		}
		for _, op := range ops {
			if !op.Amount.IsNotNull || (op.Amount.IsNotNull && op.Amount.Value == 0) {
				return nil, fmt.Errorf("invalid amount: %v, date: %v", op.Amount, k)
			}
			distance := 0
			network := ""
			typ := ""
			for _, prop := range op.FinOpProperies {
				switch prop.PropertyCode {
				case "DIST":
					distance = int(prop.NumericValue)
				case "NETW":
					network = prop.StringValue.Value
				case "TYPE":
					typ = prop.StringValue.Value
				}
			}
			if nextDistance != 0 {
				delta := nextDistance - distance
				if delta == 0 {
					return nil, fmt.Errorf("zero delta")
				}
				pricePerLiter := int(op.Summa) * 1000 / op.Amount.Value
				litersPer100km := nextAmount / delta
				row := entities.GasReportItem{
					Date:           entities.NDate{
						IsNotNull: true,
						Value:     k,
					},
					Network:        network,
					Type:           typ,
					Amount:         op.Amount.Value,
					PricePerLiter:  pricePerLiter,
					AbsoluteDistance: distance,
					RelativeDistance: delta,
					LitersPer100km: litersPer100km,
				}
				result = append(result, row)
			}
			nextDistance = distance
			nextAmount = op.Amount.Value
		}
	}

	return result, nil
}

func getDistance(op entities.FinanceOperation) int {
	for _, prop := range op.FinOpProperies {
		if prop.PropertyCode == "DIST" {
			return int(prop.NumericValue)
		}
	}
	return 0
}

