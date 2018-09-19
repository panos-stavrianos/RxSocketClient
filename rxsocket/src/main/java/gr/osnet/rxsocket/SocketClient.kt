/*
 * Copyright (C) 2017 codeestX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package gr.osnet.rxsocket

import gr.osnet.rxsocket.meta.*
import gr.osnet.rxsocket.post.AsyncPoster
import gr.osnet.rxsocket.post.IPoster
import gr.osnet.rxsocket.post.SyncPoster
import io.reactivex.Observable
import mu.KotlinLogging
import java.io.*
import java.net.Socket
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */


private val logger = KotlinLogging.logger {}

class SocketClient(private val mConfig: SocketConfig) {

    var mSocket: Socket = Socket()
    private var mOption: SocketOption? = null
    lateinit var mObservable: Observable<DataWrapper>
    lateinit var mIPoster: IPoster
    var mExecutor: Executor = Executors.newCachedThreadPool()
    var lastExchange: Long = System.currentTimeMillis()
    fun option(option: SocketOption): SocketClient {
        mOption = option
        return this
    }


    fun connect(): Observable<DataWrapper> {
        connectedTime = System.currentTimeMillis()
        mObservable = SocketObservable(mConfig, mSocket, this, mOption)
        mIPoster = if (mConfig.mThreadStrategy == ThreadStrategy.ASYNC) AsyncPoster(this, mExecutor) else SyncPoster(this, mExecutor)
        initHeartBeat()
        return mObservable
    }

    fun waitUntilEnd() {
        while (!(mObservable as SocketObservable).isClosed)
            Thread.sleep(400)
    }

    fun disconnect() {
        if (mObservable is SocketObservable) {
            (mObservable as SocketObservable).close(Throwable(""))
        }
    }

    fun disconnectWithError(throwable: Throwable) {
        if (mObservable is SocketObservable) {
            (mObservable as SocketObservable).state = SocketState.CLOSE_WITH_ERROR
            (mObservable as SocketObservable).close(throwable)
        }
    }

    private fun initHeartBeat() {
        mOption?.apply {
            if (mHeartBeatConfig != null) {
                val disposable = Observable.interval(mHeartBeatConfig.interval / 2, TimeUnit.MILLISECONDS)
                        .subscribe {
                            when {
                                shouldSendHeartBeat() -> send(mHeartBeatConfig.data
                                        ?: ByteArray(0))
                            }
                        }
                if (mObservable is SocketObservable) {
                    (mObservable as SocketObservable).setHeartBeatRef(disposable)
                }
            }
        }
    }

    private fun shouldSendHeartBeat(): Boolean {
        mOption?.apply {
            val deltaTime = System.currentTimeMillis() - lastExchange
            return mHeartBeatConfig != null && deltaTime > mHeartBeatConfig.interval
        }
        return false
    }


    fun send(message: String?, encrypt: Boolean? = null, compress: Boolean? = null) {
        logger.info { "To server: $message" }
        send(message?.toByteArray(charset = mConfig.mCharset), encrypt, compress)
    }

    fun send(data: ByteArray?, encrypt: Boolean? = null, compress: Boolean? = null) {
        if (data == null) return
        val result = mOption?.pack(data, encrypt, compress) ?: data

        mIPoster.enqueue(result)
        lastExchange = System.currentTimeMillis()
    }

    fun sendFile(path: String?, encrypt: Boolean? = null, compress: Boolean? = null) {
        path?.let {
            val dest: String =
                    if (encrypt == true && compress == true) mOption?.encryptFile(path) ?: it
                    else if (compress == true) mOption?.compressFile(path) ?: it
                    else if (encrypt == true) mOption?.encryptFile(path) ?: it
                    else path

            val file = File(dest)
            val size = file.length()
            try {
                val buf = BufferedInputStream(FileInputStream(file))
                val bufferSize = 15 * 1024 * 1024
                val result = ByteArray(bufferSize)
                logger.info { "To server file size: ByteArray ->$size" }
                while (buf.available() > 0) {
                    logger.info { "=======" }
                    val available = buf.available()
                    val toSend = if (available < bufferSize) {
                        buf.read(result, 0, available)
                        result.copyOf(available)
                    } else {
                        buf.read(result, 0, bufferSize)
                        result.copyOf(bufferSize)
                    }
                    mSocket.getOutputStream()?.apply {
                        try {
                            write(toSend)
                            flush()
                        } catch (throwable: Throwable) {
                            disconnectWithError(throwable)
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            logger.info { "To server : File Size ->" + size / 1000 + "kb" }
            lastExchange = System.currentTimeMillis()
        }
    }

    fun encryptFile(path: String): String =
            mOption?.encryptFile(path) ?: path

    companion object {
        var connectedTime: Long = System.currentTimeMillis()
    }

}