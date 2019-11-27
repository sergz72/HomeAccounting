package expreval

import (
  "errors"
  "math"
  "testing"
)

const stackSize = 10
const maxDifference = 0.00000000000001

var tests = []struct {
  in  string
  out float64
  err error
}{
  {"1", 1, nil},
  {"2.34567890", 2.34567890, nil},
  {"-2.34567890", -2.34567890, nil},
  {"+2.34567890", 2.34567890, nil},
  {"2*(3+4)", 14, nil},
  {"2*(3-4)", -2, nil},
  {"-2*(3-4)", 2, nil},
  {"-2*(-3-4)", 14, nil},
  {".", 0, errors.New("empty statement")},
  {".1", 0.1, nil},
  {".1.", 0, errors.New("unexpected comma")},
  {"..1", 0, errors.New("unexpected comma")},
  {"1+2*3-4", 3, nil},
  {"1-2*3/6", 0, nil},
  {"1-2*3/0", 0, errors.New("division by zero")},
  {"2+(3+4)*(4-8)", -26, nil},
  {"2+(3+4)*(4-8)+8", 0, errors.New("output stack overflow")},
  {"2+(3+4)*(4-8", 0, errors.New(") is missing")},
  {"2+(3+4)*4-8)", 0, errors.New("( is missing")},
  {"2+(3+x)*4-8)", 0, errors.New("unexpected character: x")},
}

func TestExpressionEvaluator(t *testing.T) {
  for _, test := range tests {
    t.Run(test.in, func(t *testing.T) {
      result, err := Eval(test.in, stackSize)
      if (math.Abs(result-test.out) > maxDifference) {
        t.Errorf("Test: %v, expected: %v, got: %v", test.in, test.out, result)
      }
      if (err == nil && test.err != nil) {
        t.Errorf("Test: %v, expected error message: %v, got no error", test.in, test.err.Error())
      } else if (err != nil && test.err == nil) {
        t.Errorf("Test: %v, expected not error, got error message: %v", test.in, err)
      } else if (err != nil && test.err != nil && err.Error() != test.err.Error()) {
        t.Errorf("Test: %v, expected error message: %v, got error message: %v", test.in, test.err.Error(), err)
      }
    })
  }
}
