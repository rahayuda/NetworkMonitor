package com.example.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.SupplicantState
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var wifiStatusTextView: TextView
    private lateinit var signalStrengthTextView: TextView
    private lateinit var macAddressTextView: TextView
    private lateinit var networkTypeTextView: TextView
    private lateinit var dhcpInfoTextView: TextView
    private lateinit var networkRadiusTextView: TextView
    private lateinit var securitySummaryTextView: TextView
    private lateinit var securityConclusionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val checkNetworkButton = findViewById<Button>(R.id.checkNetworkButton)

        // Inisialisasi TextView baru
        wifiStatusTextView = findViewById(R.id.wifi_status)
        signalStrengthTextView = findViewById(R.id.signal_strength)
        macAddressTextView = findViewById(R.id.mac_address)
        networkTypeTextView = findViewById(R.id.network_type)
        dhcpInfoTextView = findViewById(R.id.dhcp_info)
        networkRadiusTextView = findViewById(R.id.network_radius)
        securitySummaryTextView = findViewById(R.id.security_summary)
        securityConclusionTextView = findViewById(R.id.security_conclusion)  // Inisialisasi

        checkNetworkButton.setOnClickListener {
            getNetworkStatus()
        }
    }

    private fun getNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Reset visibilitas semua TextView
        wifiStatusTextView.visibility = View.GONE
        signalStrengthTextView.visibility = View.GONE
        macAddressTextView.visibility = View.GONE
        networkTypeTextView.visibility = View.GONE
        dhcpInfoTextView.visibility = View.GONE
        networkRadiusTextView.visibility = View.GONE
        securitySummaryTextView.visibility = View.GONE  // Reset visibilitas
        securityConclusionTextView.visibility = View.GONE // Reset visibilitas

        // Deteksi Jaringan Wi-Fi
        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            wifiStatusTextView.text = Html.fromHtml("<b>Network:</b> Wi-Fi<br><b>Connectivity:</b> Connected<br><b>Status:</b> ${getWifiSecurityType()}", Html.FROM_HTML_MODE_LEGACY)
            wifiStatusTextView.visibility = View.VISIBLE
            getNetworkSpeed { speed ->
                wifiStatusTextView.append(Html.fromHtml("<br><b>Speed:</b> ${speed} Mbps", Html.FROM_HTML_MODE_LEGACY))
            }

            // Tambahkan informasi tambahan tentang Wi-Fi
            signalStrengthTextView.text = Html.fromHtml("<b>Signal Strength:</b> ${getSignalStrength()}", Html.FROM_HTML_MODE_LEGACY)
            signalStrengthTextView.visibility = View.VISIBLE

            macAddressTextView.text = Html.fromHtml("<b>MAC Address:</b> ${getMacAddress()}", Html.FROM_HTML_MODE_LEGACY)
            macAddressTextView.visibility = View.VISIBLE

            networkTypeTextView.text = Html.fromHtml("<b>Network Type:</b> Wi-Fi", Html.FROM_HTML_MODE_LEGACY)
            networkTypeTextView.visibility = View.VISIBLE

            dhcpInfoTextView.text = Html.fromHtml("<b>DHCP </b> ${getDhcpInfo()}", Html.FROM_HTML_MODE_LEGACY)
            dhcpInfoTextView.visibility = View.VISIBLE

            networkRadiusTextView.text = Html.fromHtml("<b>Network Radius </b> ${getNetworkRadius()}", Html.FROM_HTML_MODE_LEGACY)
            networkRadiusTextView.visibility = View.VISIBLE

            // Tambahkan kesimpulan keamanan
            val securitySummary = getSecuritySummary()
            securitySummaryTextView.text = securitySummary
            securitySummaryTextView.visibility = View.VISIBLE

            // Tampilkan kesimpulan berdasarkan ringkasan keamanan
            val securityConclusion = getSecurityConclusion()
            securityConclusionTextView.text = securityConclusion
            securityConclusionTextView.visibility = View.VISIBLE
        } else {
            wifiStatusTextView.text = Html.fromHtml("<b>Network:</b> Wi-Fi\n<b>Connectivity:</b> Off", Html.FROM_HTML_MODE_LEGACY)
            wifiStatusTextView.visibility = View.VISIBLE
        }
    }

    private fun getSecuritySummary(): String {
        // (kode yang sudah disediakan sebelumnya untuk menentukan ringkasan keamanan)
        // Tambahkan kode ini dari penjelasan sebelumnya
        val wifiSecurity = getWifiSecurityType()
        val signalStrength = getSignalStrength().removeSuffix(" dBm").toInt()  // Mengambil nilai integer dari string
        val dhcpInfo = getDhcpInfo().contains("192.168") // Menggunakan DHCP info untuk analisis

        return when {
            wifiSecurity.contains("WPA3") -> "Wi-Fi Network is Secure (WPA3)."
            wifiSecurity.contains("WPA2") && signalStrength > -70 -> "Wi-Fi Network is Secure (WPA2) with good signal strength."
            wifiSecurity.contains("WPA2") && signalStrength <= -70 -> "Wi-Fi Network is Secure (WPA2) but weak signal. Risk may be higher."
            wifiSecurity.contains("WPA") && dhcpInfo -> "Wi-Fi Network is somewhat secure (WPA) but use with caution."
            wifiSecurity.contains("WPA") -> "Wi-Fi Network is insecure (WPA). Consider upgrading."
            else -> "Wi-Fi Network is Unsecure (Open Network). Consider using a secure connection."
        }
    }

    private fun getSecurityConclusion(): String {
        val wifiSecurity = getWifiSecurityType()
        return when {
            wifiSecurity.contains("WPA3") -> "You are connected to a highly secure network."
            wifiSecurity.contains("WPA2") -> "Consider upgrading to WPA3 for better security."
            wifiSecurity.contains("WPA") -> "The network is vulnerable, avoid sensitive transactions."
            else -> "The network is unsecure, avoid using this connection."
        }
    }

    @Suppress("DEPRECATION")
    private fun getWifiSecurityType(): String {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
            val ssid = wifiInfo.ssid

            when {
                ssid.startsWith("\"WPA3") -> "Secure (WPA3)"
                ssid.startsWith("\"WPA2") -> "Secure (WPA2)"
                ssid.startsWith("\"WPA") -> "Secure (WPA)"
                else -> "Unsecure (Open Network)"
            }
        } else {
            "Not connected to Wi-Fi"
        }
    }
    @Suppress("DEPRECATION")
    private fun getSignalStrength(): String {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        return "${wifiManager.connectionInfo.rssi} dBm"
    }
    @Suppress("DEPRECATION")
    private fun getMacAddress(): String {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo.macAddress ?: "Not available"
    }
    @Suppress("DEPRECATION")
    private fun getDhcpInfo(): String {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        return "IP: ${intToIp(dhcpInfo.ipAddress)}, Gateway: ${intToIp(dhcpInfo.gateway)}"
    }

    private fun intToIp(ip: Int): String {
        return String.format("%d.%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff),
            (ip shr 24 and 0xff))
    }

    private fun getNetworkRadius(): String {
        // Implementasi untuk mendapatkan jangkauan jaringan, jika perlu
        return "Range: Unknown"
    }

    private fun getNetworkSpeed(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            val url = URL("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png")
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            var downloadedBytes = 0

            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (true) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                downloadedBytes += bytesRead
            }
            inputStream.close()

            val endTime = System.currentTimeMillis()
            val downloadTimeInSeconds = (endTime - startTime) / 1000.0
            val speedInMbps = if (downloadTimeInSeconds > 0) (downloadedBytes * 8 / 1024.0 / 1024.0) / downloadTimeInSeconds else 0.0

            launch(Dispatchers.Main) {
                callback(String.format("%.2f", speedInMbps))
            }
        }
    }
}
