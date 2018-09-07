# RxSocket (Under Development)
This project is a fork from [codeestX](https://github.com/codeestX/RxSocketClient/)

# Features Added
* Compression
* Encryption
* CheckSum (with Ok - Wrong Responses)
* File send

# Usage
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
        implementation 'com.github.panos-stavrianos:RxSocketClient:0.0.3'
}
```

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
                .setFirstContact(first)//FirstContact is always not compressed or encrypted
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

//In case you have multiple disposables you can use CompositeDisposable to add and dispose them all together
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
                         .setEncryption(key, EncryptionPadding.PKCS5Padding, "ENC:")
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

                      "Hey you" -> mClient.send("me?")

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
                          mClient.send("File size: $encryptedFileSize", true, true)

                          // In this case we already have encrypted our file and jpeg images are already compressed
                          // so we pass false in both parameters
                          mClient.sendFile(encryptedPath, encrypt = false, compress =  false)
                      }

                      "I am done with you..." -> mClient.disconnect() // or disposables.clear()

            }
          }
```

# License

      Copyright (c) 2017 codeestX

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.

