package com.sz.home_accounting.core

import com.sz.file_server.lib.FileService
import com.sz.file_server.lib.GetLastResponse
import com.sz.file_server.lib.GetResponse
import com.sz.file_server.lib.KeyValue
import com.sz.home_accounting.core.entities.Dicts
import com.sz.home_accounting.core.entities.FinanceRecord
import com.sz.smart_home.common.NetworkService.Callback
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.random.Random
import java.nio.ByteBuffer
import java.util.*

class HomeAccountingService(private val fileService: FileService, private val key: ByteArray) {
    var dbVersion: Int = 0
        private set

    private fun buildFinanceRecord(data: ByteArray): FinanceRecord {
        val decrypted = decrypt(data)
        return FinanceRecord.fromBinary(decrypted)
    }

    fun getFinanceRecord(date: Int, callback: Callback<Pair<Int, FinanceRecord>>) {
        fileService.getLast(1, date, object: Callback<GetLastResponse> {
            override fun onResponse(response: GetLastResponse) {
                try {
                    val record: FinanceRecord = if (response.data != null) {
                        buildFinanceRecord(response.data!!.second.data)
                    } else {
                        FinanceRecord.create()
                    }
                    dbVersion = response.dbVersion
                    callback.onResponse((response.data?.first ?: date) to record)
                } catch (t: Throwable) {
                    callback.onFailure(t)
                }
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    fun getFinanceRecords(from: Int, callback: Callback<SortedMap<Int, FinanceRecord>>) {
        fileService.get(from, 99999999, object: Callback<GetResponse> {
            override fun onResponse(response: GetResponse) {
                try {
                    callback.onResponse(
                        response.data.map { it.key to buildFinanceRecord(it.value.data) }.toMap().toSortedMap()
                    )
                } catch (t: Throwable) {
                    callback.onFailure(t)
                }
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }

        })
    }

    fun set(records: SortedMap<Int, FinanceRecord>, callback: Callback<Unit>) {
        val values = records.map { KeyValue(it.key, buildEncryptedRecord(it.value)) }
        fileService.set(dbVersion, values, callback)
    }

    private fun buildEncryptedRecord(value: FinanceRecord): ByteArray {
        return if (value.operations.isEmpty()) {ByteArray(0)} else {encrypt(value.toBinary())}
    }

    fun getDicts(callback: Callback<Dicts>) {
        fileService.get(0, 0, object: Callback<GetResponse> {
            override fun onResponse(response: GetResponse) {
                try {
                    if (response.data.isEmpty())
                        throw IllegalStateException("Empty response")
                    val decrypted = decrypt(response.data[0]!!.data)
                    val decompressed = decompress(decrypted)
                    val dicts = Dicts.fromBinary(decompressed)
                    dbVersion = response.dbVersion
                    callback.onResponse(dicts)
                } catch (t: Throwable) {
                    callback.onFailure(t)
                }
            }

            override fun onFailure(t: Throwable) {
                callback.onFailure(t)
            }

        })
    }

    fun encrypt(data: ByteArray): ByteArray {
        val iv = Random.nextBytes(12)
        val cipher = ChaCha20(key, iv, 0u)
        val encrypted = cipher.encrypt(data)
        val buffer = ByteBuffer.allocate(encrypted.size + iv.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    private fun decrypt(data: ByteArray): ByteArray {
        if (data.size < 13) {
            throw IOException("Invalid response")
        }
        val iv = data.sliceArray(0..12)
        val cipher = ChaCha20(key, iv, 0u)
        return cipher.encrypt(data.sliceArray(12..<data.size))
    }

    private fun decompress(decrypted: ByteArray): ByteArray {
        val inStream = BZip2CompressorInputStream(ByteArrayInputStream(decrypted))
        inStream.use {
            val result = inStream.readBytes()
            println("Decompressed size = ${result.size}")
            return result
        }
    }
}