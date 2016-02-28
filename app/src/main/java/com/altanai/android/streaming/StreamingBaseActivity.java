package com.altanai.android.streaming;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.StreamingProfile;

import org.json.JSONException;
import org.json.JSONObject;

public class StreamingBaseActivity extends Activity implements CameraStreamingManager.StreamingStateListener {

    private static final String TAG = "StreamingBaseActivity";

    protected Button mShutterButton;
    protected boolean mShutterButtonPressed = false;

    protected static final int MSG_UPDATE_SHUTTER_BUTTON_STATE = 0;
    protected String mStatusMsgContent;
    protected TextView mSatusTextView;

    protected CameraStreamingManager mCameraStreamingManager;

    protected JSONObject mJSONObject;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SHUTTER_BUTTON_STATE:
                    if (!mShutterButtonPressed) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // disable the shutter button before startStreaming
                                setShutterButtonEnabled(false);
                                boolean res = mCameraStreamingManager.startStreaming();
                                mShutterButtonPressed = true;
                                Log.i(TAG, "res:" + res);
                                if (!res) {
                                    mShutterButtonPressed = false;
                                    setShutterButtonEnabled(true);
                                }
                                setShutterButtonPressed(mShutterButtonPressed);
                            }
                        }).start();
                    } else {
                        // disable the shutter button before stopStreaming
                        setShutterButtonEnabled(false);
                        mCameraStreamingManager.stopStreaming();
                        setShutterButtonPressed(false);
                    }
                    break;
                default:
                    Log.e(TAG, "Invalid message");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        String title="streamName";
        String streamJsonStrFromServer=
                "{ \"id\": \"altanai\", " +
                        "\"hub\": \"live\"," +
                        "\"title\": \""+title+"\", " +
                        "\"publishKey\": \"858574776476476\",  " +
                        "\"publishSecurity\": \"dynamic\",  " +
                        "\"hosts\" : " +
                        "{ \"publish\" : { \"rtmp\"   : \"domain.com/live/\"  }," +
                        "\"play\" : {  \"hls\"    : \"xxx.hls1.domain.com\",  \"rtmp\"   :\"xxx.live1.domain.com\"  }  } }";

        JSONObject mJSONObject = null;
        try {
            mJSONObject = new JSONObject(streamJsonStrFromServer);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        StreamingProfile.Stream stream = new StreamingProfile.Stream(mJSONObject);

        StreamingProfile profile = new StreamingProfile();
        profile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_MEDIUM1)
                .setAudioQuality(StreamingProfile.AUDIO_QUALITY_HIGH2)
                .setStream(stream);

        CameraStreamingSetting setting = new CameraStreamingSetting();
        setting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setContinuousFocusModeEnabled(true)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_4_3);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraStreamingManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShutterButtonPressed = false;
        mCameraStreamingManager.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraStreamingManager.onDestroy();
    }

    @Override
    public void onStateChanged(final int state, Object extra) {
        Log.i(TAG, "onStateChanged state:" + state);
        switch (state) {
            case CameraStreamingManager.STATE.PREPARING:
                mStatusMsgContent = getString(R.string.string_state_preparing);
                break;
            case CameraStreamingManager.STATE.READY:
                mStatusMsgContent = getString(R.string.string_state_ready);
                // start streaming when READY
                onShutterButtonClick();
                break;
            case CameraStreamingManager.STATE.CONNECTING:
                mStatusMsgContent = getString(R.string.string_state_connecting);
                break;
            case CameraStreamingManager.STATE.STREAMING:
                mStatusMsgContent = getString(R.string.string_state_streaming);
                setShutterButtonEnabled(true);
                break;
            case CameraStreamingManager.STATE.SHUTDOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(false);
                break;
            case CameraStreamingManager.STATE.IOERROR:
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                break;
            case CameraStreamingManager.STATE.NETBLOCKING:
                mStatusMsgContent = getString(R.string.string_state_netblocking);
                break;
            case CameraStreamingManager.STATE.CONNECTION_TIMEOUT:
                mStatusMsgContent = getString(R.string.string_state_con_timeout);
                break;
            case CameraStreamingManager.STATE.UNKNOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_EMPTY:
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_FULL:
                break;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSatusTextView.setText(mStatusMsgContent);
            }
        });
    }

    @Override
    public boolean onStateHandled(final int state, Object extra) {
        Log.i(TAG, "onStateHandled state:" + state);
        return false;
    }

    protected void setShutterButtonPressed(final boolean pressed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButtonPressed = pressed;
                mShutterButton.setPressed(pressed);
            }
        });
    }

    protected void setShutterButtonEnabled(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButton.setFocusable(enable);
                mShutterButton.setClickable(enable);
                mShutterButton.setEnabled(enable);
            }
        });
    }

    protected void onShutterButtonClick() {
        mHandler.removeMessages(MSG_UPDATE_SHUTTER_BUTTON_STATE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_SHUTTER_BUTTON_STATE), 50);
    }
}