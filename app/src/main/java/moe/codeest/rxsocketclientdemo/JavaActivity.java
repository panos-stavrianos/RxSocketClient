package moe.codeest.rxsocketclientdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import gr.osnet.rxsocket.RxSocketClient;
import gr.osnet.rxsocket.SocketClient;
import gr.osnet.rxsocket.SocketSubscriber;
import gr.osnet.rxsocket.meta.SocketConfig;
import gr.osnet.rxsocket.meta.SocketOption;
import gr.osnet.rxsocket.meta.ThreadStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import kotlin.text.Charsets;


/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */
public class JavaActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "JavaActivity";

    private static final String IP = "192.168.1.101";
    private static final int PORT = 55731;
    private static final byte[] HEART_BEAT = {1, 3, 4};
    private static final byte HEAD = 2;
    private static final byte TAIL = 3;

    private static final byte[] MESSAGE = {0, 1, 3};
    private static final String MESSAGE_STR = "TEST";

    private SocketClient mClient;
    private Disposable ref;
    private Button btnSend;
    private Button btnConnect;
    private Button btnDisConnect;
    private TextView tvReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSend = findViewById(R.id.btn_send);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisConnect = findViewById(R.id.btn_disconnect);
        tvReceive = findViewById(R.id.tv_receive);
        btnSend.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnDisConnect.setOnClickListener(this);

        //init
        mClient = RxSocketClient
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
    }

    private void connect() {
        //connect
        ref = mClient.connect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SocketSubscriber() {
                    @Override
                    public void onConnected() {
                        //onConnected
                        Log.e(TAG, "onConnected");
                    }

                    @Override
                    public void onResponse(@NotNull String data, long timePassed) {

                    }

                    @Override
                    public void onDisconnectedWithError(@NotNull Throwable throwable, long timePassed) {

                    }

                    @Override
                    public void onDisconnected(long timePassed) {

                    }
                }, throwable -> {
                    //onError
                    Log.e(TAG, throwable.toString());
                });
    }

    public void connect(SocketSubscriber subscriber) {
        ref = mClient.connect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                //send bytes
                mClient.sendBytes(MESSAGE, false);

                //or send string
//                mClient.sendData(MESSAGE_STR);
                break;

            case R.id.btn_connect:
                //connect
                connect();
                break;

            case R.id.btn_disconnect:
                //disconnect
                //mClient.disconnect();
                //or disconnect
                if (ref != null && !ref.isDisposed())
                    ref.dispose();
                break;
        }
    }
}
