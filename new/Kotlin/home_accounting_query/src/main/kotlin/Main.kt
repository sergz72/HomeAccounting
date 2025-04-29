package com.sz.home_accounting.query

import com.sz.file_server.lib.FileService
import com.sz.file_server.lib.FileServiceConfig
import com.sz.home_accounting.core.DB
import com.sz.home_accounting.core.HomeAccountingService
import com.sz.home_accounting.core.entities.*
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
    println("Usage: java -jar home_accounting_query.jar userId serverKeyFileName keyFileName hostName port db_name")
}

fun help() {
    println("Commands:\nexit\ntoday\ndate YYYYMMDD\nadd\ndelete id\nupdate_summa id new_summa")
}

suspend fun main(args: Array<String>) {
    if (args.size != 6) {
        usage()
        return
    }
    val userId = args[0].toInt()
    val serverKeyBytes = Files.readAllBytes(Paths.get(args[1]))
    val keyBytes = Files.readAllBytes(Paths.get(args[2]))
    val hostName = args[3]
    val port = args[4].toInt()
    val dbName = args[5]

    val config = FileServiceConfig(
        userId = userId,
        key = serverKeyBytes,
        hostName = hostName,
        port = port,
        dbName = dbName,
        timeoutMs = 1000
    )
    val fileService = FileService(config)
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
                    "delete" -> {
                        if (parts.size != 2) {
                            help()
                        } else {
                            val id = parts[1].toInt()
                            delete(db, channel, id)
                            channel.receive()
                        }
                    }
                    "update_summa" -> {
                        if (parts.size != 3) {
                            help()
                        } else {
                            val id = parts[1].toInt()
                            val newSumma = parts[2].toLong()
                            update(db, channel, id, newSumma)
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

fun update(db: DB, channel: Channel<Unit>, id: Int, newSumma: Long) {
    val op = db.data!!.operations[id]
    val newOp = FinanceOperation(op.amount, newSumma, op.subcategory, op.account, op.properties)
    db.update(id, newOp, DefaultCallback(channel, db))
}

fun delete(db: DB, channel: Channel<Unit>, id: Int) {
    db.delete(id, DefaultCallback(channel, db))
}

fun add(db: DB, channel: Channel<Unit>) {
    val accounts = db.buildAccounts(db.date).map { it.value.name to it.key }.toMap()
    val accountId = autocomplete("Account: ", accounts) ?: return
    val subcategoryId = autocomplete("Subcategory: ", db.buildSubcategories()) ?: return
    print("Amount: ")
    val amountString = readlnOrNull() ?: return
    val amount = if (amountString.isEmpty()) {0L} else {amountString.toLongOrNull() ?: return}
    if (amount < 0L) {
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
    val properties = buildProperties(accounts) ?: return
    val op = FinanceOperation(if (amount == 0L) {null} else {amount}, summa, subcategoryId, accountId, properties)
    db.add(op, DefaultCallback(channel, db))
}

fun autocomplete(prompt: String, values: Map<String, Int>): Int? {
    var variants: List<Pair<String, Int>>
    do {
        print(prompt)
        val value = readlnOrNull() ?: return null
        variants = values.filter { it.key.contains(value, true) }.toList()
    } while (variants.isEmpty())
    var idx = 1
    for (variant in variants) {
        println("$idx ${variant.first}")
        idx++
    }
    val index = readlnOrNull()?.toIntOrNull() ?: return null
    return variants[index-1].second
}

fun buildProperties(accounts: Map<String, Int>): List<FinOpProperty>? {
    val result = mutableListOf<FinOpProperty>()
    while (true) {
        print("Key: ")
        val key = readlnOrNull() ?: return null
        if (key.isEmpty())
            return result
        when (key) {
            "SECA" -> {
                val accountId = autocomplete("Account: ", accounts) ?: return null
                result.add(FinOpProperty(accountId.toLong(), null, null, FinOpPropertyCode.SECA))
            }
            "DIST" -> {
                val dist = readlnOrNull()?.toLongOrNull() ?: return null
                if (dist <= 0L) {return null}
                result.add(FinOpProperty(dist, null, null, FinOpPropertyCode.DIST))
            }
            "NETW" -> {
                val network = readlnOrNull() ?: return null
                if (network.isEmpty()) {return null}
                result.add(FinOpProperty(null, network, null, FinOpPropertyCode.NETW))
            }
            "TYPE" -> {
                val type = readlnOrNull() ?: return null
                if (type.isEmpty()) {return null}
                result.add(FinOpProperty(null, type, null, FinOpPropertyCode.TYPE))
            }
        }
    }
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
