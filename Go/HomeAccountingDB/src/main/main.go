package main

import (
	"core"
	"database/sql"
	"expreval"
	"fmt"
	_ "github.com/lib/pq"
	"os"
	"strconv"
)

func main() {
	if len(os.Args) != 5 && len(os.Args) != 3 {
		fmt.Println("Usage: main [server user password port_number][calculate expression]")
		return
	}
	if os.Args[1] == "calculate" {
		result, err := expreval.Eval(os.Args[2], core.PARSER_STACK_SIZE)
		if err != nil {
			fmt.Println(err.Error())
			return
		}
		fmt.Printf("Result: %v\n", result)
		return
	}
	portNumber, err := strconv.Atoi(os.Args[4])
	if err != nil || portNumber <= 0 {
		fmt.Println("Incorrect port number")
		return
	}
	dbinfo := fmt.Sprintf("host=%s user=%s password=%s dbname=home_accounting sslmode=disable",
		                     os.Args[1], os.Args[2], os.Args[3])
	pgdb, erro := sql.Open("postgres", dbinfo)
	if erro != nil {
		fmt.Println("Database connection error: " + err.Error())
		return
	}
	defer pgdb.Close()
	db := core.DBDATA{pgdb}
	fmt.Printf("Starting server on port %v\n", portNumber)
	err = core.ServerStart(&db, portNumber)
	if err != nil {
		fmt.Println("ServerStart error: " + err.Error())
	}
}
