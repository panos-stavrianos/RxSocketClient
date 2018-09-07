@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package gr.osnet.rxsocket

import com.google.common.io.ByteStreams
import gr.osnet.rxsocket.meta.toString
import org.apache.commons.codec.binary.Base64
import java.io.*
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8


/**
 * Created by panos on 15/11/2017.
 */

val ByteArray.toBase64: ByteArray get() = Base64.encodeBase64(this)

val ByteArray.fromBase64: ByteArray get() = Base64.decodeBase64(this)

object CompressEncrypt {


    fun encrypt(plaintext: String, password: String, padding: String): ByteArray = encrypt(plaintext.toByteArray(Charsets.UTF_8), password, padding)

    fun encrypt(plaintext: ByteArray, password: String, padding: String): ByteArray {
        if (password.isEmpty()) return ByteArray(0)
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, 100, 128)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

        val keyBytes = f.generateSecret(spec).encoded
        val key = SecretKeySpec(keyBytes, "AES")

        val ivBytes = ByteArray(16)
        random.nextBytes(ivBytes)

        val iv = IvParameterSpec(ivBytes)

        val c = Cipher.getInstance("AES/CBC/$padding")

        c.init(Cipher.ENCRYPT_MODE, key, iv)
        val encValue = c.doFinal(plaintext)

        val finalCipherText = ByteArray(encValue.size + 2 * 16)
        System.arraycopy(ivBytes, 0, finalCipherText, 0, 16)
        System.arraycopy(salt, 0, finalCipherText, 16, 16)
        System.arraycopy(encValue, 0, finalCipherText, 32, encValue.size)
        return finalCipherText
    }

    fun encryptFile(path: String, password: String, padding: String): String {
        if (password.isEmpty()) return path
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, 100, 128)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

        val keyBytes = f.generateSecret(spec).encoded
        val key = SecretKeySpec(keyBytes, "AES")

        val ivBytes = ByteArray(16)
        random.nextBytes(ivBytes)

        val iv = IvParameterSpec(ivBytes)

        val c = Cipher.getInstance("AES/CBC/$padding")

        c.init(Cipher.ENCRYPT_MODE, key, iv)

        val fileOutputStream = FileOutputStream("$path.enc")
        fileOutputStream.write(ivBytes)
        fileOutputStream.write(salt)
        fileOutputStream.flush()

        val cos = CipherOutputStream(fileOutputStream, c)
        val isa: InputStream = FileInputStream(path) //Input stream
        ByteStreams.copy(isa, cos)
        isa.close()
        cos.flush()
        cos.close()
        return "$path.enc"
    }


    fun decrypt(data: ByteArray, password: String, padding: String): ByteArray {
        if (password.isEmpty()) return ByteArray(0)

        try {
            val ivBytes = ByteArray(16)
            val salt = ByteArray(16)
            val cipherBytes = ByteArray(data.size - 2 * 16)

            System.arraycopy(data, 0, ivBytes, 0, 16)
            System.arraycopy(data, 16, salt, 0, 16)
            System.arraycopy(data, 32, cipherBytes, 0, data.size - 2 * 16)

            val spec = PBEKeySpec(password.toCharArray(), salt, 100, 128)
            val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

            val keyBytes = f.generateSecret(spec).encoded
            val key = SecretKeySpec(keyBytes, "AES")

            val c = Cipher.getInstance("AES/CBC/$padding")

            val ivParams = IvParameterSpec(ivBytes)
            c.init(Cipher.DECRYPT_MODE, key, ivParams)
            return c.doFinal(cipherBytes)

        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return ByteArray(0)
    }

    fun decryptFile(path: String, password: String, padding: String): String {
        if (password.isEmpty()) return ""
        try {
            val fis = FileInputStream(path)
            val newName = path.replace(".enc", ".dec")
            val fos = FileOutputStream(newName)

            val ivBytes = ByteArray(16)
            fis.read(ivBytes)
            val salt = ByteArray(16)
            fis.read(salt)

            val spec = PBEKeySpec(password.toCharArray(), salt, 100, 128)
            val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

            val keyBytes = f.generateSecret(spec).encoded
            val key = SecretKeySpec(keyBytes, "AES")

            val c = Cipher.getInstance("AES/CBC/$padding")

            val ivParams = IvParameterSpec(ivBytes)
            c.init(Cipher.DECRYPT_MODE, key, ivParams)
            val cis = CipherInputStream(fis, c)

            checkNotNull(cis)
            checkNotNull(fos)
            val buf = ByteArray(8192)
            var total: Long = 0
            try {
                while (true) {
                    val r = cis.read(buf)
                    if (r == -1) {
                        break
                    }
                    fos.write(buf, 0, r)
                    total += r.toLong()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            fos.flush()
            fos.close()
            cis.close()
            return newName
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return ""
    }

    fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(data.toString) }
        return bos.toByteArray()
    }

    fun compressFile(path: String): String {
        try {
            val fis = FileInputStream(path)
            val fos = FileOutputStream("$path.gzip")
            val gzipOS = GZIPOutputStream(fos)
            val buffer = ByteArray(1024)

            var len = fis.read(buffer)
            while (len != -1) {
                gzipOS.write(buffer, 0, len)
                len = fis.read(buffer)
            }

            //close resources
            gzipOS.close()
            fos.close()
            fis.close()
            return "$path.gzip"

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return path
    }

    fun decompress(data: ByteArray): ByteArray = GZIPInputStream(data.inputStream()).bufferedReader(UTF_8).use { it.readText() }.toByteArray(Charsets.UTF_8)

    fun decompressFile(path: String): String {
        try {
            val fis = FileInputStream(path)
            val gis = GZIPInputStream(fis)
            val fos = FileOutputStream(path.replace(".gzip", ".deco"))
            val buffer = ByteArray(1024)
            var len = gis.read(buffer)
            while (len != -1) {
                fos.write(buffer, 0, len)
                len = gis.read(buffer)
            }
            //close resources
            fos.close()
            gis.close()
            return path.replace(".gzip", ".deco")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return path
    }

    private fun decompressGzipFile(gzipFile: String, newFile: String) {
        try {
            val fis = FileInputStream(gzipFile)
            val gis = GZIPInputStream(fis)
            val fos = FileOutputStream(newFile)
            val buffer = ByteArray(1024)
            var len = gis.read(buffer)
            while (len != -1) {
                fos.write(buffer, 0, len)
                len = gis.read(buffer)
            }
            //close resources
            fos.close()
            gis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun compressGzipFile(file: String, gzipFile: String) {
        try {
            val fis = FileInputStream(file)
            val fos = FileOutputStream(gzipFile)
            val gzipOS = GZIPOutputStream(fos)
            val buffer = ByteArray(1024)


            var len = fis.read(buffer)
            while (len != -1) {
                gzipOS.write(buffer, 0, len)
                len = fis.read(buffer)
            }

            //close resources
            gzipOS.close()
            fos.close()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}