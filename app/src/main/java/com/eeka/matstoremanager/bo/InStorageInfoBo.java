package com.eeka.matstoremanager.bo;

public class InStorageInfoBo {

    /**
     * CLOTH_TYPE : KZ
     * STOR_AREA : A
     * STOR_LOCATION : A-2
     * SHOP_ORDER : AAAA
     * WORK_CENTER : YDHL001
     * ITEM : ZSNV0006
     * RFID : 123
     * SIZE : 38
     * QUANTITY : 20
     * USER_ID : wangshuai
     */

    private String CLOTH_TYPE;
    private String STOR_AREA;
    private String STOR_LOCATION;
    private String SHOP_ORDER;
    private String WORK_CENTER;
    private String ITEM;
    private String RFID;
    private String SIZE;
    private String STOR_QUANTITY;
    private String ME_QUANTITY;
    private String USER_ID;
    private boolean isIn;//是否已入库

    public boolean isIn() {
        return isIn;
    }

    public void setIn(boolean in) {
        isIn = in;
    }

    public String getME_QUANTITY() {
        return ME_QUANTITY;
    }

    public void setME_QUANTITY(String ME_QUANTITY) {
        this.ME_QUANTITY = ME_QUANTITY;
    }

    public String getSTOR_QUANTITY() {
        return STOR_QUANTITY;
    }

    public void setSTOR_QUANTITY(String STOR_QUANTITY) {
        this.STOR_QUANTITY = STOR_QUANTITY;
    }

    public String getCLOTH_TYPE() {
        return CLOTH_TYPE;
    }

    public void setCLOTH_TYPE(String CLOTH_TYPE) {
        this.CLOTH_TYPE = CLOTH_TYPE;
    }

    public String getSTOR_AREA() {
        return STOR_AREA;
    }

    public void setSTOR_AREA(String STOR_AREA) {
        this.STOR_AREA = STOR_AREA;
    }

    public String getSTOR_LOCATION() {
        return STOR_LOCATION;
    }

    public void setSTOR_LOCATION(String STOR_LOCATION) {
        this.STOR_LOCATION = STOR_LOCATION;
    }

    public String getSHOP_ORDER() {
        return SHOP_ORDER;
    }

    public void setSHOP_ORDER(String SHOP_ORDER) {
        this.SHOP_ORDER = SHOP_ORDER;
    }

    public String getWORK_CENTER() {
        return WORK_CENTER;
    }

    public void setWORK_CENTER(String WORK_CENTER) {
        this.WORK_CENTER = WORK_CENTER;
    }

    public String getITEM() {
        return ITEM;
    }

    public void setITEM(String ITEM) {
        this.ITEM = ITEM;
    }

    public String getRFID() {
        return RFID;
    }

    public void setRFID(String RFID) {
        this.RFID = RFID;
    }

    public String getSIZE() {
        return SIZE;
    }

    public void setSIZE(String SIZE) {
        this.SIZE = SIZE;
    }

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String USER_ID) {
        this.USER_ID = USER_ID;
    }
}
