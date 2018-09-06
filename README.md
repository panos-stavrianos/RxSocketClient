# RxSocket (Under Development)
This project is a fork from [codeestX](https://github.com/codeestX/RxSocketClient/)

# Added
* compression
* encryption
* CRC16 (with Ok - Wrong Responses)
* First Contact Message

# Usage

Step 1. Add the JitPack repository to your build file

	allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
   
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.codeestX:RxSocketClient:v1.0.1'
	}
	
### init
```java
SocketClient mClient = RxSocketClient
                .create(new SocketConfig.Builder()
                        .setIp(IP)
                        .setPort(PORT)
                        .setCharset(Charsets.UTF_8)
                        .setThreadStrategy(ThreadStrategy.ASYNC)
                        .setTimeout(30 * 1000)
                        .build())
                .option(new SocketOption.Builder()
                        .setHeartBeat(HEART_BEAT, 60 * 1000)
                        .setHead(HEAD)
                        .setTail(TAIL)
                        .build());

```
| value | default | description |
| :--: | :--: | :--: |
| Ip | required | host address |
| Port | required | port number |
| Charset | UTF_8 | the charset when encode a String to byte[] |
| ThreadStrategy | Async | sending data asynchronously or synchronously|
| Timeout | 0 | the timeout of a connection, millisecond |
| HeartBeat | Optional | value and interval of heartbeat, millisecond |
| Head | Optional | appending bytes at head when sending data, not included heartbeat |
| Tail | Optional | appending bytes at last when sending data, not included heartbeat |

### connect
```java
Disposable ref = mClient.connect()
                // anything else what you can do with RxJava
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SocketSubscriber() {
                   
                    @Override
                    public void onConnected() {}
                   
                    @Override
                    public void onResponse(@NotNull String data, long timePassed) {}
                   
                    @Override
                    public void onDisconnectedWithError(@NotNull Throwable throwable, long timePassed) {}
                   
                    @Override
                    public void onDisconnected(long timePassed) {}
               
                }, throwable -> {
                    Log.e(TAG, throwable.toString());
                });
    
```

### disconnect
```java
mClient.disconnect();
//or
ref.dispose();
```

### sendData
```java
mClient.sendData(bytes);
//or
mClient.sendData(string);
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

