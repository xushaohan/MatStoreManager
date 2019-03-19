package com.eeka.matstoremanager.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.bo.ContextInfoBo;
import com.eeka.matstoremanager.bo.InStorageInfoBo;
import com.eeka.matstoremanager.bo.PositionInfoBo;
import com.eeka.matstoremanager.bo.UserInfoBo;
import com.eeka.matstoremanager.dialog.MyAlertDialog;
import com.eeka.matstoremanager.fragment.LoginFragment;
import com.eeka.matstoremanager.http.HttpHelper;
import com.eeka.matstoremanager.utils.NetUtil;
import com.eeka.matstoremanager.utils.SpUtil;
import com.tencent.bugly.beta.Beta;

import java.util.List;

public class MainActivity extends NFCActivity {

    public static final String TAG_EXIT = "isExit";

    private PositionInfoBo mPositionInfo;

    private TextView mTv_shopOrder;
    private TextView mTv_size;
    private EditText mEt_storageNo;
    private EditText mEt_rfid;
    private EditText mEt_inQTY;//入库数量
    private InStorageInfoBo mData;
    private String mClothType;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mLayout_users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initView();
        checkResource();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isExit = intent.getBooleanExtra(TAG_EXIT, false);
            if (isExit) {
                finish();
                System.exit(0);
            }
        }
    }

    @Override
    public void sendNFCData(String nfc) {
        mEt_rfid.setText(nfc);
        onKeyAction(mEt_rfid);
    }

    @Override
    protected void initView() {
        super.initView();
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mTv_shopOrder = findViewById(R.id.tv_shopOrder);
        mTv_size = findViewById(R.id.tv_size);
        mLayout_users = findViewById(R.id.layout_loginUsers);
        mEt_storageNo = findViewById(R.id.et_storageNo);
        mEt_rfid = findViewById(R.id.et_rfidNo);
        mEt_inQTY = findViewById(R.id.et_inQTY);

        mEt_rfid.setOnEditorActionListener(new EditOnKeyListener());
        mEt_storageNo.setOnEditorActionListener(new EditOnKeyListener());
        mEt_storageNo.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        onKeyAction(v);
                    }
                }
                return false;
            }
        });

        findViewById(R.id.btn_setting).setOnClickListener(this);
        findViewById(R.id.btn_storage).setOnClickListener(this);
        findViewById(R.id.btn_login).setOnClickListener(this);

        refreshLoginUsers();
    }

    private class EditOnKeyListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == KeyEvent.KEYCODE_ENDCALL) {
                onKeyAction(v);
            }
            return false;
        }
    }

    private String mLastNum;

    private void onKeyAction(View v) {
        if (v.getId() == R.id.et_storageNo) {
            //操作前先清理之前的数据
            mTv_shopOrder.setText(null);
            mTv_size.setText(null);
            mEt_inQTY.setText(null);
            mEt_rfid.setText(null);

            String storage = mEt_storageNo.getText().toString();
            if (!TextUtils.isEmpty(mLastNum)) {
                mLastNum = storage.replaceFirst(mLastNum, "");
            } else {
                mLastNum = storage;
            }
            mEt_storageNo.setText(mLastNum);
        }
        String rfid = mEt_rfid.getText().toString();
        if (!isEmpty(mLastNum)) {
            String[] split = mLastNum.split("&");
            if (split.length != 2) {
                showErrorDialog("仓位不合法");
                locationReqFocus();
            } else {
                mClothType = split[0];
                String location = split[1];
                String[] area = location.split("-");
                if (area.length != 2) {
                    showErrorDialog("仓位不合法");
                    locationReqFocus();
                } else {
                    if (isEmpty(rfid))
                        rfid = null;
                    showLoading();
                    HttpHelper.getWareHouseMessage(area[0], location, mClothType, rfid, MainActivity.this);
                }
            }
        }
    }

    private void locationReqFocus() {
        mEt_storageNo.postDelayed(new Runnable() {
            @Override
            public void run() {
                mEt_storageNo.requestFocus();
            }
        }, 50);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.btn_setting:
                startActivity(new Intent(mContext, SettingActivity.class));
                mDrawerLayout.closeDrawer(Gravity.START);
                break;
            case R.id.btn_login:
                showLoginDialog();
                break;
            case R.id.btn_storage:
                storage();
                break;
        }
    }

    private void storage() {
        List<UserInfoBo> positionUsers = SpUtil.getPositionUsers();
        if (positionUsers == null || positionUsers.size() == 0) {
            showErrorDialog("请员工先上岗后操作");
            return;
        }
        String s = mEt_inQTY.getText().toString();
        if (isEmpty(s)) {
            MyAlertDialog.showAlert(mContext, "入库数不能为空");
            return;
        }
        int inQTY = Integer.valueOf(s);
        if (inQTY > Integer.valueOf(mData.getSTOR_QUANTITY())) {
            showErrorDialog("入库数量不能大于卡内数量");
            return;
        }
        mData.setUSER_ID(positionUsers.get(0).getEMPLOYEE_NUMBER());
        mData.setCLOTH_TYPE(mClothType);
        mData.setQUANTITY(s);
        showLoading();
        HttpHelper.inStorage(mData, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectivityReceiver);
    }

    /**
     * 显示登录弹框
     */
    public void showLoginDialog() {
        final Dialog loginDialog = new Dialog(mContext);
        loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loginDialog.setContentView(R.layout.dlg_login);

        final LoginFragment loginFragment = (LoginFragment) mFragmentManager.findFragmentById(R.id.loginFragment);
        loginFragment.setOnClockCallback(new LoginFragment.OnClockCallback() {
            @Override
            public void onClockIn(boolean success) {
                if (success) {
                    loginDialog.dismiss();
                    refreshLoginUsers();
                }
            }
        });

        loginDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mFragmentManager.beginTransaction().remove(loginFragment).commit();
            }
        });
        loginDialog.show();
    }

    private String mLogoutUserId;//点击离岗时的员工ID

    private void refreshLoginUsers() {
        mLayout_users.removeAllViews();
        List<UserInfoBo> users = SpUtil.getPositionUsers();
        if (users != null && users.size() != 0) {
            for (final UserInfoBo user : users) {
                View view = LayoutInflater.from(mContext).inflate(R.layout.layout_loginuser, null);
                TextView tv_name = view.findViewById(R.id.tv_userName);
                TextView tv_userId = view.findViewById(R.id.tv_userId);
                tv_name.setText(user.getNAME());
                tv_userId.setText(user.getEMPLOYEE_NUMBER());

                view.findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLoading();
                        mLogoutUserId = user.getEMPLOYEE_NUMBER();
                        HttpHelper.positionLogout(mLogoutUserId, MainActivity.this);
                    }
                });

                mLayout_users.addView(view);
            }
        }
    }

    private void logoutSuccess() {
        List<UserInfoBo> loginUsers = SpUtil.getPositionUsers();
        if (loginUsers != null) {
            for (UserInfoBo user : loginUsers) {
                if (user.getEMPLOYEE_NUMBER().equals(mLogoutUserId)) {
                    loginUsers.remove(user);
                    break;
                }
            }
            SpUtil.savePositionUsers(loginUsers);
        }
        refreshLoginUsers();
    }

    /**
     * 网络状态发生变化接收器
     */
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetUtil.isNetworkAvalible(mContext)) {
                Beta.checkUpgrade();
                checkResource();
            }
        }
    };

    /**
     * 检查本地资源，如果已有资源，返回true，否则返回false,并从服务器获取资源
     */
    private boolean checkResource() {
        ContextInfoBo contextInfo = SpUtil.getContextInfo();
        if (contextInfo == null || mPositionInfo == null) {
            showLoading();
            HttpHelper.initData(this);
            return false;
        }
        return true;
    }

    @Override
    public void onSuccess(String url, JSONObject resultJSON) {
        super.onSuccess(url, resultJSON);
        if (HttpHelper.isSuccess(resultJSON)) {
            if (HttpHelper.queryPositionByPadIp_url.equals(url)) {
                ContextInfoBo contextInfoBo = JSON.parseObject(HttpHelper.getResultStr(resultJSON), ContextInfoBo.class);
                SpUtil.saveContextInfo(contextInfoBo);
                assert contextInfoBo != null;
                List<UserInfoBo> positionUsers = contextInfoBo.getLOGIN_USER_LIST();
                SpUtil.savePositionUsers(positionUsers);

//                HttpHelper.findProcessWithPadId(this);
            } else if (HttpHelper.findProcessWithPadId_url.equals(url)) {
                mPositionInfo = JSON.parseObject(HttpHelper.getResultStr(resultJSON), PositionInfoBo.class);
                assert mPositionInfo != null;
                SpUtil.save(SpUtil.KEY_RESOURCE, JSON.toJSONString(mPositionInfo.getRESR_INFOR()));
            } else if (HttpHelper.getWareHouseMessage.equals(url)) {
                mData = JSON.parseObject(resultJSON.getJSONObject("result").toString(), InStorageInfoBo.class);
                if (mData != null) {
                    String rfid = mData.getRFID();
                    mEt_rfid.setText(rfid);
                    if (isEmpty(rfid)) {
                        mEt_rfid.setEnabled(true);
                    } else {
                        mEt_rfid.setEnabled(false);
                    }
                    mTv_shopOrder.setText(mData.getSHOP_ORDER());
                    mTv_size.setText(mData.getSIZE());
                    mEt_inQTY.setText(mData.getSTOR_QUANTITY());
                }
            } else if (HttpHelper.inStorage.equals(url)) {
                toast("入库成功");
            } else if (HttpHelper.positionLogout_url.equals(url)) {
                toast("用户离岗成功");
                logoutSuccess();
            }
        } else if (HttpHelper.getWareHouseMessage.equals(url)) {
            locationReqFocus();
        }
    }
}
