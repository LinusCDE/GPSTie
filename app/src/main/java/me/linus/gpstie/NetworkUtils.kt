package me.linus.gpstie

import java.net.NetworkInterface

/**
 * Get Local Ip in WiFi or AP
 * Thanks to https://stackoverflow.com/a/15060411/3949509 for this solution
 */
fun getLocalIp(): String? {
    for(networkInterface in NetworkInterface.getNetworkInterfaces()){
        if(networkInterface.name.contains("wlan"))
            for(ipAddress in networkInterface.inetAddresses)
                if(ipAddress.address.size == 4 /* = is IPv4 */
                        && !ipAddress.isLoopbackAddress /* = is not 127.0.0.1 */)
                    return ipAddress.hostAddress
    }

    return null // = No WiFi connected or no (detected) AP active
}