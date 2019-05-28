package com.eeka.matstoremanager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.fragment.LoginFragment;

/**
 * 登录界面
 * Created by Lenovo on 2017/5/15.
 */

public class LoginActivity extends BaseActivity implements LoginFragment.OnLoginCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_login);

        initView();
    }

    @Override
    protected void initView() {
        super.initView();

        LoginFragment loginFragment = new LoginFragment();
        loginFragment.setType(LoginFragment.TYPE_LOGIN);
        loginFragment.setOnLoginCallback(this);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.loginFragment, loginFragment);
        transaction.commit();
    }

    @Override
    public void onLogin(boolean success) {
        if (success) {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.putExtra(MainActivity.TAG_INIT, true);
            startActivity(intent);
            finish();
        }
    }
}
