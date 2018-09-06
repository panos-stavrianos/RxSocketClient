package moe.codeest.rxsocketclientdemo

import android.util.Log
import androidx.work.*
import gr.osnet.rxsocket.RxSocketClient
import gr.osnet.rxsocket.SocketSubscriber
import gr.osnet.rxsocket.meta.SocketConfig
import gr.osnet.rxsocket.meta.SocketOption
import gr.osnet.rxsocket.meta.ThreadStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.util.concurrent.TimeUnit


/**
 * Created by panos on 8/2/2018.
 */

class Communication : Worker() {

    private var disposables = CompositeDisposable()

    override fun doWork(): Result {
        Log.e(TAG, "doWork")

        disposables.clear()
        Log.e(TAG, "Connecting to $host:$port")

        val mClient = RxSocketClient
                .create(SocketConfig.Builder()
                        .setIp(host)
                        .setPort(port)
                        .setCharset(Charsets.UTF_8)
                        .setThreadStrategy(ThreadStrategy.SYNC)
                        .setTimeout(5, TimeUnit.SECONDS)
                        .build())
                .option(SocketOption.Builder()
                        .setHeartBeat(HEART_BEAT, 15, TimeUnit.SECONDS)
                        .setPreSharedKey(key)//if you pass a key then everything you receive it will decrypted automatically
                        .useCompression(true)
                        .useCheckSum(false)
                        .setHead(HEAD)
                        .setTail(TAIL)
                        .setEncryptionPrefix("ENC^")
                        .usePKCS7(false)
                        .setFirstContact(first)
                        .build())
        mClient.connect()
                .doOnSubscribe { disposables.add(it) }
                .subscribe(
                        object : SocketSubscriber() {
                            override fun onConnected() {
                                Log.e(TAG, "onConnected")

                                //You can use encryption by passing true on the send and sendFile

                                mClient.send("Hey", true, compress = true)//Send String

                                mClient.send("Hello".toByteArray(), true)//Send Bytes

                                //Send File
                                // val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/1.jpg"
                                // mClient.sendFile(path, false)
                            }

                            override fun onDisconnected(timePassed: Long) {
                                Log.e(TAG, "onDisconnected in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec")
                            }

                            override fun onDisconnectedWithError(throwable: Throwable, timePassed: Long) {
                                Log.e(TAG, "onDisconnectedWithError in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec, cause: ${throwable.message}")
                            }

                            override fun onResponse(data: String, timePassed: Long) {
                                Log.e(TAG, "onResponse in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec: $data")
                            }
                        },
                        Consumer {
                            it.printStackTrace()
                        })

        mClient.waitUntilEnd()
        return Worker.Result.SUCCESS
    }

    companion object {

        private val TAG = Communication::class.java.simpleName
        private val HEART_BEAT = "beep".toByteArray()
        private const val first = "Hello World"
        private const val host = "192.168.1.172"
        private const val port = 30011
        private const val key = "1234"
        private const val HEAD: Byte = 2
        private const val TAIL: Byte = 3
        private val myConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build()
        private var communicationWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<Communication>()
                .setConstraints(myConstraints)
                .build()

        fun scheduleRepeaterWorker() {
            Log.e(TAG, "scheduleRepeaterWorker")

            WorkManager.getInstance().let {
                val myConstraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build()

                val communicationWork =
                        PeriodicWorkRequestBuilder<Communication>(15, TimeUnit.MINUTES)
                                .setConstraints(myConstraints)
                                .build()

                val compressionWorkId = communicationWork.id
                it.cancelWorkById(compressionWorkId)

                it.enqueue(communicationWork)
            }
        }

        fun fireWorker() {
            Log.e(TAG, "fireWorker")

            WorkManager.getInstance().let {
                it.cancelAllWork()
                communicationWork = OneTimeWorkRequestBuilder<Communication>()
                        .setConstraints(myConstraints)
                        .build()
                it.enqueue(communicationWork)
            }

        }
    }
}
