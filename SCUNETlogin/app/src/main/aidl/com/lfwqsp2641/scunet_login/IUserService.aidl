// IUserService.aidl
package com.lfwqsp2641.scunet_login;

interface IUserService {
    void destroy() = 16777114;
    void grantPermission(String packageName, String permission) = 1;
    String getCurrentNetworkSsid() = 2;
    boolean connectToOpenWifi(String ssid) = 3;
    boolean disconnectWifi() = 4;
}