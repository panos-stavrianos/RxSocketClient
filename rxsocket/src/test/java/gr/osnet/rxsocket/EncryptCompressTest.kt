package gr.osnet.rxsocket

import gr.osnet.rxsocket.meta.EncryptionPadding
import gr.osnet.rxsocket.meta.toString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class EncryptCompressTest {

    @Test
    fun encryptDecryptString() {
        val password = "1234"
        val original = "Hello"
        val enc = CompressEncrypt.encrypt(original, password, EncryptionPadding.PKCS5Padding.padding)//for some reason PKCS7 breaks in unit test
        val dec = CompressEncrypt.decrypt(enc, password, EncryptionPadding.PKCS5Padding.padding)
        Assertions.assertEquals(original, dec.toString)
    }

    @Test
    fun encryptDecryptBytes() {
        val password = "1234"
        val original = "Hello".toByteArray(Charsets.UTF_8)
        val enc = CompressEncrypt.encrypt(original, password, EncryptionPadding.PKCS5Padding.padding)//for some reason PKCS7 breaks in unit test
        val dec = CompressEncrypt.decrypt(enc, password, EncryptionPadding.PKCS5Padding.padding)
        Assertions.assertArrayEquals(original, dec)
    }

    @Test
    fun encryptDecryptFile() {
        val password = "1234"
        val original = "src/test/resources/1.jpg"
        val enc = CompressEncrypt.encryptFile(original, password, EncryptionPadding.PKCS5Padding.padding)//for some reason PKCS7 breaks in unit test
        val dec = CompressEncrypt.decryptFile(enc, password, EncryptionPadding.PKCS5Padding.padding)

    }

    @Test
    fun compressDecompressString() {
        val original = "Hello".toByteArray(Charsets.UTF_8)
        val compressed = CompressEncrypt.compress(original)
        val decompressed = CompressEncrypt.decompress(compressed)
        Assertions.assertArrayEquals(original, decompressed)
    }

    @Test
    fun compressDecompressBytes() {
        val original = "Hello".toByteArray(Charsets.UTF_8)
        val compressed = CompressEncrypt.compress(original)
        val decompressed = CompressEncrypt.decompress(compressed)
        Assertions.assertArrayEquals(original, decompressed)
    }

    @Test
    fun compressDecompressFile() {
        val original = "src/test/resources/1.txt"
        val compressed = CompressEncrypt.compressFile(original)
        val decompressed = CompressEncrypt.decompressFile(compressed)
    }


}