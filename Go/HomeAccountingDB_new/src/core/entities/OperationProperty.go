package entities

import (
  "encoding/json"
	"io/ioutil"
)

type OperationProperty struct {
	Code string
	Name string
	DataType string
	NeedRecalc bool
}

func ReadOperationPropertiesFromJson(path string) ([]OperationProperty, error) {
  var properties []OperationProperty
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &properties); err != nil {
    return nil, err
  }
  
  return properties, nil
}