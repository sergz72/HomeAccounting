package accounting.home.homeaccounting.entities

import com.google.gson.annotations.SerializedName

import java.math.BigDecimal
import java.time.LocalDate

data class FinOpProperty(@SerializedName("NumericValue") val numericValue: BigDecimal?,
                         @SerializedName("StringValue") val stringValue: String?,
                         @SerializedName("DateValue") val dateValue: LocalDate?,
                         @SerializedName("PropertyCode") val propertyCode: String) {
    constructor(propertyCode: String, numericValue: BigDecimal) :  this(numericValue = numericValue, stringValue = null, dateValue = null, propertyCode = propertyCode)
    constructor(propertyCode: String, stringValue: String) :  this(numericValue = null, stringValue = stringValue, dateValue = null, propertyCode = propertyCode)
}
