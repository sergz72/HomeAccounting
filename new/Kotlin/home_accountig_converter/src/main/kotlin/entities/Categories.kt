package com.sz.home_accounting.converter.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.inputStream

@Serializable
data class Category(val id: Int,
                       val name: String) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(filePath: Path): Map<Int, String> {
            filePath.inputStream().use { stream ->
                val subcategories = Json.decodeFromStream<Array<Category>>(stream)
                return subcategories.associate { it.id to it.name }
            }
        }

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
