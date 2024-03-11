package com.print.card.model;


public class ResponseModel {
    private String reqNo;
    private String cardId;
    private String invokeCls = "70";
    private String ngMessage;

    public ResponseModel() {
    }

    public ResponseModel(String reqNo, String cardId, String invokeCls, String ngMessage) {
        this.reqNo = reqNo;
        this.cardId = cardId;
        this.invokeCls = invokeCls;
        this.ngMessage = ngMessage;
    }

    public String getReqNo() {
        return reqNo;
    }

    public void setReqNo(String reqNo) {
        this.reqNo = reqNo;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getInvokeCls() {
        return invokeCls;
    }

    public void setInvokeCls(String invokeCls) {
        this.invokeCls = invokeCls;
    }

    public String getNgMessage() {
        return ngMessage;
    }

    public void setNgMessage(String ngMessage) {
        this.ngMessage = ngMessage;
    }

    public static ResponseModel success(String requestCode, String cardId) {
        return new ResponseModel(requestCode, cardId, "70", null);
    }

    public static ResponseModel fail(String requestCode, String message) {
        return new ResponseModel(requestCode, null, "90", message);
    }
}
