package com.eeka.matstoremanager.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eeka.matstoremanager.R;
import com.eeka.matstoremanager.adapter.CommonAdapter;
import com.eeka.matstoremanager.adapter.ViewHolder;
import com.eeka.matstoremanager.bo.StorageWaitListItemBo;
import com.eeka.matstoremanager.http.HttpHelper;

import java.util.List;

public class WaitList extends BaseActivity {

    private List<StorageWaitListItemBo> mList_data;
    private ItemAdapter mItemAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_waitlist);

        initView();
        initData();
    }

    @Override
    protected void initView() {
        super.initView();
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("待入库位清单");

        ListView listView = findViewById(R.id.lv_waitList);
        mItemAdapter = new ItemAdapter(mContext, mList_data, R.layout.item_waitlist);
        listView.setAdapter(mItemAdapter);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();
        showLoading();
        HttpHelper.getStorageWaitList(this);
    }

    private class ItemAdapter extends CommonAdapter<StorageWaitListItemBo> {

        ItemAdapter(Context context, List<StorageWaitListItemBo> list, int layoutId) {
            super(context, list, layoutId);
        }

        @Override
        public void convert(ViewHolder holder, StorageWaitListItemBo item, int position) {
            holder.setText(R.id.tv_workCenter,item.getDESCRIPTION());
            holder.setText(R.id.tv_item,item.getITEM());
            holder.setText(R.id.tv_sizeCode,item.getSIZE_CODE());
            holder.setText(R.id.tv_packageNo,item.getSUB_SEQ());
            holder.setText(R.id.tv_packageQty,item.getSUB_QTY());
            holder.setText(R.id.tv_shopOrder,item.getSHOP_ORDER());
        }
    }

    @Override
    public void onSuccess(String url, JSONObject resultJSON) {
        super.onSuccess(url, resultJSON);
        if (HttpHelper.getStorageWaitList.equals(url)) {
            if (HttpHelper.isSuccess(resultJSON)) {
                mList_data = JSON.parseArray(resultJSON.getJSONArray("result").toString(), StorageWaitListItemBo.class);
                mItemAdapter.notifyDataSetChanged(mList_data);
            }
        }
    }
}
