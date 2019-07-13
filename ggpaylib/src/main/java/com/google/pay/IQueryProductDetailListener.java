package com.google.pay;

public interface IQueryProductDetailListener {

    void querySuccess(SkuDetails value);

    void queryFailed(int status,String msg);

    default void queryGoodsIdNoExist(){
    }

}
