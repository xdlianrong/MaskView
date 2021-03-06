package com.xdlr.maskview.mainView.purchaseView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.xdlr.maskview.R;
import com.xdlr.maskview.dao.GsdFastBlur;
import com.xdlr.maskview.dao.RenderScriptBlur;
import com.xdlr.maskview.dao.UrlTransBitmap;
import com.xdlr.maskview.dao.UserRequest;
import com.xdlr.maskview.mainView.confirmOrders.ConfirmOrders;
import com.xdlr.maskview.mainView.login.LoginByPhoneCode;
import com.xdlr.maskview.mainView.shoppingCart.entity.ShoppingCartData;
import com.xdlr.maskview.mainView.userInfoAndSellList.UserInfoAndSellList;
import com.xdlr.maskview.util.CircleImageView;
import com.xdlr.maskview.util.UtilParameter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PurchaseView extends AppCompatActivity implements View.OnClickListener {

    private String selectedImgUrl;
    private View allView, headerView, footerView;
    private PhotoView Iv_showPurchaseImg;
    private CircleImageView mIvHeadView;
    private TextView tv_imgPrice;
    private TextView tv_imgSellerName;
    private TextView tv_imgTopic;
    private TextView tv_sellDate;
    private boolean isHidden = false;
    private Bitmap bgBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private int screenWidth;
    private int screenHeight;
    private Context mContext;
    private UserRequest ur;
    private String imgOwner;   //照片持有者昵称
    private String sellDate;  //照片上架日期
    private String imgPrice;  //照片价格
    private String imgTopic;  //照片主题
    private String mGetHeadViewPath;
    private int mGetChildPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_view);
        mContext = this;
        // 先拿到选中的图片URL
        Iv_showPurchaseImg = findViewById(R.id.iv_showPurchaseImg);
        Bundle bundle = getIntent().getExtras();
        selectedImgUrl = bundle.getString("selectedImgUrl");
        mGetHeadViewPath = bundle.getString("selectedHeadViewPath");
        mGetChildPosition = bundle.getInt("childPositionDisplayHall");
        // 获取数据
        initData();
        // 点击图片显隐图片信息
        Iv_showPurchaseImg.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                updateHeaderAndFooter();
            }
        });
    }


    //请求服务器获取选中照片的信息
    private void initData() {
        //将图片地址截取为图片名称
        final String imgName = selectedImgUrl.substring(selectedImgUrl.indexOf("img"));
        ur = new UserRequest();
        ExecutorService es = Executors.newCachedThreadPool();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String result = ur.getOneSellImgInfo(imgName, null);
                if (result.contains("true")) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        imgOwner = jsonObject.get("userName") + "";
                        imgPrice = jsonObject.get("imgPrice") + "";
                        imgTopic = jsonObject.get("imgTopic") + "";
                        sellDate = jsonObject.get("sellDate") + "";
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        es.submit(task);
        es.shutdown();
        while (true) {
            if (es.isTerminated()) {
                initView();
                break;
            }
        }
    }

    private void initView() {
        // 获取屏幕宽高
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        screenWidth = point.x;
        screenHeight = point.y;
        mIvHeadView = findViewById(R.id.iv_purchaseView_headView);
        mIvHeadView.setOnClickListener(this);
        tv_imgPrice = findViewById(R.id.tv_purchase_imgPrice);
        tv_imgSellerName = findViewById(R.id.iv_purchase_sellerName);
        tv_imgTopic = findViewById(R.id.tv_purchase_imgTopic);
        tv_sellDate = findViewById(R.id.iv_purchase_sellDate);
        ImageView iv_back = findViewById(R.id.iv_purchaseBack);
        allView = findViewById(R.id.allView_purchase);
        headerView = findViewById(R.id.frameLayout_header_view);
        footerView = findViewById(R.id.frameLayout_footer_view);
        footerView.getBackground().setAlpha(10); //设置半透明
        headerView.getBackground().setAlpha(10); //设置半透明
        iv_back.setOnClickListener(this);
        Button bt_jumpOrders = findViewById(R.id.bt_purchase_now);
        bt_jumpOrders.setOnClickListener(this);
        Button bt_addOneToShoppingCart = findViewById(R.id.bt_purchase_addShoppingCart);
        bt_addOneToShoppingCart.setOnClickListener(this);
        //柔和的图片背景色
        //showSoftColor();
        text();
        tv_imgSellerName.setText(imgOwner);
        tv_imgPrice.setText(imgPrice);
        tv_imgTopic.setText(imgTopic);
        tv_sellDate.setText(sellDate);
        //网络加载图片
        Glide.with(this).load(selectedImgUrl).dontAnimate().into(Iv_showPurchaseImg);
        if (!mGetHeadViewPath.equals(UtilParameter.IMAGES_IP)) {
            Glide.with(this).load(mGetHeadViewPath).dontAnimate().into(mIvHeadView);
        }
    }

    private boolean checkLogin() {
        return UtilParameter.myPhoneNumber != null;
    }

    private void jumpLoginByCode() {
        Intent intent = new Intent(PurchaseView.this, LoginByPhoneCode.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_purchaseBack:
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_right);
                finish();
                break;
            case R.id.bt_purchase_now:
                if (!checkLogin()) {
                    jumpLoginByCode();
                } else {
                    jumpConfirmOrders();
                }
                break;
            case R.id.bt_purchase_addShoppingCart:
                if (!checkLogin()) {
                    jumpLoginByCode();
                } else {
                    addToMyShoppingCart();
                }
                break;
            case R.id.iv_purchaseView_headView:
                Intent intent = new Intent(mContext, UserInfoAndSellList.class);
                intent.putExtra("userNickName", imgOwner);
                intent.putExtra("childPositionDisplayHall", mGetChildPosition);
                startActivity(intent);
                break;
        }
    }

    // 添加到购物车
    private void addToMyShoppingCart() {
        ExecutorService es = Executors.newCachedThreadPool();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String imgPath = selectedImgUrl.substring(selectedImgUrl.indexOf("img"));
                final String result = ur.addOneToMyShoppingCart(imgOwner, imgPath, UtilParameter.myToken);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.equals("")) {
                            try {
                                JSONObject jsonObject = new JSONObject(result);
                                String tag = jsonObject.get("result") + "";
                                if (tag.equals("true")) {
                                    Toast.makeText(mContext, "加入成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    String note = jsonObject.get("data") + "";
                                    Toast.makeText(mContext, note, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(mContext, "服务器未响应!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        es.submit(task);
        es.shutdown();
    }

    // 点击立即购买,跳转确认订单界面
    private void jumpConfirmOrders() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = ur.isMyImg(imgOwner, UtilParameter.myToken);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("-----------", "run: " + result);
                        if (!result.equals("")) {
                            if (result.contains("true")) {
                                Intent intent = new Intent(PurchaseView.this, ConfirmOrders.class);
                                List<ShoppingCartData.DataBean> data = new ArrayList<>();
                                ShoppingCartData.DataBean dataBean = new ShoppingCartData.DataBean();
                                dataBean.setSellerName(tv_imgSellerName.getText() + "");
                                List<ShoppingCartData.DataBean.GoodsInfoBean> goodsData = new ArrayList<>();
                                ShoppingCartData.DataBean.GoodsInfoBean goodsInfoBean = new ShoppingCartData.DataBean.GoodsInfoBean();
                                goodsInfoBean.setImgPath(selectedImgUrl.substring(selectedImgUrl.indexOf("/img")));
                                goodsInfoBean.setImgPrice(tv_imgPrice.getText().toString());
                                goodsInfoBean.setImgTopic(imgTopic);
                                goodsData.add(goodsInfoBean);
                                dataBean.setGoodsInfo(goodsData);
                                data.add(dataBean);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("selected_shopping_goods", (Serializable) data);
                                bundle.putInt("mAllSelectedPrice", Integer.parseInt(imgPrice));
                                intent.putExtras(bundle);
                                startActivity(intent);
                            } else {
                                Toast.makeText(mContext, "不能购买自己的图片", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(mContext, "服务器未响应,请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void updateHeaderAndFooter() {
        if (isHidden) {
            headerView.animate().translationY(0);
            footerView.animate().translationY(0);
        } else {
            headerView.animate().translationY(-headerView.getMeasuredHeight());
            footerView.animate().translationY(footerView.getMeasuredHeight());
        }
        isHidden = !isHidden;
    }

    private void text() {
        long startTime = System.currentTimeMillis();
        UrlTransBitmap trans = new UrlTransBitmap();
        Bitmap bitmap = trans.returnBitMap(selectedImgUrl, mContext);
        long endTime1 = System.currentTimeMillis();
        Log.e("---", "转bitmap时间: " + (endTime1 - startTime));
        Bitmap mAll = RenderScriptBlur.blur(mContext, bitmap, 0.06f, 10);
        allView.setBackground(new BitmapDrawable(getResources(), mAll));
        long endTime2 = System.currentTimeMillis();
        Log.e("---", "高斯模糊时间: " + (endTime2 - endTime1));
    }

    // 让照片外的部分显示与照片结合的柔和色
    /*private void showSoftColor() {
        UrlTransBitmap trans = new UrlTransBitmap();
        Bitmap bitmap = trans.returnBitMap(selectedImgUrl, mContext);
        if (bitmap != null) {
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette == null) return;
                    // palette取色不一定取得到某些特定的颜色，这里通过取多种颜色来避免取不到颜色的情况
                    if (palette.getDarkVibrantColor(Color.TRANSPARENT) != Color.TRANSPARENT) {
                        createLinearGradientBitmap(palette.getDarkVibrantColor(Color.TRANSPARENT),
                                palette.getDarkVibrantColor(Color.TRANSPARENT));
                    } else if (palette.getDarkMutedColor(Color.TRANSPARENT) != Color.TRANSPARENT) {
                        createLinearGradientBitmap(palette.getDarkMutedColor(Color.TRANSPARENT),
                                palette.getDarkMutedColor(Color.TRANSPARENT));
                    } else {
                        createLinearGradientBitmap(palette.getDarkMutedColor(Color.TRANSPARENT),
                                palette.getDarkMutedColor(Color.TRANSPARENT));
                    }
                    *//* 获取有活力的颜色 : palette.getVibrantSwatch();
                    获取有活力的亮色 : palette.getLightVibrantSwatch();
                    获取柔和的颜色 : palette.getMutedSwatch();
                    获取柔和的亮色 : palette.getLightMutedSwatch();
                    获取柔和的暗色 : palette.getDarkMutedColor(Color.TRANSPARENT)*//*
                }
            });
        }
    }*/

    // 创建线性渐变背景色
    /*private void createLinearGradientBitmap(int darkColor, int color) {
        int bgColors[] = new int[2];
        bgColors[0] = darkColor;
        bgColors[1] = color;
        if (bgBitmap == null) {
            bgBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_4444);
        }
        if (mCanvas == null) {
            mCanvas = new Canvas();
        }
        if (mPaint == null) {
            mPaint = new Paint();
        }
        mCanvas.setBitmap(bgBitmap);
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        LinearGradient gradient = new LinearGradient(0, 0, 0, bgBitmap.getHeight(), bgColors[0], bgColors[1], Shader.TileMode.CLAMP);
        mPaint.setShader(gradient);
        RectF rectF = new RectF(0, 0, bgBitmap.getWidth(), bgBitmap.getHeight());
        mCanvas.drawRect(rectF, mPaint);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            allView.setBackground(new BitmapDrawable(getResources(), bgBitmap));
        }
    }*/

    // 设置本机返回键操作
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_right);
            finish();
        }
        return super.dispatchKeyEvent(event);
    }
}
