package accounting.home.homeaccounting

import accounting.home.homeaccounting.entities.Account
import accounting.home.homeaccounting.entities.Dicts
import accounting.home.homeaccounting.entities.IdName
import accounting.home.homeaccounting.entities.Subcategory
import kotlin.math.abs

class DB(dictsIn: Dicts) {
    private var dicts: Dicts = dictsIn
    private var accountMap: Map<Int, Account> = dictsIn.accounts.map { it.id to it }.toMap()
    var activeAccounts: List<Account> = dictsIn.accounts.filter { account -> account.activeTo == null }.toList()
        private set
    private var categories: Map<Int, String> = dictsIn.categories.map { it.id to it.name }.toMap()
    private var subcategoryMap: Map<Int, Subcategory> = dictsIn.subcategories.map { it.id to it }.toMap()

    fun getCategoryNameBySubcategoryId(subcategoryId: Int): String {
        val category = subcategoryMap.getValue(subcategoryId).categoryId.toInt()
        return categories.getValue(category)
    }

    fun getCategoryNameById(categoryId: Int): String {
        return categories.getValue(categoryId)
    }

    fun getSubcategoryName(subcategoryId: Int): String {
        return subcategoryMap.getValue(subcategoryId).name
    }

    fun getSubcategory(subcategoryId: Int): Subcategory? {
        return subcategoryMap[subcategoryId]
    }

    fun getAccount(accountId: Int): Account? {
        return accountMap[accountId]
    }

    fun formatMoney(value: Int): String {
        return String.format("%d.%02d", value / 100, abs(value % 100))
    }

    fun formatMoney3(value: Int?): String {
        return if (value == null) {
            ""
        } else String.format("%d.%03d", value / 1000, abs(value % 1000))
    }

    fun getHints(code: String): List<String>? {
        return dicts.hints[code]
    }

    fun getSubcategories(): List<Subcategory> {
        return dicts.subcategories
    }

    fun getCategories(): List<IdName> {
        return dicts.categories
    }

    fun getAccounts(): List<Account> {
        return dicts.accounts
    }
}
