Trivial Drive
============

Sample for In-App Billing version 3

Introduction
------------

This sample is provided to demonstrate Google Play In-app Billing. To read
more visit https://developer.android.com/google/play/billing/index.html

This game is a simple "driving" game where the player can buy gas
and drive. The car has a tank which stores gas. When the player purchases
gas, the tank fills up (1/4 tank at a time). When the player drives, the gas
in the tank diminishes (also 1/4 tank at a time).

The user can also purchase a "premium upgrade" that gives them a red car
instead of the standard blue one (exciting!).

The user can also purchase a subscription ("infinite gas") that allows them
to drive without using up any gas while that subscription is active. The
subscription can either be purchased monthly or yearly.

Pre-requisites
--------------

- [Documentation](https://developer.android.com/google/play/billing/billing_overview.html)

Screenshots
-----------
![Screenshot1](playstore/screenshot1.png)

Getting Started
---------------

This sample can't be run as-is. You have to create your own
application instance in the Developer Console and modify this
sample to point to it. Here is what you must do:

ON THE GOOGLE PLAY DEVELOPER CONSOLE

1. Create an application on the Developer Console, available at
   https://play.google.com/apps/publish/.

2. Copy the application's public key (a base-64 string). You can find this in
   the "Services & APIs" section under "Licensing & In-App Billing".

IN THE CODE

3. Open MainActivity.java, find the declaration of base64EncodedPublicKey and
   replace the placeholder value with the public key you retrieved in Step 2.

4. Change the sample's package name to your package name. To do that, update the
   package name in AndroidManifest.xml and correct the references (especially the
   references to the R object).

5. Export an APK, signing it with your PRODUCTION (not debug) developer certificate.

BACK TO THE GOOGLE PLAY DEVELOPER CONSOLE

6. Upload your APK to Google Play for Alpha Testing.

7. Make sure to add your test account (the one you will use to test purchases)
   to the "testers" section of your app. Your test account CANNOT BE THE SAME AS
   THE PUBLISHER ACCOUNT. If it is, your purchases won't go through.

8. Under In-app Products, create MANAGED in-app items with these IDs:
       premium, gas
   Set their prices to 1 dollar. You can choose a different price if you like.

9. Under In-app Products, create SUBSCRIPTION items with these IDs:
       infinite_gas_monthly, infinite_gas_yearly
   Set their prices to 1 dollar and the billing recurrence to monthly for
   infinite_gas_monthly and yearly for infinite_gas_yearly. To prevent being charged
   while testing, set the trial period to 7 days.

10. Publish your APK to the Alpha channel. Wait 2-3 hours for Google Play to process the APK
   If you don't wait for Google Play to process the APK, you might see errors where Google Play
   says that "this version of the application is not enabled for in-app billing" or something
   similar. Ensure that the In-App products move to the "Active" state within the console before
   testing.

TEST THE CODE

11. Install the APK signed with your PRODUCTION certificate, to a
test device [*].
12. Run the app.
13. Make purchases using the test account you added in Step 7.

Remember to refund any real purchases you make, if you don't want the
charges to actually to through. Remember, you can use the tester functionality within
the Google Play console to define test Google Accounts that won't be charged.
When using the tester functionality make sure to look for "Test" language appended
to each receipt. If you don't see "Test" then you will need to be sure to refund/cancel
the charge.

[*]: it will be easier to use a test device that doesn't have your
developer account logged in; this is because, if you attempt to purchase
an in-app item using the same account that you used to publish the app,
the purchase will not go through.

A NOTE ABOUT SECURITY
---------------------

This sample app implements signature verification but does not demonstrate
how to enforce a tight security model. When releasing a production application
to the general public, we highly recommend that you implement the security best
practices described in our documentation at:

http://developer.android.com/google/play/billing/billing_best_practices.html

In particular, you should set developer payload strings when making purchase
requests and you should verify them when reading back the results. This will make
it more difficult for a malicious party to perform a replay attack on your app.

Support
-------
If you've found an error in this sample, please file an issue:
https://github.com/googlesamples/android-play-billing/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

License
-------
Copyright 2012 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

CHANGELOG
---------

   2012-11-29: Initial release
   2013-01-08: Updated to include support for subscriptions
   2015-03-13: Updated to new dev console and added yearly subscriptions
   2015-08-27: Ported to gradle and prepped for transitioning to GitHub


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





