package com.sz.home_accounting.core.entities

import java.nio.ByteBuffer

data class Account(
    val id: Int,
    val name: String,
    val currency: String,
    val activeTo: Int?,
    var cashAccount: Int?
) {
    companion object {
        fun fromBinary(buffer: ByteBuffer): Map<Int, Account> {
            var size = buffer.getShort()
            val result = mutableMapOf<Int, Account>()
            while (size-- > 0) {
                val id = buffer.getShort().toInt()
                val account = create(id, buffer)
                result[id] = account
            }
            return result
        }

        private fun create(id: Int, buffer: ByteBuffer): Account {
            val nameSize = buffer.get().toInt()
            var array = ByteArray(nameSize)
            buffer.get(array)
            val name = String(array)
            array = ByteArray(3)
            buffer.get(array)
            val currency = String(array)
            val activeTo = buffer.getInt()
            val cashAccount = buffer.getShort().toInt()
            return Account(id, name, currency, if (activeTo == 0) { null }  else {activeTo},
                            if (cashAccount == 0) { null } else {cashAccount})
        }

        fun toBinary(accounts: Map<Int, Account>, buffer: ByteBuffer) {
            buffer.putShort(accounts.size.toShort())
            for ((id, account) in accounts) {
                buffer.putShort(id.toShort())
                account.toBinary(buffer)
            }
        }
    }

    private fun toBinary(buffer: ByteBuffer) {
        var bytes = name.toByteArray()
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
        bytes = currency.toByteArray()
        buffer.put(bytes[0])
        buffer.put(bytes[1])
        buffer.put(bytes[2])
        buffer.putInt(activeTo ?: 0)
        buffer.putShort((cashAccount ?: 0).toShort())
    }
}
