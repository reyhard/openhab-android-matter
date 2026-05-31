package org.openhab.matter.companion.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatterSetupIconResourceTest {
    @Test
    fun materialAndWelcomeIconDrawablesExistAsVectors() {
        listOf(
            "ic_material_arrow_back",
            "ic_material_settings",
            "ic_welcome_easy",
            "ic_welcome_home",
            "ic_welcome_private"
        ).forEach { assertVectorDrawable(it) }
    }

    @Test
    fun welcomeAndTopBarUseDrawableIconResources() {
        assertSourceReferences("WelcomeScreen.kt", "R.drawable.ic_welcome_easy")
        assertSourceReferences("WelcomeScreen.kt", "R.drawable.ic_welcome_private")
        assertSourceReferences("WelcomeScreen.kt", "R.drawable.ic_welcome_home")
        assertSourceReferences("MatterSetupScaffold.kt", "R.drawable.ic_material_arrow_back")
        assertSourceReferences("MatterSetupScaffold.kt", "R.drawable.ic_material_settings")
    }

    @Test
    fun launcherForegroundUsesOpenhabIconSvgArtwork() {
        val sourceSvg = interfaceFile("openhab-icon.svg").readText()
        val launcherVector = drawableFile("ic_launcher_foreground.xml").readText()

        assertTrue(sourceSvg.contains("id=\"orange_arrow\""), "Source SVG should contain the openHAB arrow path")
        assertTrue(sourceSvg.contains("inkscape:label=\"beta_background\""), "Source SVG should contain the beta badge")
        assertTrue(launcherVector.contains("M5.242,21.133"), "Launcher should use the requested openHAB arrow geometry")
        assertTrue(launcherVector.contains("M16,4C22.61,4"), "Launcher should use the requested openHAB circle geometry")
        assertTrue(launcherVector.contains("M16.10709,17.280769"), "Launcher should include the requested beta badge")
        assertTrue(launcherVector.contains("M15.617096,18.96373"), "Launcher should include the beta text paths")
        assertTrue(!launcherVector.contains("android:strokeColor"), "Launcher icon should not keep the old app-frame strokes")
        assertTrue(!launcherVector.contains("#1F67BF"), "Launcher icon should not keep the old blue app-frame color")
    }

    private fun assertVectorDrawable(resourceName: String) {
        val file = drawableFile("$resourceName.xml")
        assertTrue(file.isFile, "Missing drawable resource ${file.path}")

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        assertEquals("vector", root.nodeName)
        assertEquals("24", root.getAttribute("android:viewportWidth"))
        assertEquals("24", root.getAttribute("android:viewportHeight"))
        assertTrue(
            root.getElementsByTagName("path").length > 0,
            "$resourceName should contain at least one vector path"
        )
    }

    private fun assertSourceReferences(fileName: String, reference: String) {
        val sourceFile = sourceFile(fileName)
        assertTrue(sourceFile.isFile, "Missing source file ${sourceFile.path}")
        assertTrue(
            sourceFile.readText().contains(reference),
            "${sourceFile.path} should reference $reference"
        )
    }

    private fun drawableFile(name: String): File {
        return listOf(
            File("src/main/res/drawable", name),
            File("app/src/main/res/drawable", name)
        ).firstOrNull(File::isFile) ?: File("src/main/res/drawable", name)
    }

    private fun sourceFile(name: String): File {
        return listOf(
            File("src/main/kotlin/org/openhab/matter/companion/ui", name),
            File("src/main/kotlin/org/openhab/matter/companion/ui/components", name),
            File("app/src/main/kotlin/org/openhab/matter/companion/ui", name),
            File("app/src/main/kotlin/org/openhab/matter/companion/ui/components", name)
        ).firstOrNull(File::isFile) ?: File("src/main/kotlin/org/openhab/matter/companion/ui", name)
    }

    private fun interfaceFile(name: String): File {
        return listOf(
            File("docs/interface", name),
            File("../docs/interface", name),
            File("app/../docs/interface", name)
        ).firstOrNull(File::isFile) ?: File("docs/interface", name)
    }
}
