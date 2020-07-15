package com.xdlr.maskview.mainView.userInfoAndSellList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.xdlr.maskview.R;
import com.xdlr.maskview.dao.AvertTwoTouch;
import com.xdlr.maskview.dao.UserRequest;
import com.xdlr.maskview.mainView.herAllFansInfo.HerAllFansInfo;
import com.xdlr.maskview.mainView.herAllFocusInfo.HerAllFocusInfo;
import com.xdlr.maskview.mainView.login.LoginByPhoneCode;
import com.xdlr.maskview.mainView.userInfoAndSellList.adapter.UserSellListAdapter;
import com.xdlr.maskview.util.CircleImageView;
import com.xdlr.maskview.util.UtilParameter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserInfoAndSellList extends AppCompatActivity implements View.OnClickListener {

    private CircleImageView mIvHeadView;
    private TextView mTvNickName;
    private TextView mTvFansCount;
    private TextView mTvFocusCount;
    private Button mBtFocus;
    private RecyclerView mRecyclerView;
    private Context mContext;
    private UserRequest mRequest;
    private String mHeadViewPath;
    private String mNickName;
    private String mFansCount;
    private String mFocusCount;
    private List<String> mSellImgList;
    private int mRelation;
    private static final int MY_INFO_SUCCESS = 0;
    private static final int FOCUS_SUCCESS = 1;
    private static final int FOCUS_FAIL = 2;
    private static final int NO_RESPONSE = 3;
    private String mGetNickName;
    private int mGetChildPosition;
    private int mGetSelectedItemPosition;
    private UserSellListAdapter mAdapter;
    private AlertDialog mNoResponseAlert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info_and_sell_list);

        initView();
    }

    private void initView() {
        mContext = this;
        mIvHeadView = findViewById(R.id.iv_userSellList_HeadView);
        mTvNickName = findViewById(R.id.tv_userSellList_nickName);
        mTvFansCount = findViewById(R.id.tv_userSellList_fansCount);
        mTvFocusCount = findViewById(R.id.tv_userSellList_focusCount);
        mBtFocus = findViewById(R.id.bt_userSellList_focus);
        mBtFocus.setOnClickListener(this);
        mBtFocus.setVisibility(View.INVISIBLE);
        mRecyclerView = findViewById(R.id.rv_userSellList_recycler);
        ImageView finishActivity = findViewById(R.id.iv_userSellList_finish);
        finishActivity.setOnClickListener(this);
        LinearLayout mLvFansInfo = findViewById(R.id.lv_userSellList_fansInfo);
        LinearLayout mLvFocusInfo = findViewById(R.id.lv_userSellList_focusInfo);
        mLvFansInfo.setOnClickListener(this);
        mLvFocusInfo.setOnClickListener(this);
        mRequest = new UserRequest();
        mSellImgList = new ArrayList<>();
        initData();
    }

    private void initData() {
        mGetNickName = getIntent().getStringExtra("userNickName");
        mGetChildPosition = getIntent().getIntExtra("childPositionDisplayHall", -9);
        mGetSelectedItemPosition = getIntent().getIntExtra("selectedItemPositionHerWindow", -9);
        ExecutorService es = Executors.newCachedThreadPool();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String result;
                if (UtilParameter.myToken == null) {
                    // 未登录
                    result = mRequest.getUserInfoAndSellList(mGetNickName, null);
                } else {
                    // 登录后
                    result = mRequest.getUserInfoAndSellList(mGetNickName, UtilParameter.myToken);
                }
                if (!result.equals("")) {
                    if (result.contains("true")) {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            mHeadViewPath = jsonObject.get("headViewPath") + "";
                            mNickName = jsonObject.get("userName") + "";
                            mFansCount = jsonObject.get("fansCount") + "";
                            mFocusCount = jsonObject.get("focusCount") + "";
                            JSONArray array = (JSONArray) jsonObject.get("sellImgPathList");
                            for (int i = 0; i < array.length(); i++) {
                                mSellImgList.add(array.get(i) + "");
                            }
                            mRelation = (int) jsonObject.get("relation");
                            mHandler.sendEmptyMessage(MY_INFO_SUCCESS);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // 未响应
                    mHandler.sendEmptyMessage(NO_RESPONSE);
                }
            }
        };
        es.submit(task);
        es.shutdown();

        while (true) {
            if (es.isTerminated()) {
                break;
            }
        }
    }

    private void showSellImgList() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);

        //垂直方向的2列
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        //防止Item切换
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new UserSellListAdapter(mContext, mSellImgList);
        mRecyclerView.setAdapter(mAdapter);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MY_INFO_SUCCESS) {
                mTvNickName.setText(mNickName);
                mTvFansCount.setText(mFansCount);
                mTvFocusCount.setText(mFocusCount);
                if (!mHeadViewPath.equals("")) {
                    String url = UtilParameter.IMAGES_IP + mHeadViewPath;
                    Glide.with(mContext).load(url).dontAnimate().placeholder(R.mipmap.head).into(mIvHeadView);
                }
                if (mSellImgList.size() > 0) {
                    showSellImgList();
                }
                if (mRelation == 0) {
                    mBtFocus.setVisibility(View.VISIBLE);
                }
                if (mRelation == 1) {
                    mBtFocus.setVisibility(View.INVISIBLE);
                }
            } else if (msg.what == FOCUS_SUCCESS) {
                mBtFocus.setVisibility(View.INVISIBLE);
                int fansCount = Integer.parseInt(mTvFansCount.getText() + "");
                int nowFans = fansCount + 1;
                mTvFansCount.setText(nowFans + "");
                // 发送广播
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setAction("action.refreshDisplayHallFansCount");
                        intent.setAction("action.refreshHerWindowUI");
                        Bundle bundle = new Bundle();
                        bundle.putInt("recyclerChildPosition", mGetChildPosition);
                        bundle.putInt("windowSelectedItemPosition", mGetSelectedItemPosition);
                        intent.putExtra("msg", bundle);
                        sendBroadcast(intent);
                    }
                }).start();
            } else if (msg.what == FOCUS_FAIL) {
                Toast toast = Toast.makeText(mContext, "关注失败,请稍后重试!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (msg.what == NO_RESPONSE) {
                mTvFansCount.setText("null");
                mTvFocusCount.setText("null");
                mTvNickName.setText("null");
                mNoResponseAlert = new AlertDialog.Builder(mContext).setMessage("网络不佳,请稍后再试!")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();
                mNoResponseAlert.show();
                mNoResponseAlert.setCanceledOnTouchOutside(false);
                mNoResponseAlert.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                            mNoResponseAlert.cancel();
                            finish();
                            return true;
                        }
                        return false;
                    }
                });
            }
            return false;
        }
    });

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_userSellList_finish:
                finish();
                break;
            case R.id.bt_userSellList_focus:
                if (AvertTwoTouch.isFastClick()) {
                    if (!checkLogin()) {
                        jumpLoginByCode();
                    } else {
                        focusHer();
                    }
                }
                break;
            case R.id.lv_userSellList_fansInfo:
                Intent intent_fansInfo = new Intent(mContext, HerAllFansInfo.class);
                intent_fansInfo.putExtra("userName", mGetNickName);
                mContext.startActivity(intent_fansInfo);
                break;
            case R.id.lv_userSellList_focusInfo:
                Intent intent_focusInfo = new Intent(mContext, HerAllFocusInfo.class);
                intent_focusInfo.putExtra("userName", mGetNickName);
                mContext.startActivity(intent_focusInfo);
                break;
        }
    }

    /**
     * 关注某人
     */
    private void focusHer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = mRequest.focusUser(mGetNickName, UtilParameter.myToken);
                if (!result.equals("")) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        String tag = jsonObject.get("result") + "";
                        if (tag.equals("true")) {
                            // 关注成功
                            mHandler.sendEmptyMessage(FOCUS_SUCCESS);
                        } else {
                            // 关注失败
                            mHandler.sendEmptyMessage(FOCUS_FAIL);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 服务器未响应
                    mHandler.sendEmptyMessage(NO_RESPONSE);
                }
            }
        }).start();
    }

    /**
     * 检查是否登录
     */
    private boolean checkLogin() {
        return UtilParameter.myPhoneNumber != null;
    }

    private void jumpLoginByCode() {
        Intent intent = new Intent(mContext, LoginByPhoneCode.class);
        intent.putExtra("from", "finish");
        mContext.startActivity(intent);
    }

    // 设置本机返回键操作
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            UserInfoAndSellList.this.finish();
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

}
