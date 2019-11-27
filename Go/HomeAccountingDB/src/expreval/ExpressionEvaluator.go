package expreval

import (
  "database/sql"
  "errors"
  "fmt"
)

var priorities = map[int32]int{
  '+':  1,
  '-':  1,
  '*':  2,
  '/':  2,
  -'+': 3,
  -'-': 3,
}

type outputItem struct {
  op    int32
  value float64
}

type parser struct {
  output         []outputItem
  opStack        []int32
  dataStack      []float64
  outputPointer  int
  opStackPointer int
  stackSize      int
  op             sql.NullFloat64
  koef           float64
  prevOp         bool
}

func (p *parser) parseNumber(multiplier float64) {
  p.op.Valid = true
  if p.koef < 1.0 {
    p.op.Float64 += p.koef * multiplier
    p.koef /= 10
  } else {
    p.op.Float64 *= 10
    p.op.Float64 += multiplier
  }
  p.prevOp = false
}

func (p *parser) storeNumber() error {
  if p.op.Valid {
    if p.outputPointer >= p.stackSize {
      return errors.New("output stack overflow")
    }
    p.output[p.outputPointer] = outputItem{0, p.op.Float64}
    p.outputPointer++
    p.koef = 1.0
    p.op.Valid = false
    p.op.Float64 = 0
  }
  return nil
}

func (p *parser) moveToOutput(priority int) error {
  for p.opStackPointer > 0 {
    v := p.opStack[p.opStackPointer-1]
    if v == '(' {
      return nil
    }
    opPriority := priorities[v]
    if opPriority < priority {
      return nil
    }
    if p.outputPointer >= p.stackSize {
      return errors.New("output stack overflow")
    }
    p.opStackPointer--
    p.output[p.outputPointer] = outputItem{p.opStack[p.opStackPointer], 0}
    p.outputPointer++
  }
  return nil
}

func (p *parser) operation(b int32) error {
  err := p.storeNumber()
  if err != nil {
    return err
  }
  err = p.moveToOutput(priorities[b])
  if err != nil {
    return err
  }
  if p.opStackPointer >= p.stackSize {
    return errors.New("operation stack overflow")
  }
  p.opStack[p.opStackPointer] = b
  p.opStackPointer++
  p.prevOp = true
  return nil
}

func (p *parser) parse(b int32) error {
  switch b {
  case '+':
    if p.prevOp {
      b = -b
    }
    return p.operation(b)
  case '-':
    if p.prevOp {
      b = -b
    }
    return p.operation(b)
  case '*':
    if p.prevOp {
      return errors.New("invalid statement")
    }
    return p.operation(b)
  case '/':
    if p.prevOp {
      return errors.New("invalid statement")
    }
    return p.operation(b)
  case ' ':
    return p.storeNumber()
  case '(':
    err := p.storeNumber()
    if err != nil {
      return err
    }
    if p.opStackPointer >= p.stackSize {
      return errors.New("operation stack overflow")
    }
    p.opStack[p.opStackPointer] = b
    p.opStackPointer++
    p.prevOp = true
  case ')':
    if p.prevOp {
      return errors.New("invalid statement")
    }
    err := p.storeNumber()
    if err != nil {
      return err
    }
    err = p.moveToOutput(0)
    if err != nil {
      return err
    }
    if p.opStackPointer == 0 {
      return errors.New("( is missing")
    }
    p.opStackPointer--
  case '0':
    p.parseNumber(0)
  case '1':
    p.parseNumber(1)
  case '2':
    p.parseNumber(2)
  case '3':
    p.parseNumber(3)
  case '4':
    p.parseNumber(4)
  case '5':
    p.parseNumber(5)
  case '6':
    p.parseNumber(6)
  case '7':
    p.parseNumber(7)
  case '8':
    p.parseNumber(8)
  case '9':
    p.parseNumber(9)
  case '.':
    if p.koef < 1.0 {
      return errors.New("unexpected comma")
    }
    p.koef = 0.1
    p.prevOp = false
  default:
    return fmt.Errorf("unexpected character: %c", b)
  }

  return nil
}

func (p *parser) init(stackSize int) {
  p.output = make([]outputItem, stackSize)
  p.opStack = make([]int32, stackSize)
  p.dataStack = make([]float64, stackSize)
  p.koef = 1.0
  p.stackSize = stackSize
  p.prevOp = true
}

func (p *parser) getOperand() (float64, error) {
  if p.opStackPointer < 2 {
    return 0, errors.New("invalid statement")
  }
  p.opStackPointer--
  return p.dataStack[p.opStackPointer], nil
}

func (p *parser) finish() (float64, error) {
  err := p.storeNumber()
  if err != nil {
    return 0, err
  }
  err = p.moveToOutput(0)
  if err != nil {
    return 0, err
  }
  if p.opStackPointer > 0 {
    return 0, errors.New(") is missing")
  }
  if p.outputPointer == 0 {
    return 0, errors.New("empty statement")
  }
  for i := 0; i < p.outputPointer; i++ {
    data := p.output[i]
    switch data.op {
    case 0:
      if p.opStackPointer >= p.stackSize {
        return 0, errors.New("data stack overflow")
      }
      p.dataStack[p.opStackPointer] = data.value
      p.opStackPointer++
    case -'+':
      if p.opStackPointer < 1 {
        return 0, errors.New("invalid statement")
      }
    case -'-':
      if p.opStackPointer < 1 {
        return 0, errors.New("invalid statement")
      }
      p.dataStack[p.opStackPointer-1] = -p.dataStack[p.opStackPointer-1]
    case '+':
      v, err := p.getOperand()
      if err != nil {
        return 0, err
      }
      p.dataStack[p.opStackPointer-1] += v
    case '-':
      v, err := p.getOperand()
      if err != nil {
        return 0, err
      }
      p.dataStack[p.opStackPointer-1] -= v
    case '*':
      v, err := p.getOperand()
      if err != nil {
        return 0, err
      }
      p.dataStack[p.opStackPointer-1] *= v
    case '/':
      v, err := p.getOperand()
      if err != nil {
        return 0, err
      }
      if v == 0 {
        return 0, errors.New("division by zero")
      }
      p.dataStack[p.opStackPointer-1] /= v
    }
  }

  if p.opStackPointer != 1 {
    return 0, errors.New("invalid statement")
  }

  return p.dataStack[0], nil
}

func Eval(value string, stackSize int) (float64, error) {
  var p parser

  p.init(stackSize)

  for _, b := range value {
    err := p.parse(b)
    if err != nil {
      return 0, err
    }
  }

  return p.finish()
}
