package entities

import (
  "encoding/json"
	"io/ioutil"
)

type Category struct {
	Id int
	Name string
}

func ReadCategoriesFromJson(path string) ([]Category, error) {
  var categories []Category
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &categories); err != nil {
		return nil, err
  }
  
  return categories, nil
}