package org.openhab.matter.companion.ui

import android.content.Context
import java.io.IOException
import java.nio.charset.StandardCharsets

data class AboutLegalContent(
    val versionName: String,
    val appLicense: String,
    val thirdPartyNotices: String,
    val thirdPartyLicenses: String
)

interface LegalAssetReader {
    fun readText(path: String): String
}

class AndroidLegalAssetReader(
    private val context: Context
) : LegalAssetReader {
    override fun readText(path: String): String {
        return context.assets.open(path).use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
    }
}

object AboutLegalContentLoader {
    private const val APP_LICENSE_PATH = "legal/app-license.txt"
    private const val THIRD_PARTY_NOTICES_PATH = "legal/third-party-notices.txt"
    private const val CONNECTEDHOMEIP_LICENSE_PATH = "legal/connectedhomeip-apache-2.0.txt"

    fun load(versionName: String, reader: LegalAssetReader): AboutLegalContent {
        return AboutLegalContent(
            versionName = versionName,
            appLicense = readOrFallback(reader, APP_LICENSE_PATH),
            thirdPartyNotices = readOrFallback(reader, THIRD_PARTY_NOTICES_PATH),
            thirdPartyLicenses = readOrFallback(reader, CONNECTEDHOMEIP_LICENSE_PATH)
        )
    }

    private fun readOrFallback(reader: LegalAssetReader, path: String): String {
        return try {
            reader.readText(path).trim()
        } catch (e: IOException) {
            "Legal text is unavailable: $path"
        } catch (e: IllegalStateException) {
            "Legal text is unavailable: $path"
        }
    }
}
