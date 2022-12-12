package jason.he.cordova.douyin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.URLUtil;

import com.bytedance.sdk.open.aweme.base.ImageObject;
import com.bytedance.sdk.open.aweme.base.MediaContent;
import com.bytedance.sdk.open.aweme.base.VideoObject;
import com.bytedance.sdk.open.aweme.share.Share;
import com.bytedance.sdk.open.douyin.DouYinOpenConfig;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.bytedance.sdk.open.douyin.DouYinOpenApiFactory;
import com.bytedance.sdk.open.douyin.api.DouYinOpenApi;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Douyin extends CordovaPlugin {

    public static final String TAG = "Cordova.Plugin.Douyin";

    public static final String PREFS_NAME = "Cordova.Plugin.Douyin";
    public static final String DOUYINAPPID_PROPERTY_KEY = "DOUYINAPPID";

    public static final String ERROR_DOUYIN_NOT_INSTALLED = "未安装抖音";
    public static final String ERROR_INVALID_PARAMETERS = "参数格式错误";
    public static final String ERROR_SEND_REQUEST_FAILED = "分享失败";
    public static final String ERROR_WECHAT_RESPONSE_COMMON = "普通错误";
    public static final String ERROR_WECHAT_RESPONSE_USER_CANCEL = "用户点击取消并返回";
    public static final String ERROR_WECHAT_RESPONSE_SENT_FAILED = "发送失败";
    public static final String ERROR_WECHAT_RESPONSE_AUTH_DENIED = "授权失败";
    public static final String ERROR_WECHAT_RESPONSE_UNSUPPORT = "抖音不支持";
    public static final String ERROR_WECHAT_RESPONSE_UNKNOWN = "未知错误";

    public static final String EXTERNAL_STORAGE_IMAGE_PREFIX = "external://";

    public static final String KEY_ARG_MESSAGE = "message";
    public static final String KEY_ARG_SCENE = "scene";
    public static final String KEY_ARG_TEXT = "text";
    public static final String KEY_ARG_MESSAGE_TITLE = "title";
    public static final String KEY_ARG_MESSAGE_IMAGEURLS = "imageUrls";
    public static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
    public static final String KEY_ARG_MESSAGE_THUMB = "thumb";
    public static final String KEY_ARG_MESSAGE_MEDIA = "media";
    public static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
    public static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "webpageUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_IMAGE = "image";
    public static final String KEY_ARG_MESSAGE_MEDIA_TEXT = "text";
    public static final String KEY_ARG_MESSAGE_MEDIA_MUSICURL = "musicUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_MUSICDATAURL = "musicDataUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_VIDEOURL = "videoUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_FILE = "file";
    public static final String KEY_ARG_MESSAGE_MEDIA_EMOTION = "emotion";
    public static final String KEY_ARG_MESSAGE_MEDIA_EXTINFO = "extInfo";
    public static final String KEY_ARG_MESSAGE_MEDIA_URL = "url";

    public static final int TYPE_IMAGE_SINGLE = 1;
    public static final int TYPE_IMAGE_MULTI = 2;
    public static final int TYPE_VIDEO_SINGLE = 3;
    public static final int TYPE_VIDEO_MULTI = 4;
    public static final int TYPE_MIX = 5;

    public static final int SCENE_EDIT = 0;
    public static final int SCENE_PUBLISH = 1;

    public static final int MAX_THUMBNAIL_SIZE = 320;

    protected static CallbackContext currentCallbackContext;
    protected static IWXAPI wxAPI;
    protected static DouYinOpenApi douYinOpenApi;

    protected static String appId;

    protected static CordovaPreferences douyin_preferences;
    protected static CordovaArgs currentArgs = null;
    protected static String currentAction = null;

    @Override
    protected void pluginInitialize() {

        super.pluginInitialize();

        String id = getAppId(preferences);

        // save app id
        saveAppId(cordova.getActivity(), id);

        // init api
        initDouYinAPI(id);

        Log.d(TAG, "plugin initialized.");
    }

    protected void initDouYinAPI(String clientkey) {
        DouYinOpenApiFactory.init(new DouYinOpenConfig(clientkey));
        // douYinOpenApi = DouYinOpenApiFactory.create(cordova.getActivity());
        if (douyin_preferences == null) {
            douyin_preferences = preferences;
        }
    }

    /**
     * Get weixin api
     *
     * @param ctx
     * @return
     */
    public DouYinOpenApi getDouYinOpenAPI(Context ctx) {
        if (douYinOpenApi == null) {
            String appId = getSavedAppId(ctx);
            if (!appId.isEmpty()) {
                douYinOpenApi = DouYinOpenApiFactory.create(cordova.getActivity());
            }
        }
        return douYinOpenApi;
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, String.format("%s is called. Callback ID: %s.", action, callbackContext.getCallbackId()));
        this.currentArgs = args;
        this.currentAction = action;
        currentCallbackContext = callbackContext;
        if (PermissionHelper.hasPermission(this, WRITE_EXTERNAL_STORAGE)) {
            Log.d("execute", "Permissions already granted, or Android version is lower than 6");
            if (action.equals("share")) { // 抖音分享
                return share(args, callbackContext);
            } else if (action.equals("sendAuthRequest")) { // 发送权限验证
                return sendAuthRequest(args, callbackContext);
            } else if (action.equals("sendPaymentRequest")) { //
            } else if (action.equals("isDouyinAppInstalled")) { // 查看抖音是否已经安装
                return isInstalled(callbackContext);
            } else if (action.equals("chooseInvoiceFromWX")) {
            }
        } else {
            Log.d("SaveImageGallery", "Requesting permissions for WRITE_EXTERNAL_STORAGE");
            PermissionHelper.requestPermission(this, 1000, WRITE_EXTERNAL_STORAGE);
        }

        return false;
    }

    protected boolean share(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        final DouYinOpenApi api = getDouYinOpenAPI(cordova.getActivity());
        Share.Request request = new Share.Request();
        // check if installed
        if (!api.isAppInstalled()) {
            callbackContext.error(ERROR_DOUYIN_NOT_INSTALLED);
            return true;
        }
        //
        // check if # of arguments is correct
        final JSONObject params;
        try {
            params = args.getJSONObject(0);
        } catch (JSONException e) {
            callbackContext.error(ERROR_INVALID_PARAMETERS);
            return true;
        }

        JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
        MediaContent mediaContent = new MediaContent();
        ArrayList<String> mUri = new ArrayList<>();
        JSONArray tagsArr = message.getJSONArray(KEY_ARG_MESSAGE_IMAGEURLS);
        String contentPath;
        if (tagsArr != null) {
            try {
                for (int i = 0; i < tagsArr.length(); i++) {
                    File file = Util.downloadAndCacheFile(webView.getContext(), tagsArr.getString(i));
                    if (file == null) {
                        Log.d(TAG, String.format("File could not be downloaded from %s.", tagsArr.getString(i)));
                        break;
                    }
                    String url = file.getAbsolutePath();
                    contentPath = Util.getFileUri(webView.getContext(), file);

                    mUri.add(contentPath);
                    // mUri.add(url);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to add absolutePath.", e);
                callbackContext.error(ERROR_SEND_REQUEST_FAILED);
                return true;
            }
        }
        // 携带话题
        ArrayList<String> hashtags = new ArrayList<>();
        JSONArray hashArr = message.getJSONArray(KEY_ARG_MESSAGE_TITLE);
        if (hashArr != null) {
            for (int i = 0; i < hashArr.length(); i++) {
                hashtags.add(hashArr.getString(i));
            }
        }
        request.mHashTagList = hashtags;

        int type = message.has(KEY_ARG_MESSAGE_MEDIA_TYPE) ? message.getInt(KEY_ARG_MESSAGE_MEDIA_TYPE)
                : TYPE_VIDEO_SINGLE;
        switch (type) {
        case TYPE_IMAGE_SINGLE:
        case TYPE_IMAGE_MULTI:
            ImageObject imageObject = new ImageObject();
            imageObject.mImagePaths = mUri;
            mediaContent.mMediaObject = imageObject;
            break;
        case TYPE_VIDEO_SINGLE:
        case TYPE_VIDEO_MULTI:
            VideoObject videoObject = new VideoObject();
            videoObject.mVideoPaths = mUri;
            mediaContent.mMediaObject = videoObject;
            break;
        default:
            break;
        }

        request.mMediaContent = mediaContent;

        int scene = message.has(KEY_ARG_SCENE) ? message.getInt(KEY_ARG_SCENE) : SCENE_EDIT;
        if (scene == SCENE_PUBLISH && douYinOpenApi.isAppSupportShareToPublish()) {
            request.shareToPublish = true;
        }

        douYinOpenApi.share(request);
        //
        // final SendMessageToWX.Req req = new SendMessageToWX.Req();
        // req.transaction = buildTransaction();
        //
        // if (params.has(KEY_ARG_SCENE)) {
        // switch (params.getInt(KEY_ARG_SCENE)) {
        // case SCENE_FAVORITE:
        // req.scene = SendMessageToWX.Req.WXSceneFavorite;
        // break;
        // case SCENE_TIMELINE:
        // req.scene = SendMessageToWX.Req.WXSceneTimeline;
        // break;
        // case SCENE_SESSION:
        // req.scene = SendMessageToWX.Req.WXSceneSession;
        // break;
        // default:
        // req.scene = SendMessageToWX.Req.WXSceneTimeline;
        // }
        // } else {
        // req.scene = SendMessageToWX.Req.WXSceneTimeline;
        // }

        // run in background
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    //// req.message = buildSharingMessage(params);
                    currentArgs = null;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to build sharing message.", e);

                    // clear callback context
                    currentCallbackContext = null;
                    currentArgs = null;

                    // send json exception error
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                }

                // if (api.sendReq(req)) {
                // Log.i(TAG, "Message has been sent successfully.");
                // } else {
                // Log.i(TAG, "Message has been sent unsuccessfully.");
                //
                // // clear callback context
                // currentCallbackContext = null;
                //
                // // send error
                // callbackContext.error(ERROR_SEND_REQUEST_FAILED);
                // }
            }
        });

        // send no result
        sendNoResultPluginResult(callbackContext);

        return true;
    }

    protected boolean sendAuthRequest(CordovaArgs args, CallbackContext callbackContext) {
        final DouYinOpenApi api = getDouYinOpenAPI(cordova.getActivity());
        // final IWXAPI api = getDouYinOpenAPI(cordova.getActivity());

        // final SendAuth.Req req = new SendAuth.Req();
        // try {
        // req.scope = args.getString(0);
        // req.state = args.getString(1);
        // } catch (JSONException e) {
        // Log.e(TAG, e.getMessage());
        //
        // req.scope = "snsapi_userinfo";
        // req.state = "wechat";
        // }
        //
        // if (api.sendReq(req)) {
        // Log.i(TAG, "Auth request has been sent successfully.");
        //
        // // send no result
        // sendNoResultPluginResult(callbackContext);
        // } else {
        // Log.i(TAG, "Auth request has been sent unsuccessfully.");
        //
        // // send error
        // callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        // }

        return true;
    }

    protected boolean sendPaymentRequest(CordovaArgs args, CallbackContext callbackContext) {

        // check if # of arguments is correct
        // final JSONObject params;
        // try {
        // params = args.getJSONObject(0);
        // } catch (JSONException e) {
        // callbackContext.error(ERROR_INVALID_PARAMETERS);
        // return true;
        // }
        //
        // PayReq req = new PayReq();
        //
        // try {
        // final String appid = params.getString("appid");
        // final String savedAppid = getAppId(preferences);
        // if (!savedAppid.equals(appid)) {
        // this.saveAppId(cordova.getActivity(), appid);
        // }
        //
        // req.appId = appid;
        // req.partnerId = params.has("mch_id") ? params.getString("mch_id") :
        // params.getString("partnerid");
        // req.prepayId = params.has("prepay_id") ? params.getString("prepay_id") :
        // params.getString("prepayid");
        // req.nonceStr = params.has("nonce") ? params.getString("nonce") :
        // params.getString("noncestr");
        // req.timeStamp = params.getString("timestamp");
        // req.sign = params.getString("sign");
        // req.packageValue = "Sign=WXPay";
        // } catch (Exception e) {
        // Log.e(TAG, e.getMessage());
        //
        // callbackContext.error(ERROR_INVALID_PARAMETERS);
        // return true;
        // }
        //
        // final IWXAPI api = getDouYinOpenAPI(cordova.getActivity());
        //
        // if (api.sendReq(req)) {
        // Log.i(TAG, "Payment request has been sent successfully.");
        //
        // // send no result
        // sendNoResultPluginResult(callbackContext);
        // } else {
        // Log.i(TAG, "Payment request has been sent unsuccessfully.");
        //
        // // send error
        // callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        // }

        return true;
    }

    protected boolean chooseInvoiceFromWX(CordovaArgs args, CallbackContext callbackContext) {

        // final IWXAPI api = getDouYinOpenAPI(cordova.getActivity());
        final DouYinOpenApi api = getDouYinOpenAPI(cordova.getActivity());
        // // check if # of arguments is correct
        // final JSONObject params;
        // try {
        // params = args.getJSONObject(0);
        // } catch (JSONException e) {
        // callbackContext.error(ERROR_INVALID_PARAMETERS);
        // return true;
        // }
        //
        // ChooseCardFromWXCardPackage.Req req = new ChooseCardFromWXCardPackage.Req();
        //
        // try {
        // req.appId = getAppId(preferences);
        // req.cardType = "INVOICE";
        // req.signType = params.getString("signType");
        // req.cardSign = params.getString("cardSign");
        // req.nonceStr = params.getString("nonceStr");
        // req.timeStamp = params.getString("timeStamp");
        // req.canMultiSelect = "1";
        // } catch (Exception e) {
        // Log.e(TAG, e.getMessage());
        //
        // callbackContext.error(ERROR_INVALID_PARAMETERS);
        // return true;
        // }
        //
        // if (api.sendReq(req)) {
        // Log.i(TAG, "Invoice request has been sent successfully.");
        //
        // // send no result
        // sendNoResultPluginResult(callbackContext);
        // } else {
        // Log.i(TAG, "Invoice request has been sent unsuccessfully.");
        //
        // // send error
        // callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        // }

        return true;
    }

    protected boolean isInstalled(CallbackContext callbackContext) {
        final DouYinOpenApi api = getDouYinOpenAPI(cordova.getActivity());
        if (!api.isAppInstalled()) {
            callbackContext.success(0);
        } else {
            callbackContext.success(1);
        }
        return true;
    }

    /**
     * Get input stream from a url
     *
     * @param url
     * @return
     */
    protected InputStream getFileInputStream(String url) {
        try {

            InputStream inputStream = null;

            if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {

                File file = Util.downloadAndCacheFile(webView.getContext(), url);

                if (file == null) {
                    Log.d(TAG, String.format("File could not be downloaded from %s.", url));
                    return null;
                }

                url = file.getAbsolutePath();
                inputStream = new FileInputStream(file);

                Log.d(TAG, String.format("File was downloaded and cached to %s.", url));

            } else if (url.startsWith("data:image")) { // base64 image

                String imageDataBytes = url.substring(url.indexOf(",") + 1);
                byte imageBytes[] = Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT);
                inputStream = new ByteArrayInputStream(imageBytes);

                Log.d(TAG, "Image is in base64 format.");

            } else if (url.startsWith(EXTERNAL_STORAGE_IMAGE_PREFIX)) { // external path

                url = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + url.substring(EXTERNAL_STORAGE_IMAGE_PREFIX.length());
                inputStream = new FileInputStream(url);

                Log.d(TAG, String.format("File is located on external storage at %s.", url));

            } else if (!url.startsWith("/")) { // relative path

                inputStream = cordova.getActivity().getApplicationContext().getAssets().open(url);

                Log.d(TAG, String.format("File is located in assets folder at %s.", url));

            } else {

                inputStream = new FileInputStream(url);

                Log.d(TAG, String.format("File is located at %s.", url));

            }

            return inputStream;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getAppId(CordovaPreferences f_preferences) {
        if (appId == null) {
            if (f_preferences != null) {
                appId = f_preferences.getString(DOUYINAPPID_PROPERTY_KEY, "");
            } else if (douyin_preferences != null) {
                appId = douyin_preferences.getString(DOUYINAPPID_PROPERTY_KEY, "");
            }
        }
        return appId;
    }

    /**
     * Get saved app id
     *
     * @param ctx
     * @return
     */
    public static String getSavedAppId(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(DOUYINAPPID_PROPERTY_KEY, "");
    }

    /**
     * Save app id into SharedPreferences
     *
     * @param ctx
     * @param id
     */
    public static void saveAppId(Context ctx, String id) {
        if (id == null || id.isEmpty()) {
            return;
        }

        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DOUYINAPPID_PROPERTY_KEY, id);
        editor.commit();
    }

    public static CallbackContext getCurrentCallbackContext() {
        return currentCallbackContext;
    }

    private void sendNoResultPluginResult(CallbackContext callbackContext) {
        // save current callback context
        currentCallbackContext = callbackContext;

        // send no result and keep callback
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(Douyin.TAG, "Permission not granted by the user");
                currentCallbackContext.error("Permissions denied");
                return;
            }
        }

        switch (requestCode) {
        case 1000:
            Log.d(Douyin.TAG, "User granted the permission for WRITE_EXTERNAL_STORAGE");
            if (currentArgs != null) {
                if (this.currentAction.equals("share")) { // 抖音分享
                    share(currentArgs, getCurrentCallbackContext());
                } else if (this.currentAction.equals("sendAuthRequest")) { // 发送权限验证
                    sendAuthRequest(currentArgs, getCurrentCallbackContext());
                } else if (this.currentAction.equals("sendPaymentRequest")) { //
                } else if (this.currentAction.equals("isDouyinAppInstalled")) { // 查看抖音是否已经安装
                    isInstalled(getCurrentCallbackContext());
                } else if (this.currentAction.equals("chooseInvoiceFromWX")) {
                }
            }
            break;
        }
    }

}
