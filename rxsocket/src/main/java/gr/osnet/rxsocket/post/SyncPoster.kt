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

package gr.osnet.rxsocket.post

import gr.osnet.rxsocket.SocketClient
import java.util.concurrent.Executor

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */

class SyncPoster(private val mSocketClient: SocketClient, private val mExecutor: Executor) : Runnable, IPoster {

    private val queue: PendingPostQueue = PendingPostQueue()

    @Volatile
    override var executorRunning: Boolean = false

    override fun enqueue(data: ByteArray) {
        val pendingPost = PendingPost.obtainPendingPost(data)
        synchronized(this) {
            queue.enqueue(pendingPost)
            if (!executorRunning) {
                executorRunning = true
                mExecutor.execute(this)
            }
        }
    }

    override fun run() {
        try {
            try {
                while (true) {
                    var pendingPost = queue.poll(1000)
                    if (pendingPost == null) {
                        synchronized(this) {
                            pendingPost = queue.poll()
                            if (pendingPost == null) {
                                executorRunning = false
                                return
                            }
                        }
                    }
                    pendingPost?.let {
                        mSocketClient.mSocket.getOutputStream()?.apply {
                            try {
                                write(pendingPost!!.data)
                                flush()
                            } catch (throwable: Throwable) {
                                mSocketClient.disconnectWithError(throwable)
                            }
                            PendingPost.releasePendingPost(pendingPost!!)
                        }
                    }
                }
            } catch (throwable: Throwable) {
                mSocketClient.disconnectWithError(throwable)
            }

        } finally {
            executorRunning = false
        }
    }
}