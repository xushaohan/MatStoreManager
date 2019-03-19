package com.eeka.matstoremanager.http;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.App;
import com.eeka.matstoremanager.bo.InStorageInfoBo;
import com.eeka.matstoremanager.bo.UserInfoBo;
import com.eeka.matstoremanager.utils.Logger;
import com.eeka.matstoremanager.utils.NetUtil;
import com.eeka.matstoremanager.utils.SpUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

import cn.finalteam.okhttpfinal.BaseHttpRequestCallback;
import cn.finalteam.okhttpfinal.HttpRequest;
import cn.finalteam.okhttpfinal.LogUtil;
import cn.finalteam.okhttpfinal.Part;
import cn.finalteam.okhttpfinal.RequestParams;
import okhttp3.Headers;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * 网络交互类
 * Created by Lenovo on 2017/5/12.
 */

public class HttpHelper {
    private static final String STATE = "status";
    private static final String MESSAGE = "message";
    private static boolean IS_COOKIE_OUT;
    private static final String COOKIE_OUT = "SecurityException: Authorization failed.";//cookie过期
    private static String PAD_IP;

    private static String BASE_URL = App.BASE_URL;

    public static final String login_url = BASE_URL + "login?";
    public static final String logout_url = BASE_URL + "logout?";
    public static final String loginByCard_url = BASE_URL + "loginByCard?";
    public static final String positionLogin_url = BASE_URL + "position/positionLogin?";
    public static final String positionLogout_url = BASE_URL + "position/positionLogout?";
    public static final String getPositionLoginUser_url = BASE_URL + "position/getPositionLoginUser?";
    public static final String queryPositionByPadIp_url = BASE_URL + "position/getPositionContext?";
    public static final String findProcessWithPadId_url = BASE_URL + "cutpad/findPadBindOperations?";
    public static final String getCardInfo = BASE_URL + "cutpad/cardRecognition?";
    public static final String getWareHouseMessage = BASE_URL + "wareHouse/getWareHouseMessage?";
    public static final String inStorage = BASE_URL + "wareHouse/WareHouseIn?";
    private static Context mContext;

    private static HttpRequest.HttpRequestBo mCookieOutRequest;//记录cookie过期的请求，用于重新登录后再次请求

    static {
        mContext = App.mContext;
    }

    /**
     * 入库，调用此方法必须有员工登录在岗
     */
    public static void inStorage(InStorageInfoBo info, HttpCallback callback) {
        RequestParams params = getBaseParams();
        JSONObject json = new JSONObject();
        json.put("CLOTH_TYPE", info.getCLOTH_TYPE());
        json.put("STOR_AREA", info.getSTOR_AREA());
        json.put("STOR_LOCATION", info.getSTOR_LOCATION());
        json.put("SHOP_ORDER", info.getSHOP_ORDER());
        json.put("WORK_CENTER", info.getWORK_CENTER());
        json.put("ITEM", info.getITEM());
        json.put("RFID", info.getRFID());
        json.put("SIZE", info.getSIZE());
        json.put("QUANTITY", info.getQUANTITY());
        List<UserInfoBo> users = SpUtil.getPositionUsers();
        json.put("USER_ID", users.get(0).getEMPLOYEE_NUMBER());
        params.put("params",json.toJSONString());
        HttpRequest.post(inStorage, params, getResponseHandler(inStorage, callback));
    }

    /**
     * 获取库位信息
     */
    public static void getWareHouseMessage(String storageArea, String storageLocation, String type, String rfid, HttpCallback callback) {
        RequestParams params = getBaseParams();
        JSONObject json = new JSONObject();
        json.put("STOR_AREA", storageArea);
        json.put("STOR_LOCATION", storageLocation);
        json.put("CLOTH_TYPE", type);
        json.put("RFID", rfid);
        params.put("params", json.toJSONString());
        HttpRequest.post(getWareHouseMessage, params, getResponseHandler(getWareHouseMessage, callback));
    }

    /**
     * 根据RFID卡号获取信息、定制订单/批量订单/员工
     *
     * @param cardId 卡号
     */
    public static void getCardInfo(String cardId, HttpCallback callback) {
        JSONObject json = new JSONObject();
        json.put("PAD_ID", PAD_IP);
        json.put("RFID", cardId);
        RequestParams params = getBaseParams();
        params.put("params", json.toJSONString());
        HttpRequest.post(getCardInfo, params, getResponseHandler(getCardInfo, callback));
    }

    /**
     * 根据PAD的IP地址查询站点的相关信息
     */
    public static void initData(HttpCallback callback) {
        RequestParams params = getBaseParams();
        JSONObject json = new JSONObject();
        json.put("PAD_IP", PAD_IP);
        params.put("params", json.toJSONString());
        HttpRequest.post(queryPositionByPadIp_url, params, getResponseHandler(queryPositionByPadIp_url, callback));
    }

    /**
     * 查询当前平板绑定的工序
     */
    public static void findProcessWithPadId(HttpCallback callback) {
        JSONObject json = new JSONObject();
        json.put("PAD_ID", PAD_IP);
        RequestParams params = getBaseParams();
        params.put("params", json.toJSONString());
        HttpRequest.post(findProcessWithPadId_url, params, getResponseHandler(findProcessWithPadId_url, callback));
    }


    /**
     * 登录
     *
     * @param user     账号
     * @param pwd      密码
     * @param callback 回调
     */
    public static void login(String user, String pwd, HttpCallback callback) {
        RequestParams params = new RequestParams();
        params.put("j_username", user);
        params.put("j_password", pwd);
        HttpRequest.post(login_url, params, getResponseHandler(login_url, callback));
    }

    /**
     * 卡号密码登录
     *
     * @param cardId   卡号
     * @param pwd      密码
     * @param callback 回调
     */
    public static void loginByCard(String cardId, String pwd, HttpCallback callback) {
        RequestParams params = new RequestParams();
        params.put("cardId", cardId);
        params.put("passwd", pwd);
        HttpRequest.post(loginByCard_url, params, getResponseHandler(loginByCard_url, callback));
    }

    /**
     * 登出
     */
    public static void logout(HttpCallback callback) {
        RequestParams params = getBaseParams();
        HttpRequest.post(logout_url, params, getResponseHandler(logout_url, callback));
    }

    /**
     * 站位登录
     *
     * @param cardId   卡号
     * @param callback 回调
     */
    public static void positionLogin(String cardId, HttpCallback callback) {
        JSONObject json = new JSONObject();
        json.put("PAD_IP", PAD_IP);
        json.put("CARD_ID", cardId);
        RequestParams params = getBaseParams();
        params.put("params", json.toJSONString());
        HttpRequest.post(positionLogin_url, params, getResponseHandler(positionLogin_url, callback));
    }

    /**
     * 站点登出
     *
     * @param cardId   卡号
     * @param callback 回调
     */
    public static void positionLogout(String cardId, HttpCallback callback) {
        RequestParams params = getBaseParams();
        JSONObject json = new JSONObject();
        json.put("PAD_IP", PAD_IP);
        json.put("CARD_ID", cardId);
        params.put("params", json.toJSONString());
        HttpRequest.post(positionLogout_url, params, getResponseHandler(positionLogout_url, callback));
    }

    /**
     * 获取当前站位登录的人员
     */
    public static void getPositionLoginUsers(HttpCallback callback) {
        RequestParams params = getBaseParams();
        JSONObject json = new JSONObject();
        json.put("PAD_IP", PAD_IP);
        params.put("params", json.toJSONString());
        HttpRequest.post(getPositionLoginUser_url, params, getResponseHandler(getPositionLoginUser_url, callback));
    }

    /**
     * 获取固定请求参数<br>
     */
    private static RequestParams getBaseParams() {
        PAD_IP = getPadIp();
        RequestParams params = new RequestParams();
        params.put("PAD_IP", PAD_IP);
        String site = SpUtil.getSite();
        if (!TextUtils.isEmpty(site)) {
            params.put("site", site);
        }
        String cookie = SpUtil.getCookie();
        if (!TextUtils.isEmpty(cookie)) {
            params.addHeader("Cookie", cookie);
        }
        return params;
    }

    public static String getPadIp() {
        PAD_IP = NetUtil.getHostIP();
        return PAD_IP;
    }

    public static boolean isSuccess(JSONObject json) {
        return "Y".equals(json.getString(STATE));
    }

    public static String getMessage(JSONObject json) {
        return json.getString(MESSAGE);
    }

    public static String getResultStr(JSONObject json) {
        JSONObject result = json.getJSONObject("result");
        if (result != null && !result.isEmpty())
            return result.toString();
        return null;
    }

    /**
     * 登录过期后重新登录
     */
    private static void cookieOutReLogin(HttpCallback callback) {
        IS_COOKIE_OUT = true;

        //记录cookie失效前的请求，重新登录后重新请求
        HttpRequest.HttpRequestBo lastRequest = HttpRequest.mLastRequest;
        mCookieOutRequest = new HttpRequest.HttpRequestBo();
        mCookieOutRequest.setMethod(lastRequest.getMethod());
        mCookieOutRequest.setUrl(lastRequest.getUrl());
        mCookieOutRequest.setParams(lastRequest.getParams());
        mCookieOutRequest.setCallback(lastRequest.getCallback());

        UserInfoBo loginUser = SpUtil.getLoginUser();
        login(loginUser.getUSER(), loginUser.getPassword(), callback);
    }

    /**
     * 获取请求响应Handler
     */
    private static BaseHttpRequestCallback getResponseHandler(final String url, final HttpCallback callback) {
        BaseHttpRequestCallback response = new BaseHttpRequestCallback<JSONObject>() {

            @Override
            public void onFailure(int errorCode, String msg) {
                super.onFailure(errorCode, msg);
                if (!TextUtils.isEmpty(msg) && msg.contains("type=\"submit\" name=\"uidPasswordLogon\"")) {
                    //后台返回登录页面，重新登录
                    cookieOutReLogin(callback);
                } else if (callback != null) {
                    //无网络或者后台出错
                    callback.onFailure(url, errorCode, msg);
                }
            }

            @Override
            protected void onSuccess(Headers headers, JSONObject jsonObject) {
                super.onSuccess(headers, jsonObject);
                boolean success = isSuccess(jsonObject);
                if (!success) {
                    String message = jsonObject.getString("message");
                    LogUtil.writeToFile(LogUtil.LOGTYPE_HTTPFAIL, url + "\n" + message);
                }
                //登录的时候保存cookie
                if (login_url.contains(url)) {
                    if (headers != null) {
                        StringBuilder cookies = new StringBuilder();
                        List<String> values = headers.values("set-cookie");
                        for (String cookie : values) {
                            cookies.append(cookie).append(";");
                        }
                        if (!TextUtils.isEmpty(cookies)) {
                            SpUtil.saveCookie(cookies.substring(0, cookies.lastIndexOf(";")));
                        }
                    }
                    if (IS_COOKIE_OUT) {
                        if (success) {
                            IS_COOKIE_OUT = false;
                            //cookie过期后重新登录成功，继续执行之前的请求
                            if (mCookieOutRequest != null) {
                                RequestParams params = getBaseParams();
                                RequestParams lastParams = mCookieOutRequest.getParams();
                                List<Part> formParams = lastParams.getFormParams();
                                for (Part p : formParams) {
                                    params.put(p.getKey(), p.getValue());
                                }
                                HttpRequest.post(mCookieOutRequest.getUrl(), params, mCookieOutRequest.getCallback());
                            }
                            return;
                        }
                    }
                } else if (!success) {
                    String message = jsonObject.getString("message");
                    if (!TextUtils.isEmpty(message)) {
                        if (message.contains(COOKIE_OUT)) {//cookie失效，重新登录获取新的cookie
                            cookieOutReLogin(callback);
                            return;
                        }
                    }
                }
                if (callback != null)
                    callback.onSuccess(url, jsonObject);
            }

            @Override
            public void onResponse(Response httpResponse, String response, Headers headers) {
                super.onResponse(httpResponse, response, headers);
                if (!NetUtil.isNetworkAvalible(mContext)) {
                    Toast.makeText(mContext, "网络不可用，请检查网络设置", Toast.LENGTH_SHORT).show();
                    return;
                } else if (response == null) {
                    Toast.makeText(mContext, "连接错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                RequestBody body = httpResponse.request().body();
                Buffer buffer = new Buffer();
                try {
                    body.writeTo(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buffer.flush();
                buffer.close();
                String s = buffer.readString(Charset.forName("UTF-8"));
                String reqParams = URLDecoder.decode(s);
                LogUtil.writeToFile(LogUtil.LOGTYPE_HTTPRESPONSE, url + reqParams + "\n       " + response);
                Logger.d(response);
            }
        };
        return response;
    }

}
