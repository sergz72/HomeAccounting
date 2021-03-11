package main

import (
	"core"
	"core/entities"
	"fmt"
	"os"
	"sort"
	"strconv"
	"time"
)

func main() {
	if !((len(os.Args) == 3 && os.Args[2] == "test") || (len(os.Args) == 4 && os.Args[2] == "server")) {
		fmt.Println("Usage: HomeAccountingDB db_files_location test|server portNumber")
		return
	}
	fmt.Println("Reading DB files...")
	test := os.Args[2] == "test"
	start := time.Now()
	db := core.DB{}
	err := db.Load(os.Args[1], !test)
	if err != nil {
		fmt.Println("Database load error: " + err.Error())
		return
	}
	fmt.Printf("%v elapsed.\n", time.Since(start))
	if test {
		fmt.Println("Calculating finance totals...")
		start = time.Now()
		totals := make(map[int][]entities.FinanceTotal)
		err := db.BuildTotals(totals, 0)
		if err != nil {
			fmt.Println("BuildTotals error: " + err.Error())
			return
		}
		fmt.Printf("%v elapsed.\n", time.Since(start))
		fmt.Println("Checking finance totals...")
		start = time.Now()
		compareTotals(db.FinanceTotals, totals)
		fmt.Printf("%v elapsed.\n", time.Since(start))
	}
	if !test {
		portNumber, err := strconv.Atoi(os.Args[3])
		if err != nil || portNumber <= 0 {
			fmt.Println("Incorrect port number")
			return
		}
		err = core.ServerStart(&db, portNumber)
		if err != nil {
			fmt.Println("ServerStart error: " + err.Error())
		}
	}
}

func fillAccounts(totals []entities.FinanceTotal, accounts map[int]bool) {
	for _, total := range totals {
		if _, ok := accounts[total.AccountId]; !ok {
			accounts[total.AccountId] = true
		}
	}
}

func getTotalByAccountId(totals []entities.FinanceTotal, account int) *entities.FinanceTotal {
	for _, total := range totals {
		if total.AccountId == account {
			return &total
		}
	}
	return nil
}

type kvt struct {
	key   int
	value []entities.FinanceTotal
}

func compareTotals(financeTotals1 map[int][]entities.FinanceTotal, financeTotals2 map[int][]entities.FinanceTotal) {
	if len(financeTotals1) != len(financeTotals2) {
		fmt.Printf("size mismatch : %v and %v\n", len(financeTotals1), len(financeTotals2))
		return
	}

	var t []kvt
	for k, v := range financeTotals1 {
		t = append(t, kvt{k, v})
	}
	sort.Slice(t, func(i, j int) bool {
		return t[i].key < t[j].key
	})

	for _, v := range t {
		k := v.key
		totals1 := v.value
		if totals2, ok := financeTotals2[k]; !ok {
			fmt.Printf("date is absent: %v\n", k)
			return
		} else {
			accounts := make(map[int]bool)
			fillAccounts(totals1, accounts)
			fillAccounts(totals2, accounts)
			for account, _ := range accounts {
				total1 := getTotalByAccountId(totals1, account)
				total2 := getTotalByAccountId(totals2, account)
				if total1 != nil && total2 == nil && (total1.SummaIncome != 0 ||
					total1.SummaExpenditure != 0 ||
					total1.Balance != 0) {
					fmt.Printf("account %v is absent for date %v  in 2 : %v\n", account, k, total1)
					return
				}
				if total2 != nil && total1 == nil && (total2.SummaIncome != 0 ||
					total2.SummaExpenditure != 0 ||
					total2.Balance != 0) {
					fmt.Printf("account %v is absent for date %v  in 1 : %v\n", account, k, total2)
					return
				}
				if total1 != nil && total2 != nil {
					if total1.SummaIncome != total2.SummaIncome {
						fmt.Printf("account %v summaIncome mismatch for date %v: %v and %v\n", account, k, total1.SummaIncome, total2.SummaIncome)
						return
					}
					if total1.SummaExpenditure != total2.SummaExpenditure {
						fmt.Printf("account %v summaExpenditure mismatch for date %v: %v and %v\n", account, k, total1.SummaExpenditure, total2.SummaExpenditure)
						return
					}
					if total1.Balance != total2.Balance {
						fmt.Printf("account %v balance mismatch for date %v: %v and %v\n", account, k, total1.Balance, total2.Balance)
						return
					}
				}
			}
		}
	}
}
