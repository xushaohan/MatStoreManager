package com.eeka.matstoremanager.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eeka.matstoremanager.utils.Logger;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class NFCActivity extends BaseActivity {

    private NfcAdapter mNfcAdapter;
    private PendingIntent pi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化NfcAdapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
//        //初始化PendingIntent
//        // 初始化PendingIntent，当有NFC设备连接上的时候，就交给当前Activity处理
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // 新建IntentFilter，使用的是第二种的过滤机制
//        tagDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
//        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当前app正在前端界面运行，这个时候有intent发送过来，那么系统就会调用onNewIntent回调方法，将intent传送过来
        // 我们只需要在这里检验这个intent是否是NFC相关的intent，如果是，就调用处理方法
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            processIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pi, null, null);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    private void processIntent(Intent intent) {
        //取出封装在intent中的TAG
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] id = tagFromIntent.getId();
        long cardId = getDec(id);
        Logger.d("NFC卡号：" + cardId);
        sendNFCData(String.valueOf(cardId));
    }

    public abstract void sendNFCData(String nfc);

    private long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (byte aByte : bytes) {
            long value = aByte & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private String getHex(byte[] bytes) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
                "B", "C", "D", "E", "F"};
        StringBuilder out = new StringBuilder();

        for (j = 0; j < bytes.length; ++j) {
            in = (int) bytes[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out.append(hex[i]);
            i = in & 0x0f;
            out.append(hex[i]);
        }
        return out.toString();
    }
}
