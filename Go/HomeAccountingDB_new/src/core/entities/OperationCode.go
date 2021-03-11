package entities

import (
  "encoding/json"
	"io/ioutil"
)

type OperationCode struct {
	Code string
	Name string
	Sign int
}

func ReadOperationCodesFromJson(path string) ([]OperationCode, error) {
  var codes []OperationCode
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &codes); err != nil {
		return nil, err
  }
  
  return codes, nil
}