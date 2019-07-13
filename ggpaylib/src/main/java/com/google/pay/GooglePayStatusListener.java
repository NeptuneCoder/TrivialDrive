package com.google.pay;

public interface GooglePayStatusListener {

    int PARCHASE_CANCEL = -1005; //
    int QUERY_ERROR = 10002; //查询异常
    int QUERY_FAILED = 10003; //查询失败
    int CONSUME_ERROR = 10004; //消耗异常
    int CONSUME_FAILED = 10006; //消耗失败
    int SUBS_FAILED = 10008; //调用订阅方法失败
    int INAPP_FAILED = 10009; //调用购买方法失败
    int CHECK_INDENTITY_AUTH = 10011; //调用购买方法失败

    /**
     * 初始化失败是，调用该方法。在调用购买时，需要对该参数进行判断
     * https://www.jianshu.com/p/87ffdb7bc439
     * 该文章有提到解决方法
     *
     * @param boo true 初始化成功
     *            false  初始化失败
     */
    void initStatus(boolean boo);

    /**
     * 各种的支付过程中的状态
     *
     * @param code
     */
    default void onErrorCode(int code) {

    }

    /**
     * 调用购买或订阅成功时会回调该方法
     *
     * @param data 返回Google  pay购买成功返回的参数
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
     * 当setIsAutoConsume 设置为true时，
     * 将会回调该方法，根据自己的业务逻辑进行处理消耗商品逻辑
     *
     * @param purchase
     */
    default void unConsumeGoodsInfo(Purchase purchase) {

    }

    default void cancelPurchase() {

    }

    /**
     * 购买时，有未消耗的商品，回调该方法
     */
    default void haveUnConsumeGoods(Purchase purchase) {

    }

    default void otherError(int status, String errorMsg) {

    }

}
