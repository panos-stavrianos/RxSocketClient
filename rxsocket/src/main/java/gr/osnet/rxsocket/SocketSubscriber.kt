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

import gr.osnet.rxsocket.meta.DataWrapper
import gr.osnet.rxsocket.meta.SocketState
import gr.osnet.rxsocket.meta.toString
import io.reactivex.functions.Consumer


/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class SocketSubscriber : Consumer<DataWrapper> {

    override fun accept(t: DataWrapper) {
        when (t.state) {
            SocketState.CONNECTED -> {
                val data = t.data.toString
                logger.info { "From server: $data" }
                onResponse(data, t.timePassed)
            }
            SocketState.OPEN -> onConnected()
            SocketState.CLOSE -> onDisconnected(t.timePassed)
            SocketState.CLOSE_WITH_ERROR -> onDisconnectedWithError(t.throwable, t.timePassed)
        }
    }

    abstract fun onConnected()

    abstract fun onDisconnected(timePassed: Long)

    abstract fun onDisconnectedWithError(throwable: Throwable, timePassed: Long)

    abstract fun onResponse(data: String, timePassed: Long)
}