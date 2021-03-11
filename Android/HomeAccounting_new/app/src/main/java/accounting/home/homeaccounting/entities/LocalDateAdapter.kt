package accounting.home.homeaccounting.entities

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import java.io.IOException
import java.time.LocalDate

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginArray()
        out.value(value.year.toLong())
        out.value(value.monthValue.toLong())
        out.value(value.dayOfMonth.toLong())
        out.endArray()
    }

    @Throws(IOException::class)
    override fun read(inValue: JsonReader): LocalDate? {
        val t = inValue.peek()
        if (t == JsonToken.NULL) {
            inValue.skipValue()
            return null
        }
        inValue.beginArray()
        val year = inValue.nextInt()
        val month = inValue.nextInt()
        val day = inValue.nextInt()
        inValue.endArray()
        return LocalDate.of(year, month, day)
    }
}
