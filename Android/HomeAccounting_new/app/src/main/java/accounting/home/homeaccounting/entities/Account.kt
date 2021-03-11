package accounting.home.homeaccounting.entities

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

import java.time.LocalDate

data class Account(@SerializedName("Id") val id: Int,
                   @SerializedName("Name") val name: String,
                   @SerializedName("IsCash") val isCash: Boolean,
                   @SerializedName("ActiveTo") @JsonAdapter(LocalDateAdapter::class) val activeTo: LocalDate?,
                   @SerializedName("ValutaCode") val valutaCode: String) {
    companion object {
        fun buildAccount(id: Int, name: String): Account {
            return Account(id, name, false, null, "")
        }
    }

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return id == other.id
        }
        return false
    }

    override fun toString(): String {
        return name
    }
}
