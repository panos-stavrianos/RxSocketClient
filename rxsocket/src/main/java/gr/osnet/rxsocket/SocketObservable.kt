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
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.max

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */


private val logger = KotlinLogging.logger {}

class SocketObservable(private val mConfig: SocketConfig, val mSocket: Socket, val mClient: SocketClient, val mOption: SocketOption?) : Observable<DataWrapper>() {
    var state = SocketState.CLOSE

    val mReadThread: ReadThread = ReadThread()
    var observerWrapper: SocketObserver? = null
    var mHeartBeatRef: Disposable? = null
    var isClosed = false
    override fun subscribeActual(observer: Observer<in DataWrapper>?) {
        observerWrapper = SocketObserver(observer)
        isClosed = false
        observerWrapper?.let {
            observer?.onSubscribe(it)
            try {
                try {
                    mSocket.connect(InetSocketAddress(mConfig.mIp, mConfig.mPort
                            ?: 1080), mConfig.mTimeout ?: 0)
                    mClient.send(mOption?.mFirstContact, false, false)

                    observer?.onNext(DataWrapper(SocketState.OPEN, ByteArray(0)))
                    mReadThread.start()
                } catch (throwable: Throwable) {
                    state = SocketState.CLOSE_WITH_ERROR
                    close(throwable)
                }
            } catch (throwable: Throwable) {
                state = SocketState.CLOSE_WITH_ERROR
                close(throwable)
            }
        }

    }

    fun setHeartBeatRef(ref: Disposable) {
        mHeartBeatRef = ref
    }

    fun close(throwable: Throwable) {
        if (!isClosed) {
            observerWrapper?.onNext(DataWrapper(state, ByteArray(0), throwable))
            observerWrapper?.dispose()
            isClosed = true
        }
    }

    inner class SocketObserver(private val observer: Observer<in DataWrapper>?) : Disposable {
        fun onNext(data: ByteArray) {
            if (mSocket.isConnected) {
                mClient.lastExchange = System.currentTimeMillis()
                val message = mOption?.mCheckSumConfig?.checkCheckSum(data) ?: data

                mOption?.mCheckSumConfig?.apply {
                    if (isOk(message)) return@onNext
                    if (isWrong(message)) {
                        logger.info { "Server send NAK" }
                        state = SocketState.CLOSE_WITH_ERROR
                        dispose()
                        return@onNext
                    }
                }
                if (message.isEmpty()) {
                    mOption?.mCheckSumConfig?.apply {
                        logger.info { "Server send wrong CheckSum: " + data.toString }
                        mClient.send(wrong, false, false)
                        mClient.send("END", false, false)
                        mClient.disconnectWithError(Throwable("Server send wrong CheckSum"))
                    }
                } else {
                    mOption?.mCheckSumConfig?.apply {
                        mClient.send(ok, false)
                    }
                    observer?.onNext(DataWrapper(SocketState.CONNECTED, message))
                }
            }
        }

        fun onNext(dataWrapper: DataWrapper) {
            observer?.onNext(dataWrapper)
        }

        override fun dispose() {
            mHeartBeatRef?.dispose()
            mSocket.close()
            logger.info { "dispose!!" }
        }

        override fun isDisposed(): Boolean {
            return mSocket.isConnected
        }
    }


    inner class ReadThread : Thread() {

        override fun run() {
            super.run()
            try {
                var time = System.currentTimeMillis()
                var sumTime: Long = 0
                var counter = 0
                var averageTime: Long = 100

                val input = mSocket.getInputStream().bufferedReader()
                while (!mReadThread.isInterrupted && mSocket.isConnected) {
                    val data = mOption?.read(input) ?: input.readText().toByteArray(Charsets.UTF_8)

                    if (data.isNotEmpty()) {
                        val deltaTime = System.currentTimeMillis() - time
                        if (deltaTime < 1000) {
                            sumTime += deltaTime
                            counter++
                            averageTime = sumTime / counter
                            logger.info { deltaTime.toString() + " av: " + averageTime }
                        }
                        time = System.currentTimeMillis()
                        observerWrapper?.onNext(data)
                    }
                    averageTime = max(averageTime, 1000)
                    Thread.sleep(averageTime / 4)
                }
            } catch (throwable: Throwable) {
                mClient.disconnectWithError(throwable)
            }
        }
    }


}