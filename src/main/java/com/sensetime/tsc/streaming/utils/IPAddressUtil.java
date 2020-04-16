package com.sensetime.tsc.streaming.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/16 14:37
 */
public class IPAddressUtil {


    /**
     * 获取服务器IP
     * @return
     * @throws SocketException
     */
    public static String getIpAddress () throws SocketException {
        InetAddress site = null;
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();) {
            NetworkInterface e = n.nextElement();
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();) {
                InetAddress addr = a.nextElement();
                if (addr.isSiteLocalAddress()) {
                    site = addr;
                }
            }
        }
        String res = null;
        if (site != null) {
            res = site.getHostAddress();
        }
        return res;
    }

}
