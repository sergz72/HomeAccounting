package entities

import (
  "encoding/json"
	"io/ioutil"
)

type SubcategoryToPropertyCodeMap struct {
	SubcategoryCode string
	PropertyCode string
}

func ReadSubcategoryToPropertyCodeMapFromJson(path string) (map[string][]string, error) {
  var result []SubcategoryToPropertyCodeMap
  dat, err := ioutil.ReadFile(path)
  if err != nil {
    return nil, err
  }
  if err := json.Unmarshal(dat, &result); err != nil {
		return nil, err
  }

  resultMap := make(map[string][]string)
  for _, m := range result {
	  v, ok := resultMap[m.SubcategoryCode]
	  if ok {
		  v = append(v, m.PropertyCode)
		  resultMap[m.SubcategoryCode] = v
	  } else {
		  resultMap[m.SubcategoryCode] = []string{m.PropertyCode}
	  }
  }

  return resultMap, nil
}