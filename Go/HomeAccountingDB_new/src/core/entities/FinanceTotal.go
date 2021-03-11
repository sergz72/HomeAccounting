package entities

import (
  "encoding/json"
	"io/ioutil"
	"os"
)

type FinanceTotal struct {
	AccountId int
	SummaExpenditure Decimal
	SummaIncome Decimal
	Balance Decimal
}
func (t *FinanceTotal) Income(summa Decimal) {
  t.SummaIncome += summa
  t.Balance += summa
}
func (t *FinanceTotal) Expenditure(summa Decimal) {
  t.SummaExpenditure += summa
  t.Balance -= summa
}
func (t *FinanceTotal) Clone() FinanceTotal {
    return FinanceTotal{t.AccountId, t.SummaExpenditure, t.SummaIncome, t.Balance};
}
func (t *FinanceTotal) Reset() {
    t.SummaExpenditure = 0
    t.SummaIncome = 0
}

func ReadFinanceTotalsFromJson(path string) ([]FinanceTotal, error) {
  if _, err := os.Stat(path); os.IsNotExist(err) {
    return nil, nil
  }
  var totals []FinanceTotal
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &totals); err != nil {
    return nil, err
  }
  
  return totals, nil
}