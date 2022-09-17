package core

import (
  "HomeAccountingDB/src/core/entities"
  "fmt"
  "time"
)

type groupingFunc func(t int, categoryId int, valutaCode string, accountId int) int
type datesFunc func(t int) (entities.NDate, entities.NDate)

func groupByMonth(t int, _ int, valutaCode string, _ int) int {
  return ((t / 10000) % 100) * 100 + ((t / 100) % 100) * 100000 + buildHashCode(valutaCode)
}

func groupByCategory(_ int, categoryId int, valutaCode string, _ int) int {
  return categoryId * 100000 + buildHashCode(valutaCode)
}

func groupByAccount(_ int, _ int, _ string, accountId int) int {
  return accountId
}

func buildHashCode(valutaCode string) int {
  return int(valutaCode[0] - 'A') * 40 * 40 + int(valutaCode[1] - 'A') * 40 + int(valutaCode[2] - 'A')
}

func truncateToMonth(t int) (entities.NDate, entities.NDate) {
  firstOfMonth := (t / 100) * 100 + 1
  return entities.NDate{Value: firstOfMonth, IsNotNull: true}, entities.NDate{Value: getLastDayNextMonth(firstOfMonth), IsNotNull: true}}

func getLastDayNextMonth(date int) int {
  t := time.Date(date / 10000, time.Month((date / 100) % 100), date % 100, 0, 0, 0, 0, time.UTC).
    AddDate(0, 1, -1)
  return t.Year() * 10000 + int(t.Month()) * 100 + t.Day()
}

func passNil(_ int) (entities.NDate, entities.NDate) {
  return entities.NDate{Value: 0, IsNotNull: false}, entities.NDate{Value: 0, IsNotNull: false}
}

func passDate(t int) (entities.NDate, entities.NDate) {
  return entities.NDate{Value: t, IsNotNull: true}, entities.NDate{Value: t, IsNotNull: true}
}

func BuildReportFromOperations(ops []entities.PrihodRashodByDay, grouping string) ([]entities.ReportItem, error) {
  var groupBy groupingFunc
  var datesBuilder datesFunc
  switch grouping {
  case "Month":
    groupBy = groupByMonth
    datesBuilder = truncateToMonth
  case "Category":
    groupBy = groupByCategory
    datesBuilder = passNil
  case "Account":
    groupBy = groupByAccount
    datesBuilder = passNil
  case "Detailed":
    groupBy = nil
    datesBuilder = passDate
  default:
    return nil, fmt.Errorf("unknown groupBy value: %v", grouping)
  }
  accumulatorMap := make(map[int]entities.ReportItem, 0)
  result := make([]entities.ReportItem, 0)
  for _, op := range ops {
    if groupBy != nil {
      key := groupBy(op.Date, op.CategoryId, op.ValutaCode, op.AccountId)
      ri, ok := accumulatorMap[key]
      if !ok {
        ri = entities.ReportItem{}
      }
      if ri.CategoryId == 0 {
        ri.CategoryId = op.CategoryId
      } else if ri.CategoryId != op.CategoryId {
        ri.CategoryId = -1
      }
      if ri.SubcategoryId == 0 {
        ri.SubcategoryId = op.SubcategoryId
      } else if ri.SubcategoryId != op.SubcategoryId {
        ri.SubcategoryId = -1
      }
      if ri.AccountId == 0 {
        ri.AccountId = op.AccountId
      } else if ri.AccountId != op.AccountId {
        ri.AccountId = -1
      }
      ri.ValutaCode = op.ValutaCode
      ri.SummaIncome += op.Prihod
      ri.SummaExpenditure += op.Rashod
      ri.Date1, ri.Date2 = datesBuilder(op.Date)
      accumulatorMap[key] = ri
    } else {
      result = append(result, entities.ReportItem{Date1: entities.NDate{Value: op.Date, IsNotNull: true}, CategoryId: op.CategoryId,
                      SubcategoryId: op.SubcategoryId, ValutaCode: op.ValutaCode, SummaIncome: op.Prihod, SummaExpenditure: op.Rashod})
    }
  }
  for _, value := range accumulatorMap {
    result = append(result, value)
  }
  return result, nil
}
