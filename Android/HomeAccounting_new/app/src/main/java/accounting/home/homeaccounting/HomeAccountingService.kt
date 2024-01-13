package accounting.home.homeaccounting

import android.os.NetworkOnMainThreadException
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HomeAccountingService<T> {
    companion object {
        private val executor: Executor = Executors.newSingleThreadExecutor()
        private lateinit var mAddress: InetAddress
        private var mPort: Int = 0

        fun setupKey(keyStream: InputStream) {
            Aes.setKey(keyStream.readBytes())
        }

        fun setupServer(serverAddress: String, port: Int) {
            mAddress = try {
                InetAddress.getByName(serverAddress)
            } catch (e: NetworkOnMainThreadException) {
                InetAddress.getByName("127.0.0.1")
            }
            mPort = port
        }
    }

    interface Callback<T> {
        fun deserialize(response: String): T
        fun isString(): Boolean
        fun onResponse(response: T, compressed: ByteArray)
        fun onFailure(t: Throwable?, response: String?)
    }

    private var mRequest: String? = null

    fun setRequest(request: String): HomeAccountingService<T> {
        mRequest = request
        return this
    }

    fun doInBackground(callback: Callback<T>) {
        if (mRequest != null) {
            executor.execute {
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
                        @Suppress("UNCHECKED_CAST")
                        callback.onResponse(body.uncompressed as T, body.compressed)
                    } else {
                        if (body.uncompressed.isEmpty() ||
                            (body.uncompressed[0] != '{' && body.uncompressed[0] != '[')) {
                            callback.onFailure(null, body.uncompressed)
                        } else {
                            callback.onResponse(callback.deserialize(body.uncompressed), body.compressed)
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
}