package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

import accounting.home.homeaccounting.SharedResources

data class Subcategory(@SerializedName("Id") val id: Int,
                       @SerializedName("Code") val code: String?,
                       @SerializedName("Name") val name: String,
                       @SerializedName("OperationCodeId") val operationCodeId: String,
                       @SerializedName("CategoryId") val categoryId: Short) {
    companion object {
        fun buildSubcategory(id: Int, name: String, categoryId: Short): Subcategory {
            return Subcategory(id = id,
                               name = name,
                               categoryId = categoryId,
                               operationCodeId = "",
                               code = null)
        }
    }
    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is Subcategory) {
            return id == other.id
        }
        return false
    }

    override fun toString(): String {
        return name + " - " + if (categoryId.toInt() == 0) "All" else SharedResources.db!!.getCategoryNameById(categoryId.toInt())
    }
}
