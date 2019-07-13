Trivial Drive
============

Sample for In-App Billing version 3


[Google pay 接入流程](https://juejin.im/post/5cf407cbf265da1b700491f5)

继上一篇[Google 内购及登录和Facebook登录的KEY 申请和可能遇到的问题](https://www.jianshu.com/p/87ffdb7bc439)，总结一篇客户端和服务器的整体流程。

##### 首先需要明白以下概念
1. Google inapp分为购买`mHelper.launchPurchaseFlow(context, productId, RC_REQUEST,
                    mPurchaseFinishedListener, payload);`和消耗`mHelper.consumeAsync(purchase, mConsumeFinishedListener);`两个步骤。如果购买成功，再调用消耗方法。如果购买成功，不调用消耗的方法，那么下次将不能再次购买，返回的错误码为7。
2. 调用查询方法` mHelper.queryInventoryAsync(mGotInventoryListener);`，可以查询到当前用户未消耗的商品。

##### 代码实现
我对[官方提供的demo](https://github.com/googlesamples/android-play-billing.git)进行了修改,将其支付相关代码抽取成了一个独立的moudle，打包成jar在项目中使用，因为该模块不涉及到任何的资源文件所以没用使用aar。

###### 初始化
```
 String base64EncodedPublicKey = "xxxxxxxx";
              googlePay = new GooglePay(this, base64EncodedPublicKey, new OnGooglePayStatusListener() {

            /**
             * 初始化失败是，调用该方法。在调用购买时，需要对该参数进行判断
             * https://www.jianshu.com/p/87ffdb7bc439
             * 该文章有提到解决方法
             *
             * @param boo
             */
            @Override
            public void initStatus(boolean boo) {

            }

            @Override
            public void onErrorCode(int code) {

            }

            @Override
            public void onBuySuccess(OrderParam data) {
                //说明用户购买成功
                //TODO 如果设置setIsAutoConsume未false，不自动消耗。将购买订单上传到服务器，服务器到Google 服务器校验订单成功时，手动调用消耗方法
                if (googlePay != null) {
                    googlePay.consumeAsync(data.currBuyType, data.purchaseData, data.dataSignature);
                }
            }

            @Override
            public void onConsumeSuccess() {
                //说明用户消耗商品成功
            }

            @Override
            public void unConsumeAsync(Purchase purchase) {
                //如果设置setIsAutoConsume未false，用户上次有未消耗的商品，会自动回调该方法，此时上传服务器校验，如果校验成功，手动调用消耗方法
                if (googlePay != null) {
                    googlePay.consumeAsync(purchase.getItemType(), purchase.getOriginalJson(), purchase.getSignature());
                }
            }
        });
```

###### 注册回调
如果不调用该方法`googlePay.handleActivityResult(requestCode, resultCode, data)`那么支付成功后的参数将不能获取到。
```
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
```
###### 释放对象
```
 @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googlePay != null) {
            googlePay.DestoryQuote();
        }
    }
```

###### 购买商品方法
```
/**
     * 用于购买商品
     *
     * @param context
     * @param productId  Google play store 后台定义的商品的id。如果错误的话，将提示检索失败
* @param payload   该字段随机生成一定长度的字符串，用于生成的订单支付成功后根据google返回的订单信息关联。
     */
    public void buyGoods(Activity context, String productId, String payload) {
        Log.i("payload", "GpGoods:" + payload);
        try {
            mHelper.launchPurchaseFlow(context, productId, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
//            complain("Error launching purchase flow. Another async operation in progress.");
//            setWaitScreen(false);
            if (listener != null) {
                listener.onErrorCode(OnGooglePayStatusListener.INAPP_FAILED);
            }
        }
    }
```
###### 订阅商品方法
```
 /**
     * 用于订阅商品
     *
     * @param context
     * @param productId  Google play store 后台定义的商品的id。如果错误的话，将提示检索失败
* @param payload   该字段随机生成一定长度的字符串，用于生成的订单支付成功后根据google返回的订单信息关联。
     */
    public void subsGoods(Activity context, String productId, String payload) {
        Log.i("payload", "subsGoods:" + payload);
        try {
            mHelper.launchSubscriptionPurchaseFlow(context, productId, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (Exception e) {
//            complain("Error launching purchase flow. Another async operation in progress." + e);
//            setWaitScreen(false);
            if (listener != null) {
                listener.onErrorCode(OnGooglePayStatusListener.SUBS_FAILED);
            }
        }
    }

```

###### 常用方法和参数说明
1. `googlePay.setIsAutoConsume(true);` 方法设置为true时，购买完成后会自动调用消耗方法。构造函数中的第三个参数的 `    void unConsumeAsync(Purchase purchase);`方法不会被回调。
2. 初始化第三个参数的方法说明
```
对上面的第三个参数的回调方法进行说明
```
  /**
     * 初始化失败是，调用该方法。在调用购买时，需要对
     * https://www.jianshu.com/p/87ffdb7bc439
     * 该文章有提到解决方法
     *
     * @param boo
     */
    void initStatus(boolean boo);

    /**
     * 各种的支付过程中的状态
     *
     * @param code
     */
    void onErrorCode(int code);

    /**
     * 调用购买或订阅成功时会回调该方法
     *
     * @param data
     */
    void onBuySuccess(OrderParam data);

    /**
     * 调用消耗商品成功
     */
    void onConsumeSuccess();

    /**
     * <p>
     * public void setIsAutoConsume(boolean isAutoConsume) {
     * this.isAutoConsume = isAutoConsume;
     * }
     * <p>
     * setIsAutoConsume 设置为true时，将会自动消耗，不会回调该方法。设置为false，需要手动调用消耗方法
     *
     * @param purchase
     */
    void unConsumeAsync(Purchase purchase);
```
```

##### 代码混淆
配置aidl文件不被混淆
```
-keep class com.android.vending.billing.**{*;}
```





