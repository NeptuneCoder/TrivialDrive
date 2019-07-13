package com.example.android.trivialdrivesample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.pay.GooglePay;
import com.google.pay.GooglePayStatusListener;
import com.google.pay.IQueryProductDetailListener;
import com.google.pay.OrderParam;
import com.google.pay.Purchase;
import com.google.pay.RandomString;
import com.google.pay.SkuDetails;

public class InappActivity extends Activity {
    GooglePay googlePay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inapp);
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlStkgsczW8lR/dyQJIHrYtqzwkuSmCelIbXJ1x9+0RYoLut53WJF3/nk1IQRZYZbrKYssnN6/e8X4aml1d8ijudoLRT7aJDEAmynvq/Q/sfIkQQXjWm2MAowYdU2FR3W/+XX/7igEHdolXlwYJrDUnOyc5Y7DyKWZBKsZ4AfiU+9f3xaN4xC1rVeHDnOjHErmINHlfv2omP0pb1bPKXMM01pHliqo3fH5OXmcZ5qZNIgSj3NX/lYoFxlP4PGoXtcYvH1e1PrEU1Ho6apNkIEnDx4G1qeewDXsw7A0EvV/Sb+knwxGY2yre5o4NoLDcjY4+nrITKqHBliaJGf2/EqJQIDAQAB";

        googlePay = new GooglePay.Builder()
                .setContext(this)
                .setGoogleKey(base64EncodedPublicKey)
                .setListener(googlePayStatus)
                .build();

        findViewById(R.id.btn_inapp).setOnClickListener(v -> {
            RandomString randomString = new RandomString(36);
            googlePay.buyGoods(InappActivity.this, "foyo_yuanqi_001_item", randomString.nextString().toString());
        });
        findViewById(R.id.btn_query).setOnClickListener(v -> googlePay.queryProductDetails("foyo_yuanqi_001_item", new IQueryProductDetailListener() {

            @Override
            public void querySuccess(SkuDetails value) {
                Log.i("price", "price = " + value.getPrice() + "     currency = " + value.getPriceCurrencyCode());
            }

            @Override
            public void queryFailed(int status, String msg) {
                Log.i("price", "status = " + status + "     msg = " + msg);
            }


            @Override
            public void queryGoodsIdNoExist() {

            }
        }));

        findViewById(R.id.btn_handle_query).setOnClickListener(v -> googlePay.queryInventoryAsync());
    }


    private final GooglePayStatusListener googlePayStatus = new GooglePayStatusListener() {

        /**
         * 初始化失败是，调用该方法。在调用购买时，需要对该参数进行判断
         * https://www.jianshu.com/p/87ffdb7bc439
         * 该文章有提到解决方法
         *
         * @param boo
         */
        public void initStatus(boolean boo) {

        }

        public void onErrorCode(int code) {
            Log.i("code", "code=" + code);
        }

        public void onBuySuccess(OrderParam data) {
            Log.i("data", data.toString());
            //说明用户购买成功
            //TODO 如果设置setIsAutoConsume未false，不自动消耗。将购买订单上传到服务器，服务器到Google 服务器校验订单成功时，手动调用消耗方法
            if (googlePay != null) {
                googlePay.consumeAsync(data.currBuyType, data.purchaseData, data.dataSignature);
            }
        }

        public void onConsumeSuccess() {
            //说明用户消耗商品成功
        }

        @Override
        public void haveUnConsumeGoods(Purchase purchase) {
            purchase.getDeveloperPayload();
            purchase.getSku();


        }

        @Override
        public void unConsumeGoodsInfo(Purchase purchase) {
            //如果设置setIsAutoConsume未false，用户上次有未消耗的商品，会自动回调该方法，此时上传服务器校验，如果校验成功，手动调用消耗方法
            if (googlePay != null) {
                googlePay.consumeAsync(purchase.getItemType(), purchase.getOriginalJson(), purchase.getSignature());
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (googlePay == null) return;
        // Pass on the activity result to the helper for handling
        if (!googlePay.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googlePay != null) {
            googlePay.DestoryQuote(this);
        }
    }
}
