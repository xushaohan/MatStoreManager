package com.eeka.matstoremanager.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.eeka.matstoremanager.App;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.dialog.MyAlertDialog;
import com.eeka.matstoremanager.utils.SpUtil;
import com.eeka.matstoremanager.utils.SystemUtils;
import com.tencent.bugly.beta.Beta;

/**
 * 设置界面
 * Created by Lenovo on 2017/9/4.
 */

public class SettingActivity extends BaseActivity {

    private static final int REQUEST_LOGIN = 1;

    private RelativeLayout mLayout_setSystem;
    private TextView mTv_systemCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_setting);

        initView();
    }

    @Override
    protected void initView() {
        super.initView();
        findViewById(R.id.tv_setLoginUser).setOnClickListener(this);
        findViewById(R.id.tv_checkUpdate).setOnClickListener(this);

        mLayout_setSystem = findViewById(R.id.layout_setSystem);
        mLayout_setSystem.setOnClickListener(this);

        TextView tv_version = findViewById(R.id.tv_version);
        if (SystemUtils.isApkInDebug(mContext)) {
            tv_version.setOnClickListener(this);
        }
        StringBuilder sb = new StringBuilder("版本：");
        sb.append(SystemUtils.getAppVersionName(mContext));
        sb.append("(").append(SystemUtils.getAppVersionCode(mContext)).append(")");
        sb.append("_").append(SystemUtils.getChannelName(this));
        if (SystemUtils.isApkInDebug(mContext)) {
            sb.append("_debug");
        }
        tv_version.setText(sb.toString());

        mTv_systemCode = findViewById(R.id.tv_setting_system);
        String systemCode = SpUtil.get(SpUtil.KEY_SYSTEMCODE, null);
        if (!TextUtils.isEmpty(systemCode)) {
            switch (systemCode) {
                case "D":
                    mTv_systemCode.setText("D系统");
                    break;
                case "Q":
                    mTv_systemCode.setText("Q系统");
                    break;
                case "P":
                    mTv_systemCode.setText("P系统");
                    break;
                case "LH_P":
                    mTv_systemCode.setText("龙华P系统");
                    break;
            }
        }

        Switch mDebugSwitch = findViewById(R.id.switch_debug);
        mDebugSwitch.setChecked(SpUtil.isDebugLog());
        mDebugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpUtil.setDebugLog(isChecked);
            }
        });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.tv_setLoginUser:
                startActivity(new Intent(mContext, LoginActivity.class));
                break;
            case R.id.tv_checkUpdate:
//                showLoading();
//                HttpHelper.getAPKUrl(this);
                Beta.checkUpgrade();
                break;
            case R.id.tv_version:
                openSystemEnvironment();
                break;
            case R.id.layout_setSystem:
                setSystemCode();
                break;
        }
    }

    private int mClickCount;
    private long mLastMillis;

    /**
     * 显示系统环境设置的布局
     */
    private void openSystemEnvironment() {
        long curMillis = System.currentTimeMillis();
        if (mLastMillis == 0) {
            mLastMillis = curMillis;
            mClickCount++;
            return;
        }
        if (curMillis - mLastMillis < 1000) {
            mLastMillis = curMillis;
            mClickCount++;
        } else {
            mLastMillis = curMillis;
            mClickCount = 1;
        }
        if (mClickCount == 5) {
            mLayout_setSystem.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置系统环境
     */
    private void setSystemCode() {
        int checked = -1;
        String s = mTv_systemCode.getText().toString();
        if (!isEmpty(s)) {
            if (s.contains("D")) {
                checked = 0;
            } else if (s.contains("Q")) {
                checked = 1;
            } else if (s.contains("龙华P")) {
                checked = 3;
            } else if (s.contains("P")) {
                checked = 2;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("请选择系统环境");
        final int finalChecked = checked;
        builder.setSingleChoiceItems(R.array.systemCode, checked, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != finalChecked) {
                    dialog.dismiss();
                    if (which == 0) {
                        SpUtil.save(SpUtil.KEY_SYSTEMCODE, "D");
                        SpUtil.saveSite("8081");
                        App.BASE_URL = App.BASE_URL_D;
                    } else if (which == 1) {
                        SpUtil.save(SpUtil.KEY_SYSTEMCODE, "Q");
                        SpUtil.saveSite("8082");
                        App.BASE_URL = App.BASE_URL_Q;
                    } else if (which == 2) {
                        SpUtil.save(SpUtil.KEY_SYSTEMCODE, "P");
                        SpUtil.saveSite("8081");
                        App.BASE_URL = App.BASE_URL_P;
                    } else if (which == 3) {
                        SpUtil.save(SpUtil.KEY_SYSTEMCODE, "LH_P");
                        SpUtil.saveSite("8082");
                        App.BASE_URL = App.BASE_URL_P_LH;
                    }
                    MyAlertDialog.showConfirmAlert(mContext, "系统切换成功，重启应用后生效。", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(mContext, MainActivity.class);
                            intent.putExtra(MainActivity.TAG_EXIT, true);
                            startActivity(intent);
                        }
                    });
                }
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_LOGIN) {
                toast("设置成功，正在刷新数据");
                startActivity(new Intent(mContext, MainActivity.class));
                finish();
            }
        }
    }
}
