package com.sz.home_accounting.query

import com.sz.file_server.lib.FileService
import com.sz.home_accounting.query.entities.FinanceRecord
import com.sz.smart_home.common.NetworkService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

fun usage() {
    println("Usage: java -jar home_accounting_query.jar serverKeyFileName keyFileName hostName port db_name")
}

fun help() {
    println("Commands:\ntoday\ndate YYYYMMDD\nadd subcategory[/category] account summa")
}

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

    do {
        getDicts(service, channel)
        channel.receive()
    } while (!service.initialized)

    val dateNow = LocalDateTime.now()
    val now = dateNow.year * 10000 + dateNow.month.value * 100 + dateNow.dayOfMonth

    showFinanceRecord(service, channel, now)
    channel.receive()

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
                        } else
                            showFinanceRecord(service, channel, now)
                    }

                    "date" -> {
                        if (parts.size != 2 || parts[1].length != 8) {
                            help()
                        } else {
                            showFinanceRecord(service, channel, parts[1].toInt())
                        }
                    }
                    "add" -> {

                    }
                    else -> help()
                }
                channel.receive()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun showFinanceRecord(service: HomeAccountingService, channel: Channel<Unit>, date: Int) {
    service.getFinanceRecord(date, object: NetworkService.Callback<Pair<Int, FinanceRecord>> {
        override fun onResponse(response: Pair<Int, FinanceRecord>) {
            println(response.first)
            println("Totals:")
            for (total in response.second.totals) {
                println("${total.key} ${total.value}")
            }
            println("Operations:")
            var n = 1;
            for (op in response.second.operations) {
                println("$n ${op.account} amount=${op.amount} summa=${op.summa}")
                n++
            }
            GlobalScope.launch { channel.send(Unit) }
        }

        override fun onFailure(t: Throwable) {
            println(t.message)
            GlobalScope.launch { channel.send(Unit) }
        }
    })
}

@OptIn(DelicateCoroutinesApi::class)
fun getDicts(service: HomeAccountingService, channel: Channel<Unit>) {
    service.getDicts(object: NetworkService.Callback<Unit> {
        override fun onResponse(response: Unit) {
            GlobalScope.launch { channel.send(Unit) }
        }

        override fun onFailure(t: Throwable) {
            println(t.message)
            GlobalScope.launch { channel.send(Unit) }
        }
    })
}
