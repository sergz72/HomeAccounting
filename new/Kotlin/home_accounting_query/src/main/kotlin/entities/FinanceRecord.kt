package com.sz.home_accounting.query.entities

import com.sz.home_accounting.query.DBException
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class FinanceRecord(var operations: MutableList<FinanceOperation>) {
    companion object {
        fun fromBinary(data: ByteArray): FinanceRecord {
            TODO()
        }

        fun create(): FinanceRecord {
            return FinanceRecord(mutableListOf())
        }
    }

    var totals = mapOf<Int, Long>()

    fun updateChanges(changes: FinanceChanges, dicts: Dicts) {
        operations.forEach { it.updateChanges(changes, dicts) }
    }

    fun buildChanges(dicts: Dicts): FinanceChanges {
        val changes = FinanceChanges(totals)
        operations.forEach {  it.updateChanges(changes, dicts) }
        return changes
    }

    fun toBinary(): ByteArray {
        val buffer = ByteBuffer.allocate(10 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(totals.size.toShort())
        for ((accountId, value) in totals) {
            buffer.putShort(accountId.toShort())
            buffer.putLong(value)
        }
        buffer.putShort(operations.size.toShort())
        for (operation in operations) {
            operation.toBinary(buffer)
        }
        return buffer.array().copyOfRange(0, buffer.position());
    }
}

data class FinanceOperation(
    val amount: Long?,
    val summa: Long,
    val subcategory: Int,
    val account: Int,
    val properties: List<FinOpProperty>?
) {
    fun updateChanges(changes: FinanceChanges, dicts: Dicts) {
        val subcategory = dicts.subcategories.getValue(subcategory)
        when (subcategory.operationCode) {
            SubcategoryOperationCode.Incm -> changes.income(account, summa)
            SubcategoryOperationCode.Expn -> changes.expenditure(account, summa)
            SubcategoryOperationCode.Spcl -> {
                when (subcategory.code) {
                    // Пополнение карточного счета наличными
                    SubcategoryCode.Incc -> handleIncc(changes, dicts.accounts)
                    // Снятие наличных в банкомате
                    SubcategoryCode.Expc -> handleExpc(changes, dicts.accounts)
                    // Обмен валюты
                    SubcategoryCode.Exch -> handleExch(changes)
                    // Перевод средств между платежными картами
                    SubcategoryCode.Trfr -> handleTrfr(changes)
                    else -> throw DBException("unknown subcategory code")
                }
            }
        }
    }

    private fun handleTrfr(changes: FinanceChanges) {
        handleTrfrWithSumma(changes, summa)
    }

    private fun handleExch(changes: FinanceChanges) {
        if (amount != null) {
            handleTrfrWithSumma(changes, amount / 10)
        }
    }

    private fun handleTrfrWithSumma(changes: FinanceChanges, value: Long)
    {
        if (properties == null) return
        changes.expenditure(account, value)
        val secondAccountProperty = properties.find { it.code == FinOpPropertyCode.SECA }
        if (secondAccountProperty?.numericValue != null)
        {
            changes.income(secondAccountProperty.numericValue.toInt(), summa)
        }
    }

    private fun handleExpc(changes: FinanceChanges, accounts: Map<Int, Account>) {
        changes.expenditure(account, summa)
        // cash account for corresponding currency code
        val cashAccount = accounts.getValue(account).cashAccount
        if (cashAccount != null) {
            changes.income(cashAccount, summa)
        }
    }

    private fun handleIncc(changes: FinanceChanges, accounts: Map<Int, Account>) {
        changes.income(account, summa)
        // cash account for corresponding currency code
        val cashAccount = accounts.getValue(account).cashAccount
        if (cashAccount != null) {
            changes.expenditure(cashAccount, summa)
        }
    }

    fun toBinary(buffer: ByteBuffer) {
        buffer.putLong(amount ?: 0)
        buffer.putLong(summa)
        buffer.putShort(subcategory.toShort())
        buffer.putShort(account.toShort())
        if (properties != null) {
            buffer.put(properties.size.toByte())
            for (property in properties) {
                property.toBinary(buffer)
            }
        } else {
            buffer.put(0)
        }
    }
}

enum class FinOpPropertyCode {
    SECA,
    NETW,
    DIST,
    TYPE,
    PPTO,
    AMOU
}

data class FinOpProperty(
    val numericValue: Long?, val stringValue: String?,
    val dateValue: Int?,
    val code: FinOpPropertyCode
) {
    fun toBinary(buffer: ByteBuffer) {
        buffer.put(code.ordinal.toByte())
        when (code) {
            FinOpPropertyCode.SECA, FinOpPropertyCode.DIST, FinOpPropertyCode.PPTO, FinOpPropertyCode.AMOU -> buffer.putLong(numericValue!!)
            else -> {
                val bytes = stringValue!!.toByteArray(Charsets.UTF_8)
                buffer.put(bytes.size.toByte())
                buffer.put(bytes)
            }
        }
    }
}

class FinanceChanges(totals: Map<Int, Long>) {
    val changes: MutableMap<Int, FinanceChange> =
        totals.map { it.key to FinanceChange(it.value, 0, 0) }.toMap().toMutableMap()

    fun buildTotals(): Map<Int, Long> {
        return changes.map { it.key to it.value.getEndSumma() }.toMap()
    }

    fun income(account: Int, value: Long) {
        changes.getOrPut(account) { FinanceChange(0, 0, 0) }.income += value
    }

    fun expenditure(account: Int, value: Long) {
        changes.getOrPut(account) { FinanceChange(0, 0, 0) }.expenditure += value
    }
}

data class FinanceChange(val summa: Long, var income: Long, var expenditure: Long) {
    fun getEndSumma(): Long {
        return summa + income - expenditure
    }
}
