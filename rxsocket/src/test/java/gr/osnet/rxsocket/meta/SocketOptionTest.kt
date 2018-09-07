package gr.osnet.rxsocket.meta

import gr.osnet.rxsocket.fromBase64
import gr.osnet.rxsocket.toBase64
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

internal class SocketOptionTest {

    @Test
    fun add_remove_HeadTail() {
        val original = "Hello".toByteArray(Charsets.UTF_8)
        val mOptionHasHead = SocketOption.Builder()
                .setHead(2)
                .build()

        val mOptionHasTail = SocketOption.Builder()
                .setTail(3)
                .build()

        val mOptionHasBoth = SocketOption.Builder()
                .setHead(2)
                .setTail(3)
                .build()

        Assertions.assertArrayEquals(mOptionHasHead.addHeadTail(original), ByteArray(1) { 2 } + "Hello".toByteArray(Charsets.UTF_8))

        Assertions.assertArrayEquals(mOptionHasTail.addHeadTail(original), "Hello".toByteArray(Charsets.UTF_8) + ByteArray(1) { 3 })

        Assertions.assertArrayEquals(mOptionHasBoth.addHeadTail(original), ByteArray(1) { 2 } + "Hello".toByteArray(Charsets.UTF_8) + ByteArray(1) { 3 })
    }

    @Test
    fun add_remove_CheckSum() {
        val original = "123Hello456789Hello".toByteArray(Charsets.UTF_8)
        val mOptionHasCheckSum = SocketOption.Builder()
                .setCheckSum("OK".toByteArray(), "NAK".toByteArray())
                .build()

        val mOptionHasNotCheckSum = SocketOption.Builder()
                .build()

        Assertions.assertArrayEquals(mOptionHasCheckSum.mCheckSumConfig?.addCheckSum(original), "123Hello456789Hello05C5".toByteArray(Charsets.UTF_8))
        Assertions.assertArrayEquals(mOptionHasNotCheckSum.mCheckSumConfig?.addCheckSum(original)
                ?: original, original)

        Assertions.assertArrayEquals(mOptionHasCheckSum.mCheckSumConfig?.checkCheckSum("123Hello456789Hello05C5".toByteArray(Charsets.UTF_8)), original)
        Assertions.assertArrayEquals(mOptionHasNotCheckSum.mCheckSumConfig?.checkCheckSum("123Hello456789Hello".toByteArray(Charsets.UTF_8))
                ?: original, original)
    }

    @Test
    fun encrypt_compress() {

        val password = "1234"
        val original = "123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123"

        val mOptionEncryptCompress = SocketOption.Builder()
                .useCompression(true)
                .setEncryption(password, EncryptionPadding.PKCS5Padding, "ENC^")
                .build()

        val compressed = mOptionEncryptCompress.compress(original.toByteArray(Charsets.UTF_8))
        val enc = mOptionEncryptCompress.encrypt(compressed).toBase64
        println(original.toByteArray(Charsets.UTF_8).size)
        println(compressed.size)
        val dec = mOptionEncryptCompress.decrypt(enc.fromBase64)
        val decompressed = mOptionEncryptCompress.decompress(dec)
        Assertions.assertEquals(original, decompressed.toString)
    }

    @Test
    fun read() {
        val password = "1234"
        val clear = "src/test/resources/1.txt"
        val formatted = "src/test/resources/2.txt"
        val deFormatted = "src/test/resources/3.txt"

        val bufferedReader = File(clear).bufferedReader()
        val bufferedWriter = File(formatted).bufferedWriter()
        val bufferedReader2 = File(formatted).bufferedReader()
        val bufferedWriter2 = File(deFormatted).bufferedWriter()

        val mOption = SocketOption.Builder()
                .setEncryption(password, EncryptionPadding.PKCS5Padding, "ENC^")
                .useCompression(true)
                .setHead(2)
                .setTail(3)
                .build()
        while (bufferedReader.ready()) {
            val data = bufferedReader.readText()
            print(data)
            bufferedWriter.write(mOption.pack(data.toByteArray(Charsets.UTF_8), true, true).toString)
        }
        bufferedReader.close()
        bufferedWriter.close()
/*
        for (i in 0..lines) {
            val data = mOption.read(bufferedReader2, false)
            if (data.isNotEmpty()) {
                print(data.toString)
                bufferedWriter2.write(data.toString)
            }
        }*/
        bufferedReader2.close()
        bufferedWriter2.close()
    }
}
