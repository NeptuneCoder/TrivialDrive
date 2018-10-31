package com.google.pay;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONException;

import java.util.Map;
import java.util.Set;

public class GooglePay implements IabBroadcastReceiver.IabBroadcastListener {
    private final Activity context;
    private final String base64EncodedPublicKey;
    private final OnGooglePayStatusListener listener;
    private String TAG = this.getClass().getSimpleName();
    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper mHelper;

    static final int RC_REQUEST = 10001;
    private boolean isAutoConsume = false;
    //通过参数传入
    //String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApjWy+r9s6ncuh2l8OK59KrvySuTUQi5Zc1Sel/y2nVXh+7rEAVNV+Ndz75eJeT+mA3Y3uzRAfCuRR6lziyhE+5Jj330JtoWvi4SNJghVMSTs/uxK1B/Jg1GVUsYzC93QciBIEch22hCZWI93Gjq5UJ3OC5uy45YwIS4bYnjv2n7H37QSlfE1pzlNq8HktULpfD1lA6Sdc8NNRDl3c5OfUzIwYh6d2ErDjEa0EnIEksGBHlo3/zsgTwuG4Fm1DugNA/uQbvaps3tFSzc55afFWPuTtzVEVYqAvP2hJglklmmz0oZNWK8GYPg4iEeXlFWGSuWRT04zYVgJFj0LbJkGWQIDAQAB";

    /**
     * @param context                上下文参数
     * @param base64EncodedPublicKey 购买需要的公钥
     * @param listener               初始化和购买的回调
     */
    public GooglePay(Activity context, String base64EncodedPublicKey, OnGooglePayStatusListener listener) {
        this.context = context;
        this.base64EncodedPublicKey = base64EncodedPublicKey;
        this.listener = listener;
        init();
    }

    public void setIsAutoConsume(boolean isAutoConsume) {
        this.isAutoConsume = isAutoConsume;
    }


    private void init() {
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        if (context.getPackageName().startsWith("com.example")) {
            throw new RuntimeException("Please change the sample's package name! See README.");
        }

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");

        mHelper = new IabHelper(context, base64EncodedPublicKey, new IabHelperCallbackListener() {
            @Override
            public void onGgSuccess(OrderParam data) {

                if (listener != null) {
                    listener.onBuySuccess(data);
                }
            }
        });

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(false);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
//                    complain("Problem setting up in-app billing: " + result);
                    if (listener != null) {
                        listener.initFailed(false);
                    }
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(GooglePay.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                context.registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
//                    complain("Error querying inventory. Another async operation in progress.");
                    if (listener != null) {
                        listener.onGgStatus(OnGooglePayStatusListener.QUERY_ERROR);
                    }
                }
            }
        });
    }

    //TODO  购买的时候传入
//    private String productId;
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                if (listener != null) {
                    listener.onGgStatus(result.mResponse);
                }
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            if (inventory != null) {
                Set<Map.Entry<String, Purchase>> entries = inventory.mPurchaseMap.entrySet();

                for (Map.Entry<String, Purchase> item : entries) {
                    if (isAutoConsume) {
                        try {
                            mHelper.consumeAsync(item.getValue(), mConsumeFinishedListener);
                            Log.d(TAG, "We have gas. Consuming it successful." + item.getKey());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            if (listener != null) {
                                listener.onGgStatus(OnGooglePayStatusListener.CONSUME_ERROR);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.unConsumeAsync(item.getValue());
                        }
                    }
                }
            }
//            Set<String> strings = inventory.mPurchaseMap.keySet();
//            for (String item : strings) {
////                Purchase premiumPurchase = inventory.getPurchase(item);
//                Log.d(TAG, "We have gas. Consuming it.  item:" + item);
//                Purchase gasPurchase = inventory.getPurchase(item);
//                if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
//                    Log.d(TAG, "We have gas. Consuming it.");
//                    try {
//                        mHelper.consumeAsync(gasPurchase, mConsumeFinishedListener);
//                        Log.d(TAG, "We have gas. Consuming it successful." + gasPurchase.getItemType());
//                    } catch (IabHelper.IabAsyncInProgressException e) {
//                        if (listener != null) {
//                            listener.onGgFailed(OnGooglePayStatusListener.CONSUME_ERROR);
//                        }
//                    }
//                }
//
//            }
            // Do we have the premium upgrade?

//            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
//            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
//
//            // First find out which subscription is auto renewing
////            Purchase gasMonthly = inventory.getPurchase(SKU_INFINITE_GAS_MONTHLY);
////            Purchase gasYearly = inventory.getPurchase(SKU_INFINITE_GAS_YEARLY);
////            if (gasMonthly != null && gasMonthly.isAutoRenewing()) {
////                mInfiniteGasSku = SKU_INFINITE_GAS_MONTHLY;
////                mAutoRenewEnabled = true;
////            } else if (gasYearly != null && gasYearly.isAutoRenewing()) {
////                mInfiniteGasSku = SKU_INFINITE_GAS_YEARLY;
////                mAutoRenewEnabled = true;
////            } else {
////                mInfiniteGasSku = "";
////                mAutoRenewEnabled = false;
////            }
//
//            // The user is subscribed if either subscription exists, even if neither is auto
//            // renewing
////            boolean mSubscribedToInfiniteGas = (gasMonthly != null && verifyDeveloperPayload(gasMonthly))
////                    || (gasYearly != null && verifyDeveloperPayload(gasYearly));
////            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
////                    + " infinite gas subscription.");

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately


        }
    };
    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                if (listener != null) {
                    listener.onConsumeSuccess();
                }
            } else {
                if (listener != null) {
                    listener.onGgStatus(OnGooglePayStatusListener.CONSUME_FAILED);
                }
            }
        }
    };
    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
//                complain("Error purchasing: " + result);
//                setWaitScreen(false);

                return;
            }
//            if (!verifyDeveloperPayload(purchase)) {
////                complain("Error purchasing. Authenticity verification failed.");
////                setWaitScreen(false);
//                return;
//            }

            Log.d(TAG, "Purchase successful.");
//            if (purchase.getSku().equals(productId)) {
            // bought 1/4 tank of gas. So consume it.
            Log.d(TAG, "Purchase is gas. Starting gas consumption.");
            if (isAutoConsume) {
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
//                    complain("Error consuming gas. Another async operation in progress.");
//                    setWaitScreen(false);
                    return;
                }
            }
// else if (purchase.getSku().equals(SKU_1_MOUTH)) {
//                // bought the premium upgrade!
//                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
//                alert("Thank you for upgrading to premium!");
//                mIsPremium = true;
//                updateUi();
//                setWaitScreen(false);
//            } else if (purchase.getSku().equals(SKU_INFINITE_GAS_MONTHLY)
//                    || purchase.getSku().equals(SKU_INFINITE_GAS_YEARLY)) {
//                // bought the infinite gas subscription
//                Log.d(TAG, "Infinite gas subscription purchased.");
//                alert("Thank you for subscribing to infinite gas!");
//                mSubscribedToInfiniteGas = true;
//                mAutoRenewEnabled = purchase.isAutoRenewing();
//                mInfiniteGasSku = purchase.getSku();
//                mTank = TANK_MAX;
//                updateUi();
//                setWaitScreen(false);
//            }
        }
    };

    /**
     * 下面三个参数都是Purchase获取到的。
     *
     * @param mPurchasingItemType
     * @param purchaseData
     * @param dataSignature
     */
    public void consumeAsync(String mPurchasingItemType, String purchaseData, String dataSignature) {
        Purchase purchase = null;
        try {
            purchase = new Purchase(mPurchasingItemType, purchaseData, dataSignature);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mPurchaseFinishedListener != null) {
            mPurchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success"), purchase);
        }
//        if (mHelper != null) {
//            try {
//                mHelper.consumeAsync(purchase, listener);
//            } catch (IabHelper.IabAsyncInProgressException e) {
//                e.printStackTrace();
//            }
//        }

    }

//    /**
//     * Verifies the developer payload of a purchase.
//     */
//    boolean verifyDeveloperPayload(Purchase p) {
//        if (p == null) {
//            return false;
//        }
//        String payload = p.getDeveloperPayload();
//        Log.i("payload", "verifyDeveloperPayload:" + payload);
//        return developerpayload.equals(payload);
//        /*
//         * TODO: verify that the developer payload of the purchase is correct. It will be
//         * the same one that you sent when initiating the purchase.
//         *
//         * WARNING: Locally generating a random string when starting a purchase and
//         * verifying it here might seem like a good approach, but this will fail in the
//         * case where the user purchases an item on one device and then uses your app on
//         * a different device, because on the other device you will not have access to the
//         * random string you originally generated.
//         *
//         * So a good developer payload has these characteristics:
//         *
//         * 1. If two different users purchase an item, the payload is different between them,
//         *    so that one user's purchase can't be replayed to another user.
//         *
//         * 2. The payload must be such that you can verify it even when the app wasn't the
//         *    one who initiated the purchase flow (so that items purchased by the user on
//         *    one device work on other devices owned by the user).
//         *
//         * Using your own server to store and verify developer payloads across app
//         * installations is recommended.
//         */
//
//    }

    @Override
    public void receivedBroadcast() {
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
//            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    /**
     * 用于订阅商品
     *
     * @param context
     * @param productId Google play store 后台定义的商品的id。如果错误的话，将提示检索失败
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
                listener.onGgStatus(OnGooglePayStatusListener.SUBS_FAILED);
            }
        }
    }

    /**
     * 用于购买商品
     *
     * @param context
     * @param productId Google play store 后台定义的商品的id。如果错误的话，将提示检索失败
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
                listener.onGgStatus(OnGooglePayStatusListener.INAPP_FAILED);
            }
        }
    }


    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    public void DestoryQuote() {
        // very important:
        if (mBroadcastReceiver != null) {
            context.unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

}
