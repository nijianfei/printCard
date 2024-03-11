package com.print.card.dto;

public class Template {

    /**
     * 人脸照片
     */
    private Block photo;
    /**
     * 人员姓名
     */
    private Block userName;
    /**
     * 部门名称
     */
    private Block deptName;

    /**
     * 部门名称
     */
    private Block deptName2;

    /**
     * 工号
     */
    private Block userId;
    /**
     * 工卡A面
     */
    private String foregroundImage;
    /**
     * 工卡B面
     */
    private String backgroundImage;

    public Block getPhoto() {
        return photo;
    }

    public void setPhoto(Block photo) {
        this.photo = photo;
    }

    public Block getUserName() {
        return userName;
    }

    public void setUserName(Block userName) {
        this.userName = userName;
    }

    public Block getDeptName() {
        return deptName;
    }

    public void setDeptName(Block deptName) {
        this.deptName = deptName;
    }

    public Block getDeptName2() {
        return deptName2;
    }

    public void setDeptName2(Block deptName2) {
        this.deptName2 = deptName2;
    }

    public Block getUserId() {
        return userId;
    }

    public void setUserId(Block userId) {
        this.userId = userId;
    }

    public String getForegroundImage() {
        return foregroundImage;
    }

    public void setForegroundImage(String foregroundImage) {
        this.foregroundImage = foregroundImage;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
}
