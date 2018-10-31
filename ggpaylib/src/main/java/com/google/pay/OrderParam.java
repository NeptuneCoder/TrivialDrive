package com.google.pay;

import java.io.Serializable;

public class OrderParam implements Serializable {
    public String purchaseData;
    public String dataSignature;
    public int responseCode;
    public int resultCode;
    public String currBuyType = ""; // 表示当前是内购还是订阅：// Item types
//        IabHelper
//        public static final String ITEM_TYPE_INAPP = "inapp";
//        public static final String ITEM_TYPE_SUBS = "subs";

    public boolean isInapp() {
        return currBuyType.equals(IabHelper.ITEM_TYPE_INAPP);
    }


    public boolean isSubs() {
        return currBuyType.equals(IabHelper.ITEM_TYPE_SUBS);
    }

    @Override
    public String toString() {
        return "{" +
                "purchaseData:'" + purchaseData + '\'' +
                ", dataSignature:'" + dataSignature + '\'' +
                ", responseCode:" + responseCode +
                ", resultCode=" + resultCode +
                ", currBuyType:'" + currBuyType + '\'' +
                '}';
    }
}
