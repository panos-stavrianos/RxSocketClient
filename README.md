[![Release](https://jitpack.io/v/panos-stavrianos/RxSocketClient.svg)](https://jitpack.io/#panos-stavrianos/RxSocketClient)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![paypal](http://orbitsystems.gr/images/Donation-Buy%20me%20beer-blue.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=J73XPNMNBGX6C)
# RxSocket
This project is a fork from [codeestX](https://github.com/codeestX/RxSocketClient/)
 
TCP socket client Android library. 
Written in Kotlin, powered by RxJava2 

# Features Added
* Compression
* Encryption
* CheckSum (with Ok - Wrong Responses)
* File send

# Setup
1. Add the JitPack repository to your build.gradle file
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

2. Add the dependency
```gradle
dependencies {
        implementation 'com.github.panos-stavrianos:RxSocketClient:0.0.4'
}
```

# Usage
###### All examples are in kotlin

### Init
```kotlin
val mClient = RxSocketClient
        .create(SocketConfig.Builder()
                .setIp("192.168.1.2")
                .setPort(5000)
                .setCharset(Charsets.UTF_8)
                .setThreadStrategy(ThreadStrategy.SYNC)
                .setTimeout(5, TimeUnit.SECONDS)
                .build())
        .option(SocketOption.Builder()
                .setHeartBeat("beep".toByteArray(), 15, TimeUnit.SECONDS)
                .setHead(HEAD)
                .setTail(TAIL)
                .setCheckSum("ACK".toByteArray(), "NAK".toByteArray())
                .setEncryption("pre shared password", EncryptionPadding.PKCS5Padding, "ENC:")
                .setFirstContact(first)//FirstContact is neither compressed nor encrypted
                .useCompression(true)
                .build())
```
| value | default | description |
| :--: | :--: | :--: |
| Ip | required | host address |
| Port | required | port number |
| Charset | UTF_8 | the charset when encode a String to byte[] |
| ThreadStrategy | Async | sending data asynchronously or synchronously|
| Timeout | 0 | the timeout of a connection with TimeUnit as second parameter (millisecond default) |
| HeartBeat | Optional | value and interval of heartbeat, millisecond |
| CheckSum | Optional | ok and wrong responses as ByteArray |
| Encryption | Optional | password, padding and prefix |
| Compression | Optional | boolean, false default |
| Head | Optional | appending bytes at head when sending data |
| Tail | Optional | appending bytes at last when sending data |

### Connect
```kotlin
val disposables = CompositeDisposable() // Just good practice
mClient.connect()
        .doOnSubscribe { disposables.add(it) }
        .subscribe(
                object : SocketSubscriber() {
                    override fun onConnected() {
                    }

                    override fun onDisconnected(timePassed: Long) {
                        Log.e(TAG, "onDisconnected in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec")
                    }

                    override fun onDisconnectedWithError(throwable: Throwable, timePassed: Long) {
                        Log.e(TAG, "onDisconnectedWithError in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec, cause: ${throwable.message}")
                    }

                    override fun onResponse(data: String, timePassed: Long) {
                        Log.e(TAG, "onResponse in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec: $data")
                    }
                },
                Consumer {
                    it.printStackTrace()
                })
    
```

### Disconnect
```kotlin
mClient.disconnect()
//or you can force the error disconnect
mClient.disconnectWithError(Throwable("Something bad happend"))

//In case you have multiple disposables, you can use CompositeDisposable to add and dispose them all together
disposables.clear()
```

### Send
There are three send methods, for string, bytes and file.
There also two optional boolean parameters for compress and encrypt.
If you pass a boolean value it will ignore the current configuration (init)

```kotlin
mClient.send(string)
mClient.send(bytes)
mClient.sendFile(path)
//or
mClient.send(string, encrypt = false, compress =  true)
mClient.send(bytes, encrypt = true, compress =  false)
mClient.sendFile(path, encrypt = true, compress =  true)
```

### Examples
##### Small useful fragments

Init a socket with encryption, compression and head-tail
```kotlin
      val mClient = RxSocketClient
                 .create(SocketConfig.Builder()
                         .setIp(host)
                         .setPort(port)
                         .setCharset(Charsets.UTF_8)
                         .setThreadStrategy(ThreadStrategy.SYNC)
                         .setTimeout(5, TimeUnit.SECONDS)
                         .build())
                 .option(SocketOption.Builder()
                         .setEncryption("supersecretpassword", EncryptionPadding.PKCS5Padding, "ENC:")
                         .useCompression(true)
                         .setHead(HEAD)
                         .setTail(TAIL)
                         .build())
```
Now we can imagine the following scenario
```kotlin
     override fun onResponse(data: String, timePassed: Long) {
            Log.e(TAG, "onResponse in ${TimeUnit.MILLISECONDS.toSeconds(timePassed)} sec: $data")

            when (data) {
                      "Hey you" -> mClient.send("Who? Me?")

                      "Send me some bytes" -> {
                          mClient.send("Well ok...")
                          mClient.send(ByteArray(4) { i -> (i * i).toByte() })
                      }

                      "Send me a selfie" -> {
                          val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/me.jpg"
                          mClient.sendFile(path)
                      }

                      "Send me first the size of the (encrypted) file and then the actual file" -> {
                          val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/me.jpg"
                          val encryptedPath = mClient.encryptFile(path)
                          val encryptedFileSize = File(encryptedPath).length()
                          mClient.send("File size: $encryptedFileSize")

                          // In this case we already have encrypted our file and jpeg images are already compressed
                          // so we pass false in both parameters
                          mClient.sendFile(encryptedPath, encrypt = false, compress =  false)
                      }

                      "I am done with you..." -> mClient.disconnect() // or disposables.clear()
              }
     }
```

### About Encryption(AES)
There are some static parameters here... In time i will add more customization.

But for now we use:

"PBKDF2WithHmacSHA1" for the creation of the 'big key', with 100 iterations and 128 key length.
We also have a random 'salt' and 'iv' of 16 bytes each

For the actual encryption we are going to use AES/CBC with two padding choices,
either PKCS5Padding or PKCS7Padding.(For some reason PKCS7 breaks in unit tests.)
___
After we encrypt our data, we need to send the iv and salt along with the encrypted data so the decryption will be possible.

So we add them at the beginning of the message

* ______iv______ + ______salt______ + ______encrypted_data______

* ______16 Bytes______ + ______16 Bytes______ + ______n Bytes______

___
Before the decrypt part, we need to split the received data.

This can be done like this: (_not real code_)
```
iv = encrypted.getRange(0,15)
salt = encrypted.getRange(16,31)
clear_encrypted = encrypted.getRange(32,encrypted.size-1)
```
Then we use the salt and the pre shared key to create the 'big key'

And Last we use the iv to decrypt the message.

###### All this is of course already implement on the library but you need to handle the server side.

# Server
Obviously you need a server! Unless you already have on you can try [this](https://packetsender.com/) for testing.

At some point i will make some scripts (most likely python) to test all the features properly.
# License
      Copyright (c) 2017 codeestX (original)
      Copyright (c) 2018 panos-stavrianos

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.

