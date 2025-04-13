package com.sz.home_accounting.query

import com.sz.file_server.lib.FileService
import com.sz.home_accounting.query.entities.Dicts
import com.sz.home_accounting.query.entities.FinOpProperty
import com.sz.home_accounting.query.entities.FinanceOperation
import com.sz.home_accounting.query.entities.FinanceRecord
import com.sz.smart_home.common.NetworkService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class DefaultCallback(private val channel: Channel<Unit>, private val db: DB): NetworkService.Callback<FinanceRecord> {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onResponse(response: FinanceRecord) {
        println(db.date)
        printChanges(response, db.dicts!!)
        GlobalScope.launch { channel.send(Unit) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onFailure(t: Throwable) {
        println(t.message)
        GlobalScope.launch { channel.send(Unit) }
    }
}

fun usage() {
    println("Usage: java -jar home_accounting_query.jar serverKeyFileName keyFileName hostName port db_name")
}

fun help() {
    println("Commands:\nexit\ntoday\ndate YYYYMMDD\nadd subcategory[/category] account summa")
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun main(args: Array<String>) {
    if (args.size != 5) {
        usage()
        return
    }
    val serverKeyBytes = Files.readAllBytes(Paths.get(args[0]))
    val keyBytes = Files.readAllBytes(Paths.get(args[1]))
    val hostName = args[2]
    val port = args[3].toInt()
    val dbName = args[4]

    val fileService = FileService(serverKeyBytes, hostName, port, dbName)
    val service = HomeAccountingService(fileService, keyBytes)

    val channel = Channel<Unit>()

    val db = DB(service)

    do {
        db.init(DefaultCallback(channel, db))
        channel.receive()
    } while (db.dicts == null)

    while (true) {
        print(">")
        val command = readlnOrNull() ?: break
        val trimmed = command.trim()
        if (trimmed.isNotEmpty()) {
            val parts = command.split(" ")
            try {
                when (parts[0]) {
                    "today" -> {
                        if (parts.size != 1) {
                            help()
                        } else {
                            showFinanceRecord(db, channel, DB.now)
                            channel.receive()
                        }
                    }
                    "date" -> {
                        if (parts.size != 2 || parts[1].length != 8) {
                            help()
                        } else {
                            val date = parts[1].toInt()
                            showFinanceRecord(db, channel, date)
                            channel.receive()
                        }
                    }
                    "add" -> {
                        if (parts.size != 1) {
                            help()
                        } else {
                            add(db, channel)
                            channel.receive()
                        }
                    }
                    "exit" -> return
                    else -> help()
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}

fun showFinanceRecord(db: DB, channel: Channel<Unit>, date: Int) {
    db.getFinanceRecord(date, DefaultCallback(channel, db))
}

fun add(db: DB, channel: Channel<Unit>) {
    print("Account: ")
    val accountId = readlnOrNull() ?: return
    print("Subcategory: ")
    val subcategory = readlnOrNull() ?: return
    print("Amount: ")
    val amount = readlnOrNull()?.toLongOrNull()
    if (amount == null || amount < 0) {
        println("Invalid amount")
        return
    }
    print("Summa: ")
    val summa = readlnOrNull()?.toLongOrNull()
    if (summa == null || summa <= 0L) {
        println("Invalid summa")
        return
    }
    print("Properties: ")
    val properties = parseProperties(readlnOrNull()) ?: return
    TODO()
    //val op = FinanceOperation(if (amount == 0L) {null} else {amount}, summa, subcategory, account)
    //db.add(op, DefaultCallback(channel, db))
}

fun parseProperties(value: String?): List<FinOpProperty>? {
    return null
}

fun printChanges(data: FinanceRecord, dicts: Dicts) {
    val changes = data.buildChanges(dicts)
    for (change in changes.changes)
    {
        val accountName = dicts.accounts.getValue(change.key).name
        println("$accountName ${change.value.summa} ${change.value.income} ${change.value.expenditure} ${change.value.getEndSumma()}")
    }
    println("Operations")
    var id = 1
    for (op in data.operations) {
        val accountName = dicts.accounts.getValue(op.account).name
        val subcategory = dicts.subcategories.getValue(op.subcategory)
        val category = dicts.categories.getValue(subcategory.category)
        println("$id $accountName $category ${subcategory.name} ${op.amount} ${op.summa}")
        id++
    }
}
