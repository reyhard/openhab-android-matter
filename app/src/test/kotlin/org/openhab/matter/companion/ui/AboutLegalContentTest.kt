package org.openhab.matter.companion.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AboutLegalContentTest {
    @Test
    fun loaderBuildsAboutContentFromExpectedLegalAssets() {
        val reader = FakeLegalAssetReader(
            mapOf(
                "legal/app-license.txt" to "GNU General Public License",
                "legal/third-party-notices.txt" to "connectedhomeip\nMatter SDK NOTICE",
                "legal/connectedhomeip-apache-2.0.txt" to "Apache License Version 2.0"
            )
        )

        val content = AboutLegalContentLoader.load("0.1.2", reader)

        assertEquals("0.1.2", content.versionName)
        assertTrue(content.appLicense.contains("GNU General Public License"))
        assertTrue(content.thirdPartyNotices.contains("connectedhomeip"))
        assertTrue(content.thirdPartyLicenses.contains("Apache License Version 2.0"))
        assertEquals(
            listOf(
                "legal/app-license.txt",
                "legal/third-party-notices.txt",
                "legal/connectedhomeip-apache-2.0.txt"
            ),
            reader.pathsRead
        )
    }

    @Test
    fun legalAssetsContainConnectedhomeipNoticeAndApacheLicense() {
        val notices = legalAsset("third-party-notices.txt")
        val license = legalAsset("connectedhomeip-apache-2.0.txt")

        assertTrue(notices.contains("connectedhomeip"), "Notices should name connectedhomeip")
        assertTrue(notices.contains("Matter SDK"), "Notices should include the Matter SDK NOTICE")
        assertTrue(notices.contains("This NOTICE must be included"), "Notices should preserve upstream NOTICE")
        assertTrue(notices.contains("Matter compliant"), "Notices should preserve certification limitations")
        assertTrue(license.contains("Apache License Version 2.0"), "Apache license text should be bundled")
        assertTrue(license.contains("Grant of Patent License"), "Apache patent grant should be present")
    }

    private fun legalAsset(name: String): String {
        return listOf(
            java.io.File("src/main/assets/legal", name),
            java.io.File("app/src/main/assets/legal", name)
        ).firstOrNull(java.io.File::isFile)?.readText()
            ?: error("Missing legal asset $name")
    }

    private class FakeLegalAssetReader(
        private val values: Map<String, String>
    ) : LegalAssetReader {
        val pathsRead = mutableListOf<String>()

        override fun readText(path: String): String {
            pathsRead += path
            return values.getValue(path)
        }
    }
}
