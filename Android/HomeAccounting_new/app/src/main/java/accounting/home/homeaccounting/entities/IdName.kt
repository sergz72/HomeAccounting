package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

data class IdName(@SerializedName("Id") val id: Int,
                  @SerializedName("Name") val name: String) {

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is IdName) {
            return id == other.id
        }
        return false
    }

    override fun toString(): String {
        return name
    }
}
