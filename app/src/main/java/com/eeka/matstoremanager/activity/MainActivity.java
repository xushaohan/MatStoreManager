package com.eeka.matstoremanager.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
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
    public static final String TAG_INIT = "init";

    private PositionInfoBo mPositionInfo;

    private EditText mEt_storageNo;
    private List<InStorageInfoBo> mList_data;
    private String mClothType;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mLayout_users;

    private LinearLayout mLayout_storeInfo;

    private ScanReceiver mScanReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initView();

        checkResource();
    }

    @Override
    public void onStart() {
        super.onStart();
        mScanReceiver = new ScanReceiver();
        IntentFilter filter = new IntentFilter("android.intent.ACTION_DECODE_DATA");
        registerReceiver(mScanReceiver, filter);
    }

    @Override
    public void onStop() {
        unregisterReceiver(mScanReceiver);
        super.onStop();
    }

    private class ScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String scanResult = intent.getStringExtra("barcode_string");
                getMessage(scanResult);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isExit = intent.getBooleanExtra(TAG_EXIT, false);
            if (isExit) {
                finish();
                System.exit(0);
            } else {
                boolean init = intent.getBooleanExtra(TAG_INIT, false);
                if (init) {
                    checkResource();
                }
            }
        }
    }

    @Override
    public void sendNFCData(String value) {
        for (int i = 0; i < mLayout_storeInfo.getChildCount(); i++) {
            View itemView = mLayout_storeInfo.getChildAt(i);
            TextView tv_rfid = itemView.findViewById(R.id.tv_rfidNo);
            if (tv_rfid.getText().toString().equals(value)) {
                showAlert("卡号：" + value + " 已在当前仓库内，请勿重复刷卡。");
                return;
            }
        }
        getMessage(value);
    }

    @Override
    protected void initView() {
        super.initView();
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mLayout_users = findViewById(R.id.layout_loginUsers);
        mEt_storageNo = findViewById(R.id.et_storageNo);
        mLayout_storeInfo = findViewById(R.id.layout_storeInfo);

        mEt_storageNo.setOnEditorActionListener(new EditOnKeyListener());

        findViewById(R.id.btn_setting).setOnClickListener(this);
        findViewById(R.id.btn_storage).setOnClickListener(this);
        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_clean).setOnClickListener(this);
        findViewById(R.id.tv_waitList).setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View getItemView(InStorageInfoBo item) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_storeinfo, null);
        TextView tv_shopOrder = itemView.findViewById(R.id.tv_shopOrder);
        TextView tv_size = itemView.findViewById(R.id.tv_size);
        TextView tv_rfid = itemView.findViewById(R.id.tv_rfidNo);
        TextView tv_inQTY = itemView.findViewById(R.id.tv_inQTY);

        tv_shopOrder.setText(item.getSHOP_ORDER());
        tv_size.setText(item.getSIZE());
        tv_rfid.setText(item.getRFID());
        tv_inQTY.setText(item.getSTOR_QUANTITY());

        return itemView;
    }

    private class EditOnKeyListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == KeyEvent.KEYCODE_ENDCALL || actionId == KeyEvent.ACTION_DOWN) {
                String value = v.getText().toString();
                getMessage(value);
            }
            return false;
        }
    }

    private void getMessage(String key) {
        if (isEmpty(key)) {
            showAlert("库位或 RFID 卡号不能为空");
            return;
        }

        String storageNo = mEt_storageNo.getText().toString();
        if (key.contains("&")) {
            storageNo = key;
            mEt_storageNo.setText(storageNo);
        } else {
            if (isEmpty(storageNo)) {
                showAlert("请先扫描库位获取数据再扫描 RFID 卡");
                return;
            }
        }

        String[] split = storageNo.split("&");
        if (split.length != 2) {
            showErrorDialog("仓位不合法");
            locationReqFocus();
            return;
        }
        mClothType = split[0];
        String location = split[1];
        String[] area = location.split("-");
        if (area.length != 2) {
            showErrorDialog("仓位不合法");
            locationReqFocus();
            return;
        }

        showLoading();
        if (key.contains("&")) {
            HttpHelper.getWareHouseMessage(location.substring(0, 1), location, mClothType, this);
        } else {
            HttpHelper.getWareHouseMessageByRFID(location.substring(0, 1), location, mClothType, key, this);
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
            case R.id.btn_clean:
                new AlertDialog.Builder(mContext)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEt_storageNo.setText(null);
                                mLayout_storeInfo.removeAllViews();
                            }
                        })
                        .setMessage("确定清空所有数据吗？")
                        .create().show();
                break;
            case R.id.tv_waitList:
                startActivity(new Intent(mContext, WaitList.class));
                break;
        }
    }

    private void storage() {
        List<UserInfoBo> positionUsers = SpUtil.getPositionUsers();
        if (positionUsers == null || positionUsers.size() == 0) {
            showErrorDialog("请员工先上岗后操作");
            return;
        }
        int childCount = mLayout_storeInfo.getChildCount();
        if (childCount == 0) {
            showAlert("入库信息不能为空");
            return;
        }

        for (int i = 0; i < childCount; i++) {
            View childAt = mLayout_storeInfo.getChildAt(i);
            TextView tv_rfid = childAt.findViewById(R.id.tv_rfidNo);
            String rfid = tv_rfid.getText().toString();
            if (isEmpty(rfid)) {
                //卡号都未获取的情况当做本条数据为空，直接跳过
                break;
            }
            TextView tv_shopOrder = childAt.findViewById(R.id.tv_shopOrder);
            if (isEmpty(tv_shopOrder.getText().toString())) {
                showErrorDialog("卡号 " + rfid + "未获取数据，请刷卡获取。");
                return;
            }
            TextView tv_inQTY = childAt.findViewById(R.id.tv_inQTY);
            String s = tv_inQTY.getText().toString();
            if (isEmpty(s)) {
                MyAlertDialog.showAlert(mContext, "入库数不能为空");
                return;
            }

            if (i >= mList_data.size()) {
                showErrorDialog("有条目未获取数据，请核对");
                return;
            }
            InStorageInfoBo data = mList_data.get(i);
            int inQTY = Integer.valueOf(s);
            if (inQTY > Integer.valueOf(data.getME_QUANTITY())) {
                showErrorDialog("入库数量不能大于卡内数量");
                return;
            }
            data.setUSER_ID(positionUsers.get(0).getEMPLOYEE_NUMBER());
            data.setCLOTH_TYPE(mClothType);
            data.setSTOR_QUANTITY(s);
        }
        showLoading();
        HttpHelper.inStorage(mList_data, this);
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
                refreshLoginUsers();
//                HttpHelper.findProcessWithPadId(this);
            } else if (HttpHelper.findProcessWithPadId_url.equals(url)) {
                mPositionInfo = JSON.parseObject(HttpHelper.getResultStr(resultJSON), PositionInfoBo.class);
                assert mPositionInfo != null;
                SpUtil.save(SpUtil.KEY_RESOURCE, JSON.toJSONString(mPositionInfo.getRESR_INFOR()));
            } else if (HttpHelper.getWareHouseMessage.equals(url)) {
                toast("库位数据获取成功");
                mLayout_storeInfo.removeAllViews();
                mList_data = JSON.parseArray(resultJSON.getJSONArray("result").toString(), InStorageInfoBo.class);
                if (mList_data != null && mList_data.size() != 0) {
                    for (InStorageInfoBo item : mList_data) {
                        mLayout_storeInfo.addView(getItemView(item));
                    }
                }
            } else if (HttpHelper.getWareHouseMessageByRFID.equals(url)) {
                InStorageInfoBo item = JSON.parseObject(resultJSON.getJSONObject("result").toString(), InStorageInfoBo.class);
                if (isEmpty(item.getME_QUANTITY())) {
                    item.setME_QUANTITY(item.getSTOR_QUANTITY());
                }
                mList_data.add(item);
                mLayout_storeInfo.addView(getItemView(item));
            } else if (HttpHelper.inStorage.equals(url)) {
                toast("入库成功");
            } else if (HttpHelper.positionLogout_url.equals(url)) {
                toast("用户离岗成功");
                logoutSuccess();
            }
        } else {
            if (HttpHelper.getWareHouseMessage.equals(url)) {
                locationReqFocus();
            }
        }
    }
}
