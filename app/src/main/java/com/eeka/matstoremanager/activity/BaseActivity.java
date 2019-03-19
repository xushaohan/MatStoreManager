package com.eeka.matstoremanager.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.dialog.MyAlertDialog;
import com.eeka.matstoremanager.http.HttpCallback;
import com.eeka.matstoremanager.http.HttpHelper;
import com.eeka.matstoremanager.utils.ToastUtil;

/**
 * Activity基类
 * Created by Lenovo on 2017/5/13.
 */

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity implements View.OnClickListener, HttpCallback {

    protected Context mContext;

    private Dialog mProDialog;
    private TextView mTv_loadingMsg;

    protected FragmentManager mFragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = this;
        mFragmentManager = getSupportFragmentManager();

    }

    protected void initView() {

    }

    protected void initData() {
    }

    /**
     * 刷卡获取卡信息
     */
    public void getCardInfo(String orderNum) {
        showLoading();
        HttpHelper.getCardInfo(orderNum, this);
    }

    /**
     * 刷卡上岗
     */
    public void clockIn(String cardNum) {
        HttpHelper.positionLogin(cardNum, this);
    }

    /**
     * 刷卡上岗
     */
    public void clockOut(String cardNum) {
        HttpHelper.positionLogout(cardNum, this);
    }

    protected boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    protected void showLoading() {
        showLoading(getString(R.string.loading), true);
    }

    protected void showLoading(String msg, boolean cancelAble) {
        if (mProDialog == null) {
            initProgressDialog();
        }
        mTv_loadingMsg.setText(msg);
        mProDialog.setCancelable(cancelAble);
        if (!isFinishing()) {
            mProDialog.show();
        }
    }

    protected void dismissLoading() {
        if (mProDialog != null) {
            mProDialog.dismiss();
        }
    }

    private void initProgressDialog() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dlg_loading, null);
        mTv_loadingMsg = view.findViewById(R.id.tv_loading_msg);

        mProDialog = new Dialog(mContext);
        mProDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mProDialog.setContentView(view);
    }

    protected void showErrorDialog(String msg) {
        MyAlertDialog.showAlert(mContext, msg);
    }

    protected void showAlert(String msg) {
        MyAlertDialog.showAlert(mContext, msg, MyAlertDialog.TYPE.ALERT, null, false);
    }

    protected void toast(String msg) {
        toast(msg, Toast.LENGTH_LONG);
    }

    protected void toast(String msg, int duration) {
        ToastUtil.showToast(this, msg, duration);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onSuccess(String url, JSONObject resultJSON) {
        dismissLoading();
        if (!HttpHelper.isSuccess(resultJSON)) {
            showErrorDialog(resultJSON.getString("message"));
        }
    }

    @Override
    public void onFailure(String url, int code, String message) {
        dismissLoading();
        showErrorDialog(message);
    }

}
