package entities

import (
	"database/sql"
	"encoding/json"
	"github.com/lib/pq"
	_ "github.com/lib/pq"
	"time"
)

type NInt struct {
	Value sql.NullInt64
}

func (n *NInt) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		n.Value.Valid = false
		return nil
	}
	n.Value.Valid = true
	return json.Unmarshal(b, &n.Value.Int64)
}

func (n NInt) MarshalJSON() (b []byte, err error) {
	if n.Value.Valid {
		return json.Marshal(n.Value.Int64)
	}
	return json.Marshal(nil)
}

type NString struct {
	Value  sql.NullString
}

func (n *NString) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		n.Value.Valid = false
		return nil
	}
	n.Value.Valid = true
	return json.Unmarshal(b, &n.Value.String)
}

func (n NString) MarshalJSON() (b []byte, err error) {
	if n.Value.Valid {
		return json.Marshal(n.Value.String)
	}
	return json.Marshal(nil)
}

type NFloat struct {
	Value  sql.NullFloat64
}

func (n *NFloat) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		n.Value.Valid = false
		return nil
	}
	n.Value.Valid = true
	return json.Unmarshal(b, &n.Value.Float64)
}

func (n NFloat) MarshalJSON() (b []byte, err error) {
	if n.Value.Valid {
		return json.Marshal(n.Value.Float64)
	}
	return json.Marshal(nil)
}

type NDate struct {
	Value pq.NullTime
}

func (n *NDate) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		n.Value.Valid = false
		return nil
	}
	var parts []int
	err := json.Unmarshal(b, &parts)
	if err != nil {
		return err
	}
	n.Value.Valid = true
	n.Value.Time = time.Date(parts[0], time.Month(parts[1]), parts[2], 0, 0, 0, 0, time.UTC)
	return nil
}

func (n NDate) MarshalJSON() (b []byte, err error) {
	if n.Value.Valid {
		parts := []int{n.Value.Time.Year(), int(n.Value.Time.Month()), n.Value.Time.Day()}
		return json.Marshal(parts)
	}
	return json.Marshal(nil)
}

type NTime struct {
	Value pq.NullTime
}

func (n *NTime) UnmarshalJSON(b []byte) error {
	if string(b) == "null" {
		n.Value.Valid = false
		return nil
	}
	n.Value.Valid = true
	return json.Unmarshal(b, &n.Value.Time)
}

func (n NTime) MarshalJSON() (b []byte, err error) {
	if n.Value.Valid {
		return json.Marshal(n.Value.Time)
	}
	return json.Marshal(nil)
}
