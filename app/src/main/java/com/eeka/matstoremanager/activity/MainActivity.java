package com.eeka.matstoremanager.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.eeka.matstoremanager.utils.SystemUtils;
import com.tencent.bugly.beta.Beta;

import java.util.ArrayList;
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

    private int mInputIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initView();
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
    public void sendNFCData(String nfc) {
        for (int i = 0; i < mLayout_storeInfo.getChildCount(); i++) {
            View itemView = mLayout_storeInfo.getChildAt(i);
            EditText et_rfid = itemView.findViewById(R.id.et_rfidNo);
            if (et_rfid.hasFocus()) {
                mInputIndex = i;
                et_rfid.setText(nfc);
                getMessage(et_rfid);
                break;
            }
        }
    }

    @Override
    protected void initView() {
        super.initView();
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mLayout_users = findViewById(R.id.layout_loginUsers);
        mEt_storageNo = findViewById(R.id.et_storageNo);
        mLayout_storeInfo = findViewById(R.id.layout_storeInfo);
        mLayout_storeInfo.addView(getItemView(null, true));

        mEt_storageNo.setOnEditorActionListener(new EditOnKeyListener());

        findViewById(R.id.btn_setting).setOnClickListener(this);
        findViewById(R.id.btn_storage).setOnClickListener(this);
        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SystemUtils.hideKeyboard(mContext, mEt_storageNo);
            }
        }, 500);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View getItemView(InStorageInfoBo item, boolean editable) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_storeinfo, null);
        TextView mTv_shopOrder = itemView.findViewById(R.id.tv_shopOrder);
        TextView mTv_size = itemView.findViewById(R.id.tv_size);
        EditText mEt_rfid = itemView.findViewById(R.id.et_rfidNo);
        EditText mEt_inQTY = itemView.findViewById(R.id.et_inQTY);

        mEt_rfid.setEnabled(editable);

        if (item != null) {
            mTv_shopOrder.setText(item.getSHOP_ORDER());
            mTv_size.setText(item.getSIZE());
            mEt_rfid.setText(item.getRFID());
            mEt_inQTY.setText(item.getSTOR_QUANTITY());
        } else {
            mEt_rfid.setOnEditorActionListener(new EditOnKeyListener());
        }

        if (editable) {
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mLayout_storeInfo.removeView(view);
                    return true;
                }
            });
        }
        return itemView;
    }

    private class EditOnKeyListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == KeyEvent.KEYCODE_ENDCALL || actionId == KeyEvent.ACTION_DOWN) {
                if (v.getId() == R.id.et_storageNo) {
                    getMessage(mEt_storageNo);
                } else {
                    for (int i = 0; i < mLayout_storeInfo.getChildCount(); i++) {
                        View itemView = mLayout_storeInfo.getChildAt(i);
                        EditText et_rfid = itemView.findViewById(R.id.et_rfidNo);
                        if (et_rfid.hasFocus()) {
                            mInputIndex = i;
                            getMessage(et_rfid);
                            break;
                        }
                    }
                }
            }
            return false;
        }
    }

    private String mLastNum;

    private void getMessage(View v) {
        if (v.getId() == R.id.et_storageNo) {
            String storage = mEt_storageNo.getText().toString();
            if (!TextUtils.isEmpty(mLastNum) && !mLastNum.equals(storage)) {
                mLastNum = storage.replaceFirst(mLastNum, "");
            } else {
                mLastNum = storage;
            }
            mEt_storageNo.setText(mLastNum);
        }

        if (isEmpty(mLastNum)) {
            return;
        }

        String[] split = mLastNum.split("&");
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
        }

        if (v.getId() == R.id.et_storageNo) {
            showLoading();
            HttpHelper.getWareHouseMessage(location.substring(0, 1), location, mClothType, this);
        } else {
            View childAt = mLayout_storeInfo.getChildAt(mInputIndex);
            EditText et_rfid = childAt.findViewById(R.id.et_rfidNo);
            String rfid = et_rfid.getText().toString();
            if (!isEmpty(rfid)) {
                showLoading();
                HttpHelper.getWareHouseMessageByRFID(location.substring(0, 1), location, mClothType, rfid, this);
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
            case R.id.btn_add:
                if (mList_data == null || mLayout_storeInfo.getChildCount() > mList_data.size()) {
                    showErrorDialog("请勿重复添加空数据");
                    return;
                }
                mLayout_storeInfo.addView(getItemView(null, true));
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
        for (int i = 0; i < childCount; i++) {
            View childAt = mLayout_storeInfo.getChildAt(i);
            EditText et_rfid = childAt.findViewById(R.id.et_rfidNo);
            String rfid = et_rfid.getText().toString();
            if (isEmpty(rfid)) {
                //卡号都未获取的情况当做本条数据为空，直接跳过
                break;
            }
            TextView tv_shopOrder = childAt.findViewById(R.id.tv_shopOrder);
            if (isEmpty(tv_shopOrder.getText().toString())) {
                showErrorDialog("卡号 " + rfid + "未获取数据，请刷卡获取。");
                return;
            }
            EditText et_inQTY = childAt.findViewById(R.id.et_inQTY);
            String s = et_inQTY.getText().toString();
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
                mLayout_storeInfo.removeAllViews();
                mList_data = JSON.parseArray(resultJSON.getJSONArray("result").toString(), InStorageInfoBo.class);
                if (mList_data != null && mList_data.size() != 0) {
                    for (InStorageInfoBo item : mList_data) {
                        mLayout_storeInfo.addView(getItemView(item, false));
                    }
                } else {
                    mLayout_storeInfo.addView(getItemView(null, true));
                }
            } else if (HttpHelper.getWareHouseMessageByRFID.equals(url)) {
                InStorageInfoBo item = JSON.parseObject(resultJSON.getJSONObject("result").toString(), InStorageInfoBo.class);
                if (isEmpty(item.getME_QUANTITY())) {
                    item.setME_QUANTITY(item.getSTOR_QUANTITY());
                }
                mList_data.add(item);
                mLayout_storeInfo.removeViewAt(mInputIndex);
                mLayout_storeInfo.addView(getItemView(item, true), mInputIndex);
            } else if (HttpHelper.inStorage.equals(url)) {
                toast("入库成功");
            } else if (HttpHelper.positionLogout_url.equals(url)) {
                toast("用户离岗成功");
                logoutSuccess();
            }
        } else {
            if (HttpHelper.getWareHouseMessage.equals(url)) {
                locationReqFocus();
            } else if (HttpHelper.getWareHouseMessageByRFID.equals(url)) {
                //通过 RFID 获取数据失败时清空本条数据，避免遗留上张卡的数据导致与卡号不对应
                View child = mLayout_storeInfo.getChildAt(mInputIndex);
                TextView tv_shopOrder = child.findViewById(R.id.tv_shopOrder);
                TextView tv_size = child.findViewById(R.id.tv_size);
                EditText et_inQTY = child.findViewById(R.id.et_inQTY);
                tv_shopOrder.setText(null);
                tv_size.setText(null);
                et_inQTY.setText(null);
            }
        }
    }
}
