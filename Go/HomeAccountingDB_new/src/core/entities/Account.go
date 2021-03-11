package entities

import (
  "encoding/json"
	"io/ioutil"
)

type Account struct {
	Id int
	Name string
	IsCash bool
	ActiveTo NDate
	ValutaCode string
}

func ReadAccountsFromJson(path string) ([]Account, error) {
  var accounts []Account
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &accounts); err != nil {
		return nil, err
  }
  
  return accounts, nil
}