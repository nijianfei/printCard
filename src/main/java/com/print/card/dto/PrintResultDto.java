package com.print.card.dto;

import java.util.Objects;

public class PrintResultDto {
    private String accepted;
    private String printStatus;
    private String printSubStatusStatus;
    private String errorCode;
    private String errorMsg;

    public String getAccepted() {
        return accepted;
    }

    public void setAccepted(String accepted) {
        this.accepted = accepted;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    public String getPrintSubStatusStatus() {
        return printSubStatusStatus;
    }

    public void setPrintSubStatusStatus(String printSubStatusStatus) {
        this.printSubStatusStatus = printSubStatusStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public boolean isAccepted(){
        return Objects.equals(accepted, "true");
    }
}
