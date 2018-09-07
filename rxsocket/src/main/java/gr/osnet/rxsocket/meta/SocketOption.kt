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

package gr.osnet.rxsocket.meta

import gr.osnet.rxsocket.CompressEncrypt
import gr.osnet.rxsocket.fromBase64
import gr.osnet.rxsocket.toBase64
import java.io.BufferedReader
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/8
 * @description:
 */

val ByteArray.toString get() = String(this, Charsets.UTF_8)

class SocketOption(
        val mHeartBeatConfig: HeartBeatConfig?,
        val mCheckSumConfig: CheckSumConfig?,
        val mEncryptionConfig: EncryptionConfig?,
        private val mHead: Byte?,
        private val mTail: Byte?,
        private val mUseCompression: Boolean = false,
        val mFirstContact: String?
) {

    private constructor (builder: Builder) : this(
            builder.mHeartBeatConfig,
            builder.mCheckSumConfig,
            builder.mEncryptionConfig,
            builder.mHead,
            builder.mTail,
            builder.mUseCompression,
            builder.mFirstContact
    )

    class Builder {
        var mHeartBeatConfig: HeartBeatConfig? = null
            private set

        var mCheckSumConfig: CheckSumConfig? = null
            private set

        var mEncryptionConfig: EncryptionConfig? = null
            private set

        var mHead: Byte? = null
            private set

        var mTail: Byte? = null
            private set

        var mUseCompression: Boolean = false
            private set

        var mFirstContact: String? = null
            private set

        fun setHeartBeat(data: ByteArray, interval: Long, units: TimeUnit = TimeUnit.MILLISECONDS) =
                apply { this.mHeartBeatConfig = HeartBeatConfig(data, units.toMillis(interval)) }

        fun setCheckSum(ok: ByteArray, wrong: ByteArray) =
                apply { this.mCheckSumConfig = CheckSumConfig(ok, wrong) }

        fun setEncryption(password: String, padding: EncryptionPadding, prefix: String) =
                apply {
                    if (password.isNotEmpty())
                        this.mEncryptionConfig = EncryptionConfig(password, padding.padding, prefix)
                }

        fun setHead(head: Byte) = apply { this.mHead = head }

        fun setTail(tail: Byte) = apply { this.mTail = tail }

        fun setFirstContact(mFirstContact: String) = apply { this.mFirstContact = mFirstContact }

        fun useCompression(useCompression: Boolean) = apply { this.mUseCompression = useCompression }

        fun build() = SocketOption(this)
    }

    private fun hasHeadTail(): Int {
        return if (mHead != null && mTail != null)
            HeadTail.BOTH
        else if (mHead != null)
            HeadTail.HEAD_ONLY
        else if (mTail != null)
            HeadTail.TAIL_ONLY
        else
            HeadTail.NONE
    }

    fun addHeadTail(data: ByteArray): ByteArray =
            (mHead?.let { head -> ByteArray(1) { head } } ?: ByteArray(0)) +
                    data +
                    (mTail?.let { tail -> ByteArray(1) { tail } } ?: ByteArray(0))


    fun encrypt(data: ByteArray): ByteArray =
            mEncryptionConfig?.run {
                CompressEncrypt.encrypt(data, password, padding)
            } ?: data

    fun encryptFile(path: String): String =
            mEncryptionConfig?.run { CompressEncrypt.encryptFile(path, password, padding) } ?: path

    fun decrypt(data: ByteArray): ByteArray =
            mEncryptionConfig?.run {
                CompressEncrypt.decrypt(data, password, padding)
            }
                    ?: data

    fun decryptFile(path: String): String =
            mEncryptionConfig?.run { CompressEncrypt.decryptFile(path, password, padding) }
                    ?: path

    fun compress(data: ByteArray): ByteArray =
            if (mUseCompression) CompressEncrypt.compress(data) else data

    fun compressFile(path: String): String =
            CompressEncrypt.compressFile(path)

    fun decompress(data: ByteArray): ByteArray =
            if (mUseCompression) CompressEncrypt.decompress(data) else data

    fun decompressFile(path: String): String =
            if (mUseCompression) CompressEncrypt.compressFile(path) else path

    fun read(input: BufferedReader): ByteArray {
        if (!input.ready()) return ByteArray(0)
        val message = StringBuilder()

        when (hasHeadTail()) {
            HeadTail.BOTH -> {
                var next: Int
                message.append("")
                if (input.read() == mHead!!.toInt()) {
                    while (true) {
                        next = input.read()
                        if (next == -1) return ByteArray(0)
                        if (next == mTail!!.toInt())
                            break
                        message.append(next.toChar())
                    }
                }
            }
            HeadTail.HEAD_ONLY -> {//todo
            }
            HeadTail.TAIL_ONLY -> {//todo
            }
            HeadTail.NONE -> message.append(input.read().toChar())
        }
        var result = message.removePrefix(mEncryptionConfig?.prefix.toString()).toString().toByteArray(Charsets.UTF_8)
        if (mUseCompression || mEncryptionConfig != null)
            result = result.fromBase64
        result = decrypt(result)
        result = decompress(result)

        return result
    }

    /**
     * Execute the following an return the new Byte Array,
     * Order matters!
     * 1. Compress (Optional)
     * 2. Encrypt (Optional)
     * 3. Base64 (If either Compress or Encrypt we must to convert base64 because the start and end byte)
     * 4. EncryptionPrefix(If Encrypt then add the prefix)
     * 5. Add CheckSum (Optional)
     * 6. Add Head-Tail (Optional)
     * */
    fun pack(data: ByteArray, encrypt: Boolean?, compress: Boolean?): ByteArray {
        var result: ByteArray = data
        val didCompress =
                if (compress == null)
                    if (mUseCompression) {
                        result = compress(result)
                        true
                    } else false
                else
                    if (compress) {
                        result = compress(result)
                        true
                    } else false


        val didEncrypt =
                if (encrypt == null)
                    if (mEncryptionConfig != null) {
                        result = encrypt(result)
                        true
                    } else false
                else
                    if (mEncryptionConfig != null && encrypt) {
                        result = encrypt(result)
                        true
                    } else false

        if (didCompress || didEncrypt) result = result.toBase64
        if (didEncrypt) result = (mEncryptionConfig?.prefix?.toByteArray(Charsets.UTF_8)) ?: ByteArray(0) + result

        result = mCheckSumConfig?.addCheckSum(result) ?: result
        return addHeadTail(result)
    }

    class HeartBeatConfig(var data: ByteArray?, var interval: Long)
    class CheckSumConfig(var ok: ByteArray, var wrong: ByteArray) {
        fun isOk(data: ByteArray): Boolean = Arrays.equals(data, ok)

        fun isWrong(data: ByteArray): Boolean = Arrays.equals(data, ok)

        fun addCheckSum(data: ByteArray): ByteArray =
                data.toString.addCheckSum.toByteArray(Charsets.UTF_8)

        fun checkCheckSum(data: ByteArray): ByteArray {
            val message = data.toString
            return if (message.validateCheckSum)
                message.removeCheckSum.toByteArray(Charsets.UTF_8)
            else
                ByteArray(0)
        }


    }

    class EncryptionConfig(var password: String, var padding: String, var prefix: String)

}