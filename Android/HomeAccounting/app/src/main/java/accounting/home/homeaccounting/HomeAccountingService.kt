package accounting.home.homeaccounting

import android.os.AsyncTask
import android.os.NetworkOnMainThreadException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class HomeAccountingService<T>: AsyncTask<HomeAccountingService.Callback<T>, Void, Unit>() {
    companion object {
        private lateinit var mAddress: InetAddress
        private var mPort: Int = 0

        fun setupKey(keyStream: InputStream) {
            Aes.setKey(keyStream.readBytes())
        }

        fun setupServer(serverAddress: String, port: Int) {
            try {
                mAddress = InetAddress.getByName(serverAddress)
            } catch (e: NetworkOnMainThreadException) {
                mAddress = InetAddress.getByName("127.0.0.1")
            }
            mPort = port
        }
    }

    interface Callback<T> {
        fun deserialize(response: String): T
        fun isString(): Boolean
        fun onResponse(response: T)
        fun onFailure(t: Throwable?, response: String?)
    }

    private var mRequest: String? = null

    fun setRequest(request: String): HomeAccountingService<T> {
        mRequest = request
        return this
    }

    override fun doInBackground(vararg params: Callback<T>) {
        if (params.size == 1 && mRequest != null) {
            val callback = params[0]
            val socket = DatagramSocket()
            val bytes = Aes.encode(mRequest!!)
            val packet = DatagramPacket(bytes, bytes.size, mAddress, mPort)
            val receiveData = ByteArray(65507)
            try {
                socket.send(packet)
                val inPacket = DatagramPacket(receiveData, receiveData.size)
                socket.soTimeout = 10000 // 10 seconds
                socket.receive(inPacket)
                val body = Aes.decode(inPacket.data, inPacket.length)
                if (callback.isString()) {
                    callback.onResponse(body as T)
                } else {
                    if (body.isEmpty() || (body[0] != '{' && body[0] != '[')) {
                        callback.onFailure(null, body)
                    } else {
                        callback.onResponse(callback.deserialize(body))
                    }
                }
            } catch (e: Exception) {
                callback.onFailure(e, null)
            } finally {
                socket.close()
            }
        }
    }
}