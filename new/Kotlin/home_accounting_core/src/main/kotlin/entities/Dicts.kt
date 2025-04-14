package com.sz.home_accounting.core.entities

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Dicts(val accounts: Map<Int, Account>,
                 val categories: Map<Int, String>,
                 val subcategories: Map<Int, Subcategory>,
                 val subcategoryToPropertyCodeMap: Map<SubcategoryCode, Array<FinOpPropertyCode>>) {
    companion object {
        fun fromBinary(data: ByteArray): Dicts {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val accountMap = Account.fromBinary(buffer)
            val categoryMap = Category.fromBinary(buffer)
            val subcategoryMap = Subcategory.fromBinary(buffer)
            val subcategoryToPropertyCodeMap = readSubcategoryToPropertyCodeMap(buffer)
            return Dicts(accountMap, categoryMap, subcategoryMap, subcategoryToPropertyCodeMap)
        }

        private fun readSubcategoryToPropertyCodeMap(buffer: ByteBuffer): Map<SubcategoryCode, Array<FinOpPropertyCode>> {
            val result = mutableMapOf<SubcategoryCode, Array<FinOpPropertyCode>>()
            var size = buffer.get().toInt()
            while (size-- > 0) {
                val subcategoryCode = SubcategoryCode.entries[buffer.get().toInt()]
                var arraySize = buffer.get().toInt()
                val codes = mutableListOf<FinOpPropertyCode>()
                while (arraySize-- > 0) {
                    val propertyCode = FinOpPropertyCode.entries[buffer.get().toInt()]
                    codes.add(propertyCode)
                }
                result[subcategoryCode] = codes.toTypedArray()
            }
            return result
        }
    }

    fun toBinary(): ByteArray {
        val buffer = ByteBuffer.allocate(100 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        Account.toBinary(accounts, buffer)
        Category.toBinary(categories, buffer)
        Subcategory.toBinary(subcategories, buffer)
        saveSubcategoryToPropertyCodeMap(buffer)
        return buffer.array().copyOfRange(0, buffer.position())
    }

    private fun saveSubcategoryToPropertyCodeMap(buffer: ByteBuffer) {
        buffer.put(subcategoryToPropertyCodeMap.size.toByte())
        for ((key, value) in subcategoryToPropertyCodeMap) {
            buffer.put(key.ordinal.toByte())
            buffer.put(value.size.toByte())
            for (code in value) {
                buffer.put(code.ordinal.toByte())
            }
        }
    }
}
