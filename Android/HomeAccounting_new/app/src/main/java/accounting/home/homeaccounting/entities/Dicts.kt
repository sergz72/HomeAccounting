package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

data class Dicts(val accounts: List<Account>,
                 val categories: List<IdName>,
                 val subcategories: List<Subcategory>,
                 @SerializedName("subcategory_to_property_code_map") val subcategoryToPropertyCodeMap: Map<String, List<String>>,
                 val hints: Map<String, List<String>>)

