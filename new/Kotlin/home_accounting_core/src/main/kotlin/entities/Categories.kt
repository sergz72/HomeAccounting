package com.sz.home_accounting.core.entities

import java.nio.ByteBuffer

data class Category(val id: Int,
                       val name: String) {
    companion object {
        fun fromBinary(buffer: ByteBuffer): Map<Int, String> {
            var size = buffer.getShort()
            val result = mutableMapOf<Int, String>()
            while (size-- > 0) {
                val id = buffer.getShort().toInt()
                val nameLength = buffer.get().toInt()
                val array = ByteArray(nameLength)
                buffer.get(array)
                val name = String(array)
                result[id] = name
            }
            return result
        }

        fun toBinary(categories: Map<Int, String>, buffer: ByteBuffer) {
            buffer.putShort(categories.size.toShort())
            for ((id, name) in categories) {
                buffer.putShort(id.toShort())
                val bytes = name.toByteArray()
                buffer.put(bytes.size.toByte())
                buffer.put(bytes)
            }
        }
    }
}
