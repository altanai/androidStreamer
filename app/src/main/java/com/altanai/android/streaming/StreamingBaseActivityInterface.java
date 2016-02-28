package com.altanai.android.streaming;

import com.pili.pldroid.streaming.CameraStreamingManager;

import org.json.JSONObject;

/**
 * Created by altanaibisht on 27/2/16.
 */
public interface StreamingBaseActivityInterface extends  CameraStreamingManager.StreamingStateListener {

     static final String TAG = "StreamingBaseActivity";

     CameraStreamingManager mCameraStreamingManager = null;

     JSONObject mJSONObject = null;


}
