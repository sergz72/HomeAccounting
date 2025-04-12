package com.sz.home_accounting.converter.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.inputStream

@Serializable
data class SubcategoryToPropertyCodeMap(
    @Serializable(with = CodeSerializer::class) val subcategoryCode: SubcategoryCode,
    @Serializable(with = PropertyCodeSerializer::class) val propertyCode: FinOpPropertyCode
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(filePath: Path): Map<SubcategoryCode, Array<FinOpPropertyCode>> {
            filePath.inputStream().use { stream ->
                val map = Json.decodeFromStream<Array<SubcategoryToPropertyCodeMap>>(stream)
                return map
                    .groupBy { it.subcategoryCode }
                    .map { it.key to it.value.map { it.propertyCode }.toTypedArray() }
                    .toMap()
            }
        }
    }
}