package com.sz.home_accounting.converter.entities

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Dicts(val accounts: Map<Int, Account>,
                 val categories: Map<Int, String>,
                 val subcategories: Map<Int, Subcategory>) {
    companion object {
        fun fromBinary(buffer: ByteBuffer): Dicts {
            val accountMap = Account.fromBinary(buffer)
            val categoryMap = Category.fromBinary(buffer)
            val subcategoryMap = Subcategory.fromBinary(buffer)
            return Dicts(accountMap, categoryMap, subcategoryMap)
        }
    }

    fun toBinary(): ByteArray {
        val buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        Account.toBinary(accounts, buffer)
        Category.toBinary(categories, buffer)
        Subcategory.toBinary(subcategories, buffer)
        return buffer.array().copyOfRange(0, buffer.position())
    }
}
