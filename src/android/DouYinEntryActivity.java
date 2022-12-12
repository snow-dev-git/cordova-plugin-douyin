package __PACKAGE_NAME__;

import android.app.Activity;

import android.os.Bundle;
import android.widget.Toast;

import android.content.Intent;

import com.bytedance.sdk.open.aweme.CommonConstants;
import com.bytedance.sdk.open.aweme.authorize.model.Authorization;
import com.bytedance.sdk.open.aweme.common.handler.IApiEventHandler;
import com.bytedance.sdk.open.aweme.common.model.BaseReq;
import com.bytedance.sdk.open.aweme.common.model.BaseResp;
import com.bytedance.sdk.open.aweme.share.Share;
import com.bytedance.sdk.open.douyin.DouYinOpenApiFactory;
import com.bytedance.sdk.open.douyin.api.DouYinOpenApi;

/**
 * 主要功能：接受授权返回结果的activity
 * <p>
 * <p>
 * 也可通过request.callerLocalEntry = "com.xxx.xxx...activity"; 定义自己的回调类
 */
public class DouYinEntryActivity extends Activity implements IApiEventHandler {

    DouYinOpenApi douYinOpenApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        douYinOpenApi = DouYinOpenApiFactory.create(this);
        douYinOpenApi.handleIntent(getIntent(), this);
    }

    @Override
    public void onReq(BaseReq req) {

    }

    @Override
    public void onResp(BaseResp resp) {
        // 授权成功可以获得authCode

        if (resp.getType() == CommonConstants.ModeType.SHARE_CONTENT_TO_TT_RESP) {
            Share.Response response = (Share.Response) resp;
            if (response.errorCode == 0) {
                Toast.makeText(this, "分享成功", Toast.LENGTH_SHORT).show();
            } else if (response.errorCode == -2) {
                Toast.makeText(this, "分享取消", Toast.LENGTH_SHORT).show();
            }
            // Toast.makeText(this, "分享失败,errorCode: " + response.errorCode + "subcode" +
            // response.subErrorCode + " Error Msg : " + response.errorMsg,
            // Toast.LENGTH_SHORT).show();
            // 分享成功之后跳转的页面
            // Intent intent = new Intent(this, MainActivity.class);
            // startActivity(intent);
            // 分享成功之后直接结束当前activity
            finish();
        } else if (resp.getType() == CommonConstants.ModeType.SEND_AUTH_RESPONSE) {
            Authorization.Response response = (Authorization.Response) resp;
            if (resp.isSuccess()) {
                Toast.makeText(this, "授权成功，获得权限：" + response.grantedPermissions, Toast.LENGTH_LONG).show();
                // getAccessToken(response.authCode);
            } else {
                Toast.makeText(this, "授权失败" + response.grantedPermissions, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    @Override
    public void onErrorIntent(Intent intent) {
        // 错误数据
        Toast.makeText(this, "intent出错啦", Toast.LENGTH_LONG).show();
        finish();
    }
}