package com.example.smarthome.dao;

public class DeviceBean {
    private String deviceID;
    private String deviceName;
    private String deviceType;
    private String deviceCode;
    private String scenDoor;
    private String scenVoice;
    private String scenTimer;
    private String scenAuto;

    public DeviceBean(){

    }

    public DeviceBean(DeviceBean deviceBean) {
        this.deviceID = deviceBean.deviceID;
        this.deviceName = deviceBean.deviceName;
        this.deviceType = deviceBean.deviceType;
        this.deviceCode = deviceBean.deviceCode;
        this.scenDoor = deviceBean.scenDoor;
        this.scenVoice = deviceBean.scenVoice;
        this.scenTimer = deviceBean.scenTimer;
        this.scenAuto = deviceBean.scenAuto;
    }

    public DeviceBean(String deviceID, String deviceName, String deviceType, String deviceCode, String scenDoor, String scenVoice, String scenTimer, String scenAuto) {
        this.deviceID = deviceID;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.deviceCode = deviceCode;
        this.scenDoor = scenDoor;
        this.scenVoice = scenVoice;
        this.scenTimer = scenTimer;
        this.scenAuto = scenAuto;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getScenDoor() {
        return scenDoor;
    }

    public void setScenDoor(String scenDoor) {
        this.scenDoor = scenDoor;
    }

    public String getScenVoice() {
        return scenVoice;
    }

    public void setScenVoice(String scenVoice) {
        this.scenVoice = scenVoice;
    }

    public String getScenTimer() {
        return scenTimer;
    }

    public void setScenTimer(String scenTimer) {
        this.scenTimer = scenTimer;
    }

    public String getScenAuto() {
        return scenAuto;
    }

    public void setScenAuto(String scenAuto) {
        this.scenAuto = scenAuto;
    }
}
