package core

import (
	"HomeAccountingDB/src/core/entities"
	"bytes"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"
)

type Server struct {
	db *DB
	key []byte
	conn *net.UDPConn
	mutex sync.RWMutex
}

var server Server

func ServerStart(d *DB, portNumber int) error {
	key, err := ioutil.ReadFile("key.dat")
	if err != nil {
		return err
	}

  addr := net.UDPAddr{ nil, portNumber, "" }
	conn, err := net.ListenUDP("udp", &addr)
	if err != nil {
		return err
	}
	defer conn.Close()

	err = conn.SetWriteBuffer(65507)
	if err != nil {
		return err
	}

	server = Server{d, key, conn, sync.RWMutex{}}

	buffer := make([]byte, 10000)
	for {
		var n int
		var addr net.Addr
		n, addr, err = conn.ReadFrom(buffer)
		if err != nil {
			return err
		}
		go handle(addr, buffer[:n])
	}
}

func logError(errorMessage string) {
	log.Println(errorMessage)
}

func logRequest(addr net.Addr) {
	log.Printf("Incoming request from address %s\n", addr.String())
}

func logRequestBody(requestBody string) {
	log.Printf("Request body: %s\n", requestBody)
}

func handle(addr net.Addr, data []byte) {
	logRequest(addr)
	decodedData, err := AesDecode(data, server.key, false, func(nonce []byte) error {
		timePart := binary.LittleEndian.Uint64(nonce[len(nonce)-8:]) >> 16
		millis := uint64(time.Now().UnixNano() / 1000000)
		if (timePart >= millis && (timePart - millis < 60000)) ||
			 (timePart < millis && (millis - timePart < 60000)) { // 60 seconds
			return nil
		}
		return fmt.Errorf("incorrect nonce")
	})
	if err != nil {
		logError(err.Error())
		return
	}
	var writer bytes.Buffer
	command := string(decodedData)
	logRequestBody(command)
	var errorMessage string
	if strings.HasPrefix(command, "GET ") {
		command = command[4:]
		if command == "/dicts" {
			dictsHandler(&writer)
		} else if strings.HasPrefix(command, "/operations/") {
			operationsHandler(&writer, command[12:])
		} else if strings.HasPrefix(command, "/reports?") {
			reportsHandler(&writer, command[9:])
		} else {
			errorMessage = "Invalid GET operation"
		}
	} else if strings.HasPrefix(command, "PUT ") {
		command = command[4:]
		if strings.HasPrefix(command,  "/operations ") {
			modifyOperation(&writer, command[11:])
		} else {
			errorMessage = "Invalid PUT operation"
		}
	} else if strings.HasPrefix(command, "POST ") {
		command = command[5:]
		if strings.HasPrefix(command,  "/operations ") {
			addOperation(&writer, command[11:])
		} else {
			errorMessage = "Invalid POST operation"
		}
	} else if strings.HasPrefix(command, "DELETE ") {
		command = command[7:]
		if strings.HasPrefix(command,  "/operations ") {
			deleteOperation(&writer, command[11:])
		} else {
			errorMessage = "Invalid DELETE operation"
		}
	} else {
		errorMessage = fmt.Sprintf("Invalid method string: %v", command[:7])
	}
	if len(errorMessage) > 0 {
		logError(errorMessage)
		writer.Write([]byte(errorMessage))
	}
	encodedData, err := AesEncode(writer.Bytes(), server.key, true, nil)
	if err != nil {
		logError(err.Error())
		_, _ = server.conn.WriteTo([]byte(err.Error()), addr)
	} else {
		_, _ = server.conn.WriteTo(encodedData, addr)
	}
}

func writeResult(w *bytes.Buffer, prefix string, suffix *string, data interface{}, err error, title string, message *strings.Builder) bool {
	if err != nil {
		w.Write([]byte(fmt.Sprintf("500 get %v error: %v", title, err.Error())))
		return false
	}
	_, err = message.WriteString(prefix)
	if err != nil {
		w.Write([]byte(fmt.Sprintf("500 %v strings.Builder error1: %v", title, err.Error())))
		return false
	}
	var result []byte
	result, err = json.Marshal(data)
	if err != nil {
		w.Write([]byte(fmt.Sprintf("500 json.Marshal(%v) error: %v", title, err.Error())))
		return true
	}
	_, err = message.Write(result)
	if err != nil {
		w.Write([]byte(fmt.Sprintf("500 %v strings.Builder error2: %v", title, err.Error())))
		return false
	}

	if suffix != nil {
		_, err = message.WriteString(*suffix)
		if err != nil {
			w.Write([]byte(fmt.Sprintf("500 %v strings.Builder error3: %v", title, err.Error())))
			return false
		}
	}

	return true
}

func dictsHandler(w *bytes.Buffer) {
	server.mutex.RLock()
	defer server.mutex.RUnlock()

	var message strings.Builder
	if !writeResult(w, "{\"accounts\":", nil, server.db.AccountsArray, nil, "accounts", &message) {
		return
	}
	if !writeResult(w, ",\"categories\":", nil, server.db.CategoriesArray, nil, "categories", &message) {
		return
	}
	if !writeResult(w, ",\"subcategories\":", nil, server.db.SubcategoriesArray, nil, "subcategories", &message) {
		return
	}
	if !writeResult(w, ",\"subcategory_to_property_code_map\":", nil, server.db.SubcategoryToPropertyCodeMap,
		        nil, "subcategory_to_property_code_map", &message) {
		return
	}
	var hintMap map[string][]string
	hintMap = server.db.GetHints()
	suffix := "}"
	if !writeResult(w, ",\"hints\":", &suffix, hintMap, nil, "hints", &message) {
		return
	}
	w.Write([]byte(message.String()))
}

func operationsHandler(w *bytes.Buffer, req string) {
	server.mutex.RLock()
	getOperations(w, req)
	server.mutex.RUnlock()
}

func reportsHandler(w *bytes.Buffer, req string) {
	server.mutex.RLock()
	buildReport(w, req)
	server.mutex.RUnlock()
}

func getPrihodRashod(date1 int, date2 int, category string, subcategory string, account string) ([]entities.PrihodRashodByDay, error) {
	categoryId := 0
	if category != "All" {
		var err error
		categoryId, err = strconv.Atoi(category)
		if err != nil {
			return nil, err
		}
	}
	subcategoryId := 0
	if subcategory != "All" {
		var err error
		subcategoryId, err = strconv.Atoi(subcategory)
		if err != nil {
			return nil, err
		}
	}
	accountId := 0
	if account != "All" {
		var err error
		accountId, err = strconv.Atoi(account)
		if err != nil {
			return nil, err
		}
	}
	return server.db.GetPrihodRashod(date1, date2, categoryId, subcategoryId, accountId)
}

func buildReport(w *bytes.Buffer, req string) {
	values, err := url.ParseQuery(req)
	if err != nil {
		w.Write([]byte("400 Bad request: query parsing error"))
		return
	}
	date1, err := strconv.Atoi(values.Get("date1"))
	if err != nil {
		w.Write([]byte("400 Bad request: invalid date1"))
		return
	}
	date2, err := strconv.Atoi(values.Get("date2"))
	if err != nil {
		w.Write([]byte("400 Bad request: invalid date2"))
		return
	}

	typ := values.Get("type")
	if typ == "gas" {
		ops := server.db.GetGasOperations(date1, date2)
		rep, errr := BuildGasReportFromOperations(ops)
		if errr != nil {
			w.Write([]byte("500 BuildGasReportFromOperations error: " + errr.Error()))
			return
		}
		result, errm := json.Marshal(rep)
		if errm != nil {
			w.Write([]byte("500 json.Marshal(rep) error: " + errm.Error()))
			return
		}
		w.Write(result)
		return
	}

	grouping := values.Get("groupBy")
	if grouping == "" {
		w.Write([]byte("400 Bad request: missing groupBy"))
		return
	}
	category := values.Get("category")
	if category == "" {
		w.Write([]byte("400 Bad request: missing category"))
		return
	}
	subcategory := values.Get("subcategory")
	if category == "" {
		w.Write([]byte("400 Bad request: missing subcategory"))
		return
	}
	account := values.Get("account")
	if category == "" {
		w.Write([]byte("400 Bad request: missing account"))
		return
	}
	prr, errp := getPrihodRashod(date1, date2, category, subcategory, account)
	if errp != nil {
		w.Write([]byte("500 getPrihodRashod error: " + errp.Error()))
		return
	}
	rep, errr := BuildReportFromOperations(prr, grouping)
	if errr != nil {
		w.Write([]byte("500 BuildReportFromOperations error: " + errr.Error()))
		return
	}
	result, errm := json.Marshal(rep)
	if errm != nil {
		w.Write([]byte("500 json.Marshal(rep) error: " + errm.Error()))
		return
	}
	w.Write(result)
}

func addOperation(w *bytes.Buffer, req string) {
	server.mutex.Lock()
	defer server.mutex.Unlock()

	var opAdd entities.FinanceOperationAdd

	fmt.Printf("addOperation, body = %v\n", req)
	if err := json.Unmarshal([]byte(req), &opAdd); err != nil {
		fmt.Printf("addOperation json.Unmarshal error: %v\n", err.Error())
		w.Write([]byte("400 Bad request: " + err.Error()))
		return
	}

	err := server.db.AddOperaton(&opAdd)
	if err != nil {
		fmt.Printf("addOperation error: %v\n", err.Error())
		w.Write([]byte("400 AddOperation error: " + err.Error()))
		return
	}

	w.Write([]byte("OK"))
}

func modifyOperation(w *bytes.Buffer, req string) {
	server.mutex.Lock()
	defer server.mutex.Unlock()

	var opModify entities.FinanceOperationModify

	fmt.Printf("modifyOperation, body = %v\n", req)
	if err := json.Unmarshal([]byte(req), &opModify); err != nil {
		fmt.Printf("modifyOperation json.Unmarshal error: %v\n", err.Error())
		w.Write([]byte("400 Bad request: " + err.Error()))
		return
	}

	err := server.db.ModifyOperaton(&opModify)
	if err != nil {
		fmt.Printf("modifyOperation error: %v\n", err.Error())
		w.Write([]byte("400 ModifyOperation error: " + err.Error()))
		return
	}

	w.Write([]byte("OK"))
}

func deleteOperation(w *bytes.Buffer, req string) {
	server.mutex.Lock()
	defer server.mutex.Unlock()

	var opDelete entities.FinanceOperationDelete

	fmt.Printf("deleteOperation, body = %v\n", req)
	if err := json.Unmarshal([]byte(req), &opDelete); err != nil {
		fmt.Printf("deleteOperation error: %v\n", err.Error())
		w.Write([]byte("400 Bad request: " + err.Error()))
		return
	}

	err := server.db.DeleteOperaton(&opDelete)
	if err != nil {
		fmt.Printf("deleteOperation error: %v", err.Error())
		w.Write([]byte("400 DeleteOperation error: " + err.Error()))
		return
	}

	w.Write([]byte("OK"))
}

func getOperations(w *bytes.Buffer, req string) {
	date, err := strconv.Atoi(req)
	if err != nil {
		w.Write([]byte("400 Bad request: invalid date"))
		return
	}
	ops := server.db.GetFinanceOperationsAndTotals(date)
	result, err := json.Marshal(ops)
	if err != nil {
		w.Write([]byte("500 json.Marshal(ops) error: " + err.Error()))
		return
	}
	w.Write(result)
}
