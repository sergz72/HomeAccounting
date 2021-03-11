package accounting.home.homeaccounting

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.util.Arrays

import accounting.home.homeaccounting.entities.LocalDateAdapter

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull

@RunWith(Parameterized::class)
class LocalDateAdapterTest {
    @Parameterized.Parameter(value = 0)
    var mTestDate: LocalDate? = null

    @Parameterized.Parameter(value = 1)
    var mJsonString: String? = null

    @Test
    @Throws(IOException::class)
    fun localDateAdapterTest() {
        val adapter = LocalDateAdapter()
        var json: String? = null
        StringWriter().use { out ->
            JsonWriter(out).use { w ->
                adapter.write(w, mTestDate)
                json = out.toString()
            }
        }
        StringReader(json!!).use { `in` ->
            JsonReader(`in`).use { r ->
                val result = adapter.read(r)
                if (mTestDate == null)
                    assertNull(result)
                else
                    assertEquals(mTestDate, result)
            }
        }
        StringReader(mJsonString!!).use { `in` ->
            JsonReader(`in`).use { r ->
                val result = adapter.read(r)
                if (mTestDate == null)
                    assertNull(result)
                else
                    assertEquals(mTestDate, result)
            }
        }
    }

    companion object {

        @Parameterized.Parameters
        fun initParameters(): Collection<Array<Any>> {
            return Arrays.asList(*arrayOf(arrayOf<Any>(null, "null"), arrayOf(LocalDate.of(2018, 1, 2), "[2018,1,2]")))
        }
    }
}