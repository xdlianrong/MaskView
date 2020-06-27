package com.xdlr.maskview.mainView.confirmOrders;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xdlr.maskview.R;
import com.xdlr.maskview.dao.UrlTransBitmap;
import com.xdlr.maskview.dao.UserRequest;
import com.xdlr.maskview.mainView.confirmOrders.adapter.SellerNameAdapter;
import com.xdlr.maskview.mainView.confirmOrders.bean.JsonData;
import com.xdlr.maskview.mainView.myPurchase.MyPurchase;
import com.xdlr.maskview.mainView.shoppingCart.entity.ShoppingCartData;
import com.xdlr.maskview.util.GetSDPath;
import com.xdlr.maskview.util.UtilParameter;
import com.xdlr.maskview.watermark.Robustwatermark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ConfirmOrders extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView myRecyclerView;
    private Context myContext;
    private SellerNameAdapter adapter;
    private List<ShoppingCartData.DataBean> datas;
    private int allSelectedPrice;
    private int myPoints;

    private List<JsonData> sendDatas; //上传服务器的字段集合, 其中图片名称需要拼接
    private UrlTransBitmap urlTransBitmap;
    private UserRequest ur;
    private Robustwatermark robust;
    private int purchaseCount;

    private AlertDialog alertDialog;
    private AlertDialog waterMarkWaitingDialog;
    private final static int PURCHASE_WAITING = 0;
    private final static int PURCHASE_FAIL = 1;

    private String SDCardPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confrim_orders);

        myContext = this;
        initData();
    }

    private void initData() {
        datas = (List<ShoppingCartData.DataBean>) getIntent().getExtras().getSerializable("selected_shopping_goods");
        allSelectedPrice = getIntent().getExtras().getInt("allSelectedPrice");
        SDCardPath = GetSDPath.getSDPath(myContext);
        if (datas != null && datas.size() > 0) {
            initView();
            Log.e("-----------", "initData: " + datas.get(0).getGoodsInfo().get(0).getImgPath());
        }
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        ImageView iv_finishThisActivity = findViewById(R.id.confirmOrders_finishThisActivity);
        iv_finishThisActivity.setOnClickListener(this);
        myRecyclerView = findViewById(R.id.recycler_confirm_orders);
        myRecyclerView.setHasFixedSize(true);
        myRecyclerView.setItemAnimator(null);

        urlTransBitmap = new UrlTransBitmap();
        ur = new UserRequest();
        robust = new Robustwatermark();

        myPoints = Integer.parseInt(UtilParameter.myPoints);
        LinearLayout linearLayout_enough = findViewById(R.id.layout_confirmOrders_moneyEnough);
        LinearLayout linearLayout_notEnough = findViewById(R.id.layout_confirmOrders_moneyNotEnough);
        if (allSelectedPrice <= myPoints) {
            //钱数充足
            linearLayout_enough.setVisibility(View.VISIBLE);
            linearLayout_notEnough.setVisibility(View.INVISIBLE);
            TextView tv_allSelectedMoney = findViewById(R.id.tv_enough_allOrdersPrice);
            tv_allSelectedMoney.setText(allSelectedPrice + "");
            TextView tv_myMoney = findViewById(R.id.tv_enough_myMoney);
            tv_myMoney.setText(myPoints + "");
            Button bt_buyNow = findViewById(R.id.bt_buyNow);
            bt_buyNow.setOnClickListener(this);
        } else {
            //钱数不足
            linearLayout_enough.setVisibility(View.INVISIBLE);
            linearLayout_notEnough.setVisibility(View.VISIBLE);
            TextView tv_allSelectedMoney = findViewById(R.id.tv_notEnough_allOrdersPrice);
            tv_allSelectedMoney.setText(allSelectedPrice + "");
            TextView tv_myMoney = findViewById(R.id.tv_notEnough_myMoney);
            tv_myMoney.setText(myPoints + "");
            Button bt_rechargeNow = findViewById(R.id.bt_recharge);
            bt_rechargeNow.setOnClickListener(this);
        }

        //布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(myContext);
        //垂直方向或水平
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        //是否反转
        linearLayoutManager.setReverseLayout(false);
        //布局管理器与RecyclerView绑定
        myRecyclerView.setLayoutManager(linearLayoutManager);
        //创建适配器
        adapter = new SellerNameAdapter(myContext, datas);
        //RecyclerView绑定适配器
        myRecyclerView.setAdapter(adapter);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirmOrders_finishThisActivity:
                ConfirmOrders.this.finish();
                break;
            case R.id.bt_buyNow:
                purchaseImgNow();
                break;
            case R.id.bt_recharge:
                //充值界面
                break;
        }
    }

    //购买图片
    private void purchaseImgNow() {
        purchaseCount = 1;

        final File purchaseImgFile;
        if (Build.VERSION.SDK_INT >= 29) {
            purchaseImgFile = new File(myContext.getExternalFilesDir(null).getAbsolutePath());
        } else {
            purchaseImgFile = new File(SDCardPath + "/MaskView购买");
        }
        if (!purchaseImgFile.exists()) {
            boolean make = purchaseImgFile.mkdirs();
            if (!make) {
                Toast.makeText(myContext, "文件夹创建失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // 转化成传递数据的集合,便于循环
        transJsonInfo();
        // 等待框
        loadingAlert();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 水印后的图片名称
                String afterMarkImgName;
                // 图片地址
                String imgHttpUrl;
                for (int i = 0; i < sendDatas.size(); i++) {
                    FileOutputStream fos;
                    imgHttpUrl = UtilParameter.IMAGES_IP + sendDatas.get(i).imgPath;
                    Bitmap bitmap = urlTransBitmap.returnBitMap(imgHttpUrl, myContext);
                    Bitmap dstbmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    try {
                        // 加水印, 1是确权,2是交易
                        Bitmap myBitmap = robust.robustWatermark(dstbmp, UtilParameter.myPhoneNumber, UtilParameter.DEAL_FLAG);
                        String key = robust.getKey();
                        afterMarkImgName = "tra-" + UtilParameter.myPhoneNumber + "-" + sendDatas.get(i).imgName;
                        File file = new File(purchaseImgFile.getPath() + "/" + afterMarkImgName);
                        String sellerName = sendDatas.get(i).imgOwnerName;
                        // 上架的图片名称
                        String sell_imgName = sendDatas.get(i).imgPath.substring(sendDatas.get(i).imgPath.lastIndexOf("/") + 1);
                        // 上传服务器
                        String result = ur.purchaseImg(sellerName, sell_imgName, sendDatas.get(i).imgPrice, key, UtilParameter.myToken);
                        Log.e("--------", result);
                        if (result.contains("true")) {
                            // 购买成功后再存入相册
                            fos = new FileOutputStream(file);
                            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                            purchaseCount++;
                            if (Build.VERSION.SDK_INT >= 29) {
                                GetSDPath.scanFile(file, myContext);
                            } else {
                                // 发送广播,刷新图库
                                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri uri = Uri.fromFile(file);
                                intent.setData(uri);
                                sendBroadcast(intent);
                            }
                            // 购买完成后,刷新我的积分
                            myPoints = (myPoints - Integer.parseInt(sendDatas.get(i).imgPrice));
                        } else {
                            // 购买失败
                            confirmWaitingHandler.sendEmptyMessage(PURCHASE_FAIL);
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //刷新积分
                UtilParameter.myPoints = myPoints + "";
            }
        }).start();

    }

    //将订单里的图片信息转成传递的字符串集合
    private void transJsonInfo() {
        sendDatas = new ArrayList<>();
        JsonData data;
        String imgUrl;
        for (int i = 0; i < datas.size(); i++) {
            for (int j = 0; j < datas.get(i).getGoodsInfo().size(); j++) {
                data = new JsonData();
                data.imgOwnerName = datas.get(i).getSellerName();
                imgUrl = datas.get(i).getGoodsInfo().get(j).getImgPath();
                data.imgPrice = datas.get(i).getGoodsInfo().get(j).getImgPrice();
                data.imgPath = imgUrl;
                data.imgName = imgUrl.substring(imgUrl.indexOf("con-") + 16);
                sendDatas.add(data);
            }
        }
    }

    //显示购买进度提示
    private void loadingAlert() {
        View view = View.inflate(myContext, R.layout.sell_waiting_window, null);
        waterMarkWaitingDialog = new AlertDialog.Builder(myContext).setView(view).create();
        waterMarkWaitingDialog.setTitle("正在进行购买水印,请耐心等待......");
        waterMarkWaitingDialog.show();
        waterMarkWaitingDialog.setCanceledOnTouchOutside(false);
        confirmWaitingHandler.sendEmptyMessage(PURCHASE_WAITING);
    }

    //添加购买水印等待提示框
    Handler confirmWaitingHandler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == PURCHASE_WAITING) {
                TextView tv_now = waterMarkWaitingDialog.findViewById(R.id.loading_nowCount);
                TextView tv_all = waterMarkWaitingDialog.findViewById(R.id.loading_allCount);
                if (tv_now != null && tv_all != null) {
                    tv_now.setText(purchaseCount + "");
                    tv_all.setText("/" + sendDatas.size());
                }
                if (waterMarkWaitingDialog.isShowing()) {
                    confirmWaitingHandler.sendEmptyMessageDelayed(PURCHASE_WAITING, 1000);
                    if (purchaseCount > sendDatas.size()) {
                        waterMarkWaitingDialog.cancel();
                        finishAlert();
                    }
                }
            } else if (msg.what == PURCHASE_FAIL) {
                waterMarkWaitingDialog.cancel();
                alertDialog = new AlertDialog.Builder(myContext).setMessage("服务器未响应,请稍后重试")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create();
                alertDialog.show();
            }
            return false;
        }
    });


    //购买完成后提示
    private void finishAlert() {
        alertDialog = new AlertDialog.Builder(myContext)
                .setMessage("已购买" + sendDatas.size() + "张图片")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //跳转我的购买界面
                        Intent intent = new Intent(myContext, MyPurchase.class);
                        startActivity(intent);
                        finish();
                    }
                }).create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);
    }
}
