/*
 * The phone's IPv4 address on the Wi-Fi network, watched for changes.
 */
package com.kelly.reader.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address

object WifiAddress {

    /**
     * Emits the Wi-Fi IPv4 address, or null when the phone is not on Wi-Fi, now and whenever
     * the default network changes.
     */
    fun observe(context: Context): Flow<String?> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        trySend(current(manager))
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(current(manager))
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                trySend(current(manager))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(current(manager))
            }

            override fun onLost(network: Network) {
                trySend(null)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * The IPv4 address of the active network when it is Wi-Fi, else null.
     */
    fun current(manager: ConnectivityManager): String? {
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val addresses = manager.getLinkProperties(network)?.linkAddresses ?: return null
        return addresses
            .map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }
}
