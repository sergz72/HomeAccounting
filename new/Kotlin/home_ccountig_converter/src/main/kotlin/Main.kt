package com.sz.home_accounting.converter

import com.sz.home_accounting.converter.entities.*
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

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

    println("Reading dicts...")
    val accounts = Account.fromJson(Paths.get(sourceFolder, "accounts.json"))
    val categories = Category.fromJson(Paths.get(sourceFolder, "categories.json"))
    val subcategories = Subcategory.fromJson(Paths.get(sourceFolder, "subcategories.json"))
    val dicts = Dicts(accounts, categories, subcategories)

    when (entityName) {
        "dicts" -> convertDicts(dicts, sourceFolder, destFolder)
        "operations" -> convertOperations(dicts, sourceFolder, destFolder)
        else -> usage()
    }
}

fun convertOperations(dicts: Dicts, sourceFolder: String, destFolder: String) {
    println("Reading...")
    val operations = FinanceRecord.fromJson(Paths.get(sourceFolder, "dates"))
    println("Calculating totals...")
    val db = DB(dicts, operations)
    db.calculateTotals(0)
    db.printChanges(0)
    println("Converting...")
    for ((date, operation) in operations) {
        save(destFolder, date / 10000, date, operation.toBinary())
    }
    println("Writing...")
    println("Validating...")
}

fun save(destFolder: String, folderId: Int, fileId: Int, data: ByteArray) {
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
        stream.write(data)
    }
}

fun convertDicts(dicts: Dicts, sourceFolder: String, destFolder: String) {
    println("Converting...")
    val data = dicts.toBinary()
    println("Writing...")
    save(destFolder, 0, 0, data)
    println("Validating...")
    val dicts2 = Dicts.fromBinary(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN))
    compareAccounts(dicts.accounts, dicts2.accounts)
    compareCategories(dicts.categories, dicts2.categories)
    compareSubcategories(dicts.subcategories, dicts2.subcategories)
}

fun compareAccounts(accounts: Map<Int, Account>, accounts2: Map<Int, Account>) {

}

fun compareCategories(categories: Map<Int, String>, categories2: Map<Int, String>) {

}

fun compareSubcategories(subcategories: Map<Int, Subcategory>, subcategories2: Map<Int, Subcategory>) {

}