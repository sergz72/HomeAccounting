package com.sz.home_accounting.converter

import com.sz.home_accounting.converter.entities.*
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Paths
import javax.crypto.Cipher
import javax.crypto.spec.ChaCha20ParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.random.Random

fun usage() {
    println("Usage: java -jar home_accounting_converter.jar keyFileName sourceFolder destFolder dicts")
}

fun main(args: Array<String>) {
    if (args.size != 4) {
        usage()
        return
    }

    val keyBytes = Files.readAllBytes(Paths.get(args[0]))
    val sourceFolder = args[1]
    val destFolder = args[2]
    val entityName = args[3]

    val key = SecretKeySpec(keyBytes, 0, keyBytes.size, "ChaCha20")

    println("Reading dicts...")
    val accounts = Account.fromJson(Paths.get(sourceFolder, "accounts.json"))
    val categories = Category.fromJson(Paths.get(sourceFolder, "categories.json"))
    val subcategories = Subcategory.fromJson(Paths.get(sourceFolder, "subcategories.json"))
    val map = SubcategoryToPropertyCodeMap.fromJson(Paths.get(sourceFolder, "subcategory_to_property_code_map.json"))
    val dicts = Dicts(accounts, categories, subcategories, map)

    when (entityName) {
        "dicts" -> convertDicts(key, dicts, destFolder)
        "operations" -> convertOperations(key, dicts, sourceFolder, destFolder)
        else -> usage()
    }
}

fun convertOperations(key: SecretKeySpec, dicts: Dicts, sourceFolder: String, destFolder: String) {
    println("Reading...")
    val operations = FinanceRecord.fromJson(Paths.get(sourceFolder, "dates"))
    println("Calculating totals...")
    val db = DB(dicts, operations)
    db.calculateTotals(0)
    db.printChanges(0)
    println("Converting...")
    for ((date, operation) in operations) {
        save(key, destFolder, date / 10000, date, operation.toBinary())
    }
    println("Writing...")
    println("Validating...")
}

fun save(key: SecretKeySpec, destFolder: String, folderId: Int, fileId: Int, data: ByteArray) {
    val encrypted = encrypt(key, data)
    val folderPath = Paths.get(destFolder, folderId.toString())
    val filePath = Paths.get(destFolder, folderId.toString(), fileId.toString())
    if (!folderPath.exists()) {
        folderPath.createDirectory()
    }
    FileOutputStream(filePath.toFile(), false).use { stream ->
        stream.write(1)
        stream.write(0)
        stream.write(0)
        stream.write(0)
        stream.write(encrypted)
    }
}

fun encrypt(key: SecretKeySpec, data: ByteArray): ByteArray {
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

fun decrypt(key: SecretKeySpec, data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("ChaCha20")
    val iv = data.copyOfRange(0, 12)
    val param = ChaCha20ParameterSpec(iv, 0)
    cipher.init(Cipher.DECRYPT_MODE, key, param)
    val decrypted = cipher.doFinal(data)
    return decrypted.copyOfRange(12, decrypted.size)
}

fun convertDicts(key: SecretKeySpec, dicts: Dicts, destFolder: String) {
    println("Converting...")
    val data = dicts.toBinary()
    println("Compressing...")
    val compressed = compress(data)
    println("Writing...")
    save(key, destFolder, 0, 0, compressed)
    println("Validating...")
    val dicts2 = Dicts.fromBinary(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN))
    compareAccounts(dicts.accounts, dicts2.accounts)
    compareCategories(dicts.categories, dicts2.categories)
    compareSubcategories(dicts.subcategories, dicts2.subcategories)
}

fun compress(data: ByteArray): ByteArray {
    val stream = ByteArrayOutputStream()
    val outStream = BZip2CompressorOutputStream(stream, 9)
    outStream.write(data)
    outStream.close()
    return stream.toByteArray()
}

fun compareAccounts(accounts: Map<Int, Account>, accounts2: Map<Int, Account>) {

}

fun compareCategories(categories: Map<Int, String>, categories2: Map<Int, String>) {

}

fun compareSubcategories(subcategories: Map<Int, Subcategory>, subcategories2: Map<Int, Subcategory>) {

}