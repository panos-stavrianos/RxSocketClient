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
        private val mHead: Byte?,
        private val mTail: Byte?,
        val mOk: ByteArray?,
        val mWrong: ByteArray?,
        private val mUseCheckSum: Boolean = false,
        private val mUsePKCS7: Boolean = true,
        private val mUseCompression: Boolean = false,
        val mFirstContact: String?,
        private val mPreSharedKey: String?,
        private val mEncryptionPrefix: String = ""
) {

    private constructor (builder: Builder) : this(
            builder.mHeartBeatConfig,
            builder.mHead,
            builder.mTail,
            builder.mOk,
            builder.mWrong,
            builder.mUseCheckSum,
            builder.mUsePKCS7,
            builder.mUseCompression,
            builder.mFirstContact,
            builder.mPreSharedKey,
            builder.mEncryptionPrefix
    )

    class Builder {
        var mHeartBeatConfig: HeartBeatConfig? = null
            private set

        var mHead: Byte? = null
            private set

        var mTail: Byte? = null
            private set

        var mOk: ByteArray? = null
            private set

        var mWrong: ByteArray? = null
            private set

        var mUseCheckSum: Boolean = false
            private set

        var mUsePKCS7: Boolean = true
            private set

        var mUseCompression: Boolean = false
            private set


        var mFirstContact: String? = null
            private set

        var mPreSharedKey: String? = null
            private set

        var mEncryptionPrefix: String = ""
            private set

        fun setHeartBeat(data: ByteArray, interval: Long, units: TimeUnit = TimeUnit.MILLISECONDS) =
                apply { this.mHeartBeatConfig = HeartBeatConfig(data, units.toMillis(interval)) }

        fun setHead(head: Byte) = apply { this.mHead = head }

        fun setTail(tail: Byte) = apply { this.mTail = tail }

        fun setOk(ok: ByteArray) = apply { this.mOk = ok }

        fun setWrong(wrong: ByteArray) = apply { this.mWrong = wrong }

        fun useCheckSum(useCheckSum: Boolean) = apply { this.mUseCheckSum = useCheckSum }

        fun setFirstContact(mFirstContact: String) = apply { this.mFirstContact = mFirstContact }

        fun setPreSharedKey(mPreSharedKey: String) = apply { this.mPreSharedKey = mPreSharedKey }

        fun setEncryptionPrefix(mEncryptionPrefix: String) = apply { this.mEncryptionPrefix = mEncryptionPrefix }

        fun usePKCS7(usePKCS7: Boolean) = apply { this.mUsePKCS7 = usePKCS7 }

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


    fun isOk(data: ByteArray): Boolean = mOk != null && mWrong != null && Arrays.equals(data, mOk)

    fun isWrong(data: ByteArray): Boolean = mOk != null && mWrong != null && Arrays.equals(data, mOk)

    fun addCheckSum(data: ByteArray): ByteArray =
            if (mUseCheckSum)
                data.toString.addCheckSum.toByteArray(Charsets.UTF_8)
            else
                data

    fun checkCheckSum(data: ByteArray): ByteArray {
        if (mUseCheckSum) {
            val message = data.toString
            return if (message.validateCheckSum)
                message.removeCheckSum.toByteArray(Charsets.UTF_8)
            else
                ByteArray(0)
        }
        return data
    }

    fun encrypt(data: ByteArray): ByteArray =
            mPreSharedKey?.let {
                CompressEncrypt.encrypt(data, it, mUsePKCS7)
            } ?: data

    fun encryptFile(path: String): String =
            mPreSharedKey?.let { CompressEncrypt.encryptFile(path, it) } ?: path

    fun decrypt(data: ByteArray): ByteArray =
            mPreSharedKey?.let {
                CompressEncrypt.decrypt(data, it, mUsePKCS7)
            }
                    ?: data

    fun decryptFile(path: String): String =
            mPreSharedKey?.let { CompressEncrypt.decryptFile(path, it) }
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

        var result = message.removePrefix(mEncryptionPrefix).toString().toByteArray(Charsets.UTF_8)
        if (mUseCompression || !mPreSharedKey.isNullOrEmpty())
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
    fun pack(data: ByteArray, encrypt: Boolean = false, compress: Boolean = false): ByteArray {
        var result: ByteArray = data

        if (compress) result = compress(result)
        if (encrypt) result = encrypt(result)

        if (compress || encrypt) result = result.toBase64
        if (encrypt) result = (mEncryptionPrefix.toByteArray(Charsets.UTF_8)) + result

        result = addCheckSum(result)
        return addHeadTail(result)
    }

    class HeartBeatConfig(var data: ByteArray?, var interval: Long)
}