package entities

import (
  "encoding/json"
	"io/ioutil"
)

type Subcategory struct {
	Id int
	Code string
	Name string
	OperationCodeId string
	CategoryId int
}

func ReadSubcategoriesFromJson(path string) ([]Subcategory, error) {
  var subcategories []Subcategory
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &subcategories); err != nil {
		return nil, err
  }
  
  return subcategories, nil
}