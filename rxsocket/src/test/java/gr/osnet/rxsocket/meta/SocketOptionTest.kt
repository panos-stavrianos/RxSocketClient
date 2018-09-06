package gr.osnet.rxsocket.meta

import gr.osnet.rxsocket.fromBase64
import gr.osnet.rxsocket.toBase64
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

internal class SocketOptionTest {

    @Test
    fun add_remove_HeadTail() {
        val original = "Hello".toByteArray()
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

        Assertions.assertArrayEquals(mOptionHasHead.addHeadTail(original), ByteArray(1) { 2 } + "Hello".toByteArray())

        Assertions.assertArrayEquals(mOptionHasTail.addHeadTail(original), "Hello".toByteArray() + ByteArray(1) { 3 })

        Assertions.assertArrayEquals(mOptionHasBoth.addHeadTail(original), ByteArray(1) { 2 } + "Hello".toByteArray() + ByteArray(1) { 3 })
    }

    @Test
    fun add_remove_CheckSum() {
        val original = "123Hello456789Hello".toByteArray()
        val mOptionHasCheckSum = SocketOption.Builder()
                .useCheckSum(true)
                .build()

        val mOptionHasNotCheckSum = SocketOption.Builder()
                .build()

        Assertions.assertArrayEquals(mOptionHasCheckSum.addCheckSum(original), "123Hello456789Hello05C5".toByteArray())
        Assertions.assertArrayEquals(mOptionHasNotCheckSum.addCheckSum(original), original)

        Assertions.assertArrayEquals(mOptionHasCheckSum.checkCheckSum("123Hello456789Hello05C5".toByteArray()), original)
        Assertions.assertArrayEquals(mOptionHasNotCheckSum.checkCheckSum("123Hello456789Hello".toByteArray()), original)
    }

    @Test
    fun encrypt_compress() {

        val password = "1234"
        val original = "123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123123Hello456789Hello123"

        val mOptionEncryptCompress = SocketOption.Builder()
                .setPreSharedKey(password)
                .useCompression(true)
                .setEncryptionPrefix("ENC^")
                .usePKCS7(false)
                .build()

        val compressed = mOptionEncryptCompress.compress(original.toByteArray())
        val enc = mOptionEncryptCompress.encrypt(compressed).toBase64
        println(original.toByteArray().size)
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
                .setPreSharedKey(password)//if you pass a key then everything you receive it will decrypted automatically
                .useCompression(true)
                .useCheckSum(false)
                .setHead(2)
                .setTail(3)
                .setEncryptionPrefix("ENC^")
                .usePKCS7(false)
                .build()
        while (bufferedReader.ready()) {
            val data = bufferedReader.readText()
            print(data)
            bufferedWriter.write(mOption.pack(data.toByteArray(), true, true).toString)
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
