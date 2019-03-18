package com.google.pay;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GooglePay implements IabBroadcastReceiver.IabBroadcastListener {
    private final Activity context;
    private final String base64EncodedPublicKey;
    private final GooglePayStatus listener;
    private String TAG = this.getClass().getSimpleName();
    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper mHelper;

    static final int RC_REQUEST = 10001;

    /**
     * 初始化Google pay
     *
     * @param context                上下文参数
     * @param base64EncodedPublicKey 购买需要的公钥
     * @param listener               初始化和购买的回调
     */
    public GooglePay(Activity context, String base64EncodedPublicKey, GooglePayStatus listener) {
        this.context = context;
        this.base64EncodedPublicKey = base64EncodedPublicKey;
        this.listener = listener;
        init();
    }

    /**
     * 判断是否自动消耗，如果不自动消耗的情况，需要用户手动调用消耗方法，否者下次购买不能成功
     */
    protected boolean isAutoConsume() {
        return false;

    }

    private IQueryProductDetailListener queryItemDetailListener;

    /**
     * 通过商品id，查询该商品是否存在，以及获取商品的价格和单位
     *
     * @param itemId                  商品id
     * @param queryItemDetailListener
     */
    public void queryProductDetails(String itemId, IQueryProductDetailListener queryItemDetailListener) {
        List<String> list = new ArrayList<>();
        list.add(itemId);
        this.queryItemDetailListener = queryItemDetailListener;
        try {
            mHelper.queryInventoryAsync(true, list, mGotProductDetailsListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            if (mGotProductDetailsListener != null) {
                mGotProductDetailsListener.onFailed(10, e.toString());
            }
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if (mGotProductDetailsListener != null) {
                mGotProductDetailsListener.onFailed(10, e.toString());
            }
        }
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
                        listener.initFailed(true);
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


            }
        });
    }

    /**
     * 判断是否有初始化
     *
     * @return
     */
    public boolean isSetupDone() {
        if (mHelper != null) {
            return mHelper.mSetupDone;
        } else {
            return false;
        }
    }

    /**
     * 暴露给用户，自己控制查询未消耗商品的时机
     */
    public void handQueryInventoryAsync() {
        Log.i(">>>>", "handAutoQueryInventoryAsync");
        try {
            if (mHelper != null) {
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        } catch (IabHelper.IabAsyncInProgressException e) {
//                    complain("Error querying inventory. Another async operation in progress.");
            if (listener != null) {
                listener.onGgStatus(GooglePayStatus.QUERY_ERROR);
            }
        }
    }

    //TODO  购买的时候传入
//    private String productId;
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotProductDetailsListener = new IabHelper.QueryInventoryFinishedListener() {

        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null || result == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                if (queryItemDetailListener != null) {
                    queryItemDetailListener.queryFailed(result.mResponse, result.mMessage);
                }
            } else {
                Set<Map.Entry<String, SkuDetails>> entries = inventory.mSkuMap.entrySet();
                if (entries.isEmpty()) {
                    if (queryItemDetailListener != null) {
                        queryItemDetailListener.queryIdNoExist();
                    }
                } else {
                    for (Map.Entry<String, SkuDetails> item : entries) {
                        if (queryItemDetailListener != null) {
                            SkuDetails value = item.getValue();
                            queryItemDetailListener.querySuccess(value.getPriceAmountMicros(), value.getPriceCurrencyCode(), value.getTitle());
                        }
                    }
                }
            }


            Log.d(TAG, "Query inventory was successful.");

        }

        @Override
        public void onFailed(int status, String string) {

        }
    };
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
                    if (isAutoConsume()) {
                        try {
                            mHelper.consumeAsync(item.getValue(), mConsumeFinishedListener);
                            Log.d(TAG, "We have gas. Consuming it successful." + item.getKey());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            if (listener != null) {
                                listener.onGgStatus(GooglePayStatus.CONSUME_ERROR);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.unConsumeAsync(item.getValue());
                        }
                    }
                }
            }

        }

        @Override
        public void onFailed(int status, String string) {
            Log.i(">>>>", "string = >" + string);
            if (listener != null) {
                listener.onGgStatus(status);
            }
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
                    listener.onGgStatus(GooglePayStatus.CONSUME_FAILED);
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
                if (listener != null && result.mResponse == IabHelper.IABHELPER_USER_CANCELLED) {
                    listener.cancelPurchase();
                } else if (listener != null && result.mResponse == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                    listener.haveGoodsUnConsume();
                } else if (listener != null) {
                    listener.ohterError(result.mResponse, result.mMessage);
                }
                return;
            }
            Log.d(TAG, "Purchase successful.");
            Log.d(TAG, "Purchase is gas. Starting gas consumption.");
            if (isAutoConsume()) {
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    return;
                }
            }
        }
    };

    /**
     * 该方法用户手动消耗购买的商品。默认情况下，购买成功后不消耗商品。需要调用该方法。
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
        try {
            mHelper.consumeAsync(purchase, mConsumeFinishedListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
//                    complain("Error consuming gas. Another async operation in progress.");
//                    setWaitScreen(false);
            return;
        }

    }


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
                listener.onGgStatus(GooglePayStatus.SUBS_FAILED);
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
                listener.onGgStatus(GooglePayStatus.INAPP_FAILED);
            }
        } catch (IllegalStateException e) {
            if (listener != null) {
                listener.onGgStatus(GooglePayStatus.CHECK_INDENTITY_AUTH);
            }
        }
    }


    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * 释放资源，必须调用的方法。
     */
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
