package moe.codeest.rxsocketclientdemo;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import gr.osnet.rxsocket.SocketSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/12
 * @description:
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SocketTest {

    private JavaActivity activity;
    private Button btnConnect, btnDisConnect, btnSend;
    private TextView tvReceive;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        activity = Robolectric.setupActivity(JavaActivity.class);
        btnConnect = activity.findViewById(R.id.btn_connect);
        btnDisConnect = activity.findViewById(R.id.btn_disconnect);
        btnSend = activity.findViewById(R.id.btn_send);
        tvReceive = activity.findViewById(R.id.tv_receive);
    }

    @Test
    public void testActivity() {
        assertNotNull(activity);
        assertEquals("RxSocketClientDemo", activity.getTitle());
    }

    @Test
    public void testCallback() {
        assertNotNull(activity);
        TestSocketSubscriber subscriber = new TestSocketSubscriber();
        activity.connect(subscriber);
        subscriber.onConnected();
        assertEquals("onConnected", ShadowToast.getTextOfLatestToast());
        subscriber.onResponse("[1, 2, 3]", 0);
        assertEquals("[1, 2, 3]", tvReceive.getText());
        subscriber.onDisconnected(0);
        assertEquals("onDisConnected", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testUI() {
        assertNotNull(btnConnect);
        assertNotNull(btnDisConnect);
        btnConnect.performClick();
        btnDisConnect.performClick();
    }

    public class TestSocketSubscriber extends SocketSubscriber {

        @Override
        public void onConnected() {
            Toast.makeText(activity, "onConnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(long timePassed) {
            Toast.makeText(activity, "onDisconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnectedWithError(@NotNull Throwable throwable, long timePassed) {
            Toast.makeText(activity, "onDisconnectedWithError", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResponse(@NotNull String data, long timePassed) {
            tvReceive.setText(data);
        }
    }

}