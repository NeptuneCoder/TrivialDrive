package com.google.pay;

public interface IQueryProductDetailListener {

    void querySuccess(long price, String currency);

    void queryFailed();

}
