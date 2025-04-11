package com.sz.home_accounting.query.entities

import java.nio.ByteBuffer

enum class SubcategoryCode {
    Comb,
    Comc,
    Fuel,
    Prcn,
    Incc,
    Expc,
    Exch,
    Trfr,
    None
}

enum class SubcategoryOperationCode {
    Incm,
    Expn,
    Spcl
}

data class Subcategory(val id: Int,
                       val name: String,
                       val code: SubcategoryCode,
                       val operationCode: SubcategoryOperationCode,
                       val category: Int) {
    companion object {
        fun fromBinary(buffer: ByteBuffer): Map<Int, Subcategory> {
            var size = buffer.getShort()
            val result = mutableMapOf<Int, Subcategory>()
            while (size-- > 0) {
                val id = buffer.getShort().toInt()
                val subcategory = create(id, buffer)
                result[id] = subcategory
            }
            return result
        }

        private fun create(id: Int, buffer: ByteBuffer): Subcategory {
            val size = buffer.get().toInt()
            val array = ByteArray(size)
            buffer.get(array)
            val name = String(array)
            val code = SubcategoryCode.entries[buffer.get().toInt()]
            val operationCode = SubcategoryOperationCode.entries[buffer.get().toInt()]
            val category = buffer.getShort().toInt()
            return Subcategory(id, name, code, operationCode, category)
        }

        fun toBinary(subcategories: Map<Int, Subcategory>, buffer: ByteBuffer) {
            buffer.putShort(subcategories.size.toShort())
            for ((id,subcategory) in subcategories) {
                buffer.putShort(id.toShort())
                subcategory.toBinary(buffer)
            }
        }
    }

    private fun toBinary(buffer: ByteBuffer) {
        val bytes = name.toByteArray()
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
        buffer.put(code.ordinal.toByte())
        buffer.put(operationCode.ordinal.toByte())
        buffer.putShort(category.toShort())
    }
}
