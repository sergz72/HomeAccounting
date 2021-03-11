package entities

import (
	"encoding/json"
)

type NInt struct {
	Value  int
	IsNotNull bool
}

func (n *NInt) UnmarshalJSON(b []byte) (err error) {
	if string(b) == "null" {
		return nil
	}
	n.IsNotNull = true
	return json.Unmarshal(b, &n.Value)
}

func (n NInt) MarshalJSON() (b []byte, err error) {
	if n.IsNotNull {
		return json.Marshal(n.Value)
	}
	return json.Marshal(nil)
}

type NDate struct {
	IsNotNull bool
	Value  int
}

func (n *NDate) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		return nil
	}
	n.IsNotNull = true
	var parts []int
	err := json.Unmarshal(b, &parts)
	if err != nil {
		return err
	}
	n.Value = parts[0]*10000 + parts[1]*100 + parts[2]
	return nil
}

func (n NDate) MarshalJSON() (b []byte, err error) {
	if n.IsNotNull {
		parts := []int{n.Value / 10000, (n.Value / 100) % 100, n.Value % 100}

		return json.Marshal(&parts)
	}
	return json.Marshal(nil)
}

type NDecimal struct {
	Value  int
	IsNotNull bool
}

func (n *NDecimal) UnmarshalJSON(b []byte) (err error) {
	if string(b) != "null" {
		n.IsNotNull = true
		n.Value = toInt(b)
	}
	return nil
}

func (n NDecimal) MarshalJSON() (b []byte, err error) {
	if n.IsNotNull {
		return json.Marshal(n.Value)
	}
	return json.Marshal(nil)
}

func toInt(b []byte) int {
	result := 0
	minus := false
	for _, c := range b {
		if c >= '0' && c <= '9' {
			result *= 10
			result += int(c - '0')
		} else if c == '-' {
			minus = true
		}
	}
	if minus {
		return -result
	}
	return result
}

type Decimal int

func (n *Decimal) UnmarshalJSON(b []byte) (err error) {
	*n = Decimal(toInt(b))
	return nil
}

type NString struct {
	Value  string
	IsNotNull bool
}

func (n *NString) UnmarshalJSON(b []byte) (err error) {
	if string(b) == "null" {
		return nil
	}
	n.IsNotNull = true
	return json.Unmarshal(b, &n.Value)
}

func (n NString) MarshalJSON() (b []byte, err error) {
	if n.IsNotNull {
		return json.Marshal(n.Value)
	}
	return json.Marshal(nil)
}
