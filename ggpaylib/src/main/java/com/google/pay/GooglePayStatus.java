package com.google.pay;

public abstract class GooglePayStatus {

    public static final int PARCHASE_CANCEL = -1005; //
    public static final int QUERY_ERROR = 10002; //查询异常
    public static final int QUERY_FAILED = 10003; //查询失败
    public static final int CONSUME_ERROR = 10004; //消耗异常
    public static final int CONSUME_FAILED = 10006; //消耗失败

    public static final int SUBS_FAILED = 10008; //调用订阅方法失败

    public static final int INAPP_FAILED = 10009; //调用购买方法失败

    /**
     * 初始化失败是，调用该方法。在调用购买时，需要对该参数进行判断
     * https://www.jianshu.com/p/87ffdb7bc439
     * 该文章有提到解决方法
     *
     * @param boo
     */
    public abstract void initFailed(boolean boo);

    /**
     * 各种的支付过程中的状态
     *
     * @param code
     */
    public abstract void onGgStatus(int code);

    /**
     * 调用购买或订阅成功时会回调该方法
     *
     * @param data 返回Google  pay购买成功返回的参数
     */
    public abstract void onBuySuccess(OrderParam data);

    /**
     * 调用消耗商品成功
     */
    public abstract void onConsumeSuccess();

    /**
     * <p>
     * public void setIsAutoConsume(boolean isAutoConsume) {
     * this.isAutoConsume = isAutoConsume;
     * }
     * <p>
     * 当setIsAutoConsume 设置为true时，
     * 将会回调该方法，根据自己的业务逻辑进行处理消耗商品逻辑
     *
     * @param purchase
     */
    public void unConsumeAsync(Purchase purchase) {

    }

    public void cancelPurchase() {

    }

    /**
     * 购买时，有未消耗的商品，回调该方法
     */
    public void haveGoodsUnConsume() {

    }

    public void ohterError(int status, String error) {

    }

}
