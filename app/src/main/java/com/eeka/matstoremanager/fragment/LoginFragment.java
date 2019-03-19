package com.eeka.matstoremanager.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.bo.ContextInfoBo;
import com.eeka.matstoremanager.bo.UserInfoBo;
import com.eeka.matstoremanager.http.HttpHelper;
import com.eeka.matstoremanager.utils.NetUtil;
import com.eeka.matstoremanager.utils.SpUtil;
import com.eeka.matstoremanager.utils.SystemUtils;

import java.util.List;

/**
 * 登录界面
 * Created by Lenovo on 2017/7/6.
 */

public class LoginFragment extends BaseFragment {

    public static final int TYPE_CLOCK = 0;
    public static final int TYPE_LOGIN = 1;

    private EditText mET_IP, mEt_user, mEt_pwd, mEt_site;
    private OnLoginCallback mLoginCallback;
    private OnClockCallback mClockCallback;
    private int mType;

    public void setType(int mType) {
        this.mType = mType;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fm_login, null);
        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initView();
    }

    @Override
    protected void initView() {
        super.initView();
        mET_IP = mView.findViewById(R.id.et_login_IP);
        mET_IP.setText(NetUtil.getHostIP());
        mEt_user = mView.findViewById(R.id.et_login_user);
        mEt_user.requestFocus();
        mEt_pwd = mView.findViewById(R.id.et_login_pwd);
        mEt_site = mView.findViewById(R.id.et_login_site);

        if (SystemUtils.isApkInDebug(mContext)) {
            mET_IP.setEnabled(true);
            mEt_site.setEnabled(true);
        }

        Button btn_done = mView.findViewById(R.id.btn_login);
        btn_done.setOnClickListener(this);
        if (mType == TYPE_LOGIN) {
            TextView tv_alert = mView.findViewById(R.id.tv_login_alert);
            TextView tv_user = mView.findViewById(R.id.tv_login_user_tag);
            tv_alert.setText("请设置系统登录账户");
            tv_user.setText("账号：");
            btn_done.setText("完成");
        } else {
            mEt_user.setInputType(InputType.TYPE_CLASS_NUMBER);
            mView.findViewById(R.id.layout_login_pwd).setVisibility(View.GONE);
        }

        if (mType == TYPE_LOGIN) {
            UserInfoBo loginUser = SpUtil.getLoginUser();
            if (loginUser != null) {
                mEt_user.setText(loginUser.getUSER());
                mEt_pwd.setText(loginUser.getPassword());
            }
        }
        String site = SpUtil.getSite();
        if (!isEmpty(site)) {
            mEt_site.setText(site);
        }

        mView.findViewById(R.id.iv_login_morePosition).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (v.getId() == R.id.btn_login) {
            login();
        }
    }

    private void login() {
        String user = mEt_user.getText().toString();
        if (isEmpty(user)) {
            if (mType == TYPE_LOGIN) {
                toast("请输入账户名");
            } else {
                toast("请输入工号");
            }
            return;
        }
        if (mType == TYPE_LOGIN) {
            String pwd = mEt_pwd.getText().toString();
            if (isEmpty(pwd)) {
                toast("请输入密码");
                return;
            }
            showLoading();
            HttpHelper.login(user, pwd, this);
        } else {
            showLoading();
            HttpHelper.positionLogin(user, this);
        }
    }

    @Override
    public void onSuccess(String url, JSONObject resultJSON) {
        dismissLoading();
        if (HttpHelper.isSuccess(resultJSON)) {
            if (url.contains(HttpHelper.login_url)) {
                toast("系统登录成功");
                UserInfoBo userInfo = JSON.parseObject(HttpHelper.getResultStr(resultJSON), UserInfoBo.class);
                String site = mEt_site.getText().toString();
                SpUtil.saveSite(site);
                String pwd = mEt_pwd.getText().toString();
                userInfo.setPassword(pwd);
                SpUtil.saveLoginUser(userInfo);
                NetUtil.setHostIp(mET_IP.getText().toString());
                if (mLoginCallback != null)
                    mLoginCallback.onLogin(true);
            } else if (HttpHelper.positionLogin_url.equals(url)) {
                toast("用户上岗成功");
                List<UserInfoBo> positionUsers = JSON.parseArray(resultJSON.getJSONArray("result").toString(), UserInfoBo.class);
                SpUtil.savePositionUsers(positionUsers);
                if (mClockCallback != null) {
                    mClockCallback.onClockIn(true);
                }
            } else if (HttpHelper.queryPositionByPadIp_url.equals(url)) {
                ContextInfoBo contextInfoBo = JSON.parseObject(HttpHelper.getResultStr(resultJSON), ContextInfoBo.class);
                SpUtil.saveContextInfo(contextInfoBo);
                List<UserInfoBo> loginUserList = contextInfoBo.getLOGIN_USER_LIST();
                SpUtil.savePositionUsers(loginUserList);
            }
        } else {
            String message = resultJSON.getString("message");
            showErrorDialog(message);
            if (HttpHelper.positionLogin_url.equals(url)) {
                if (mClockCallback != null) {
                    mClockCallback.onClockIn(false);
                }
            }
        }
    }

    @Override
    public void onFailure(String url, int code, String message) {
        super.onFailure(url, code, message);
        if (mClockCallback != null) {
            mClockCallback.onClockIn(false);
        }
    }

    public void setOnLoginCallback(OnLoginCallback callback) {
        mLoginCallback = callback;
    }

    public interface OnLoginCallback {
        /**
         * 上班打卡回调
         *
         * @param success 是否打卡成功
         */
        void onLogin(boolean success);
    }

    public void setOnClockCallback(OnClockCallback callback) {
        mClockCallback = callback;
    }

    public interface OnClockCallback {
        /**
         * 上下班打卡回调
         *
         * @param success 是否打卡成功
         */
        void onClockIn(boolean success);
    }
}
