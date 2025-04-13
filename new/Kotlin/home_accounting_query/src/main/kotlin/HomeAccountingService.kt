package com.sz.home_accounting.query

import com.sz.file_server.lib.FileService
import com.sz.file_server.lib.GetLastResponse
import com.sz.file_server.lib.GetResponse
import com.sz.file_server.lib.KeyValue
import com.sz.home_accounting.query.entities.Dicts
import com.sz.home_accounting.query.entities.FinanceRecord
import com.sz.smart_home.common.NetworkService.Callback
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import javax.crypto.Cipher
import javax.crypto.spec.ChaCha20ParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import java.nio.ByteBuffer
import java.util.*

class HomeAccountingService(private val fileService: FileService, keyBytes: ByteArray) {
    private val key = SecretKeySpec(keyBytes, 0, keyBytes.size, "ChaCha20")
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
        val values = records.map { KeyValue(it.key, it.value.toBinary()) }
        fileService.set(dbVersion, values, callback)
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
        val cipher = Cipher.getInstance("ChaCha20")
        val iv = Random.nextBytes(12)
        val param = ChaCha20ParameterSpec(iv, 0)
        cipher.init(Cipher.ENCRYPT_MODE, key, param)
        val encrypted = cipher.doFinal(data)
        val buffer = ByteBuffer.allocate(encrypted.size + iv.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("ChaCha20")
        val iv = data.copyOfRange(0, 12)
        val param = ChaCha20ParameterSpec(iv, 0)
        cipher.init(Cipher.DECRYPT_MODE, key, param)
        return cipher.doFinal(data.copyOfRange(12, data.size))
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