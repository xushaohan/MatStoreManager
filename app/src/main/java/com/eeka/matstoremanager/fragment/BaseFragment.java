package com.eeka.matstoremanager.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.dialog.MyAlertDialog;
import com.eeka.matstoremanager.http.HttpCallback;
import com.eeka.matstoremanager.http.HttpHelper;

/**
 * Created by Lenovo on 2017/6/12.
 */

public class BaseFragment extends Fragment implements View.OnClickListener, HttpCallback {

    protected Context mContext;
    protected View mView;

    private Dialog mProDialog;
    private TextView mTv_loadingMsg;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
    }

    protected void initView() {

    }

    protected void initData() {

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
        mProDialog.show();
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
        Toast.makeText(mContext, msg, duration).show();
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
