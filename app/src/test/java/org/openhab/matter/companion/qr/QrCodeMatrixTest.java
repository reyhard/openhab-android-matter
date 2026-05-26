package org.openhab.matter.companion.qr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class QrCodeMatrixTest {
    @Test
    public void encodesMatterQrPayloadAsSquareMatrixWithFinderPatterns() {
        QrCodeMatrix matrix = QrCodeMatrix.encodeText("MT:Y.K9042C00KA0648G00");

        assertTrue(matrix.size() >= 21);
        assertEquals(matrix.size(), matrix.modules().length);
        assertEquals(matrix.size(), matrix.modules()[0].length);
        assertFinderPattern(matrix, 0, 0);
        assertFinderPattern(matrix, matrix.size() - 7, 0);
        assertFinderPattern(matrix, 0, matrix.size() - 7);
    }

    @Test
    public void rejectsPayloadsThatDoNotFitSupportedVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> QrCodeMatrix.encodeText("x".repeat(79)));
    }

    private static void assertFinderPattern(QrCodeMatrix matrix, int row, int column) {
        boolean[][] modules = matrix.modules();
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                boolean expected = y == 0 || y == 6 || x == 0 || x == 6 || (y >= 2 && y <= 4 && x >= 2 && x <= 4);
                assertEquals("finder pattern mismatch at " + (row + y) + "," + (column + x),
                        expected,
                        modules[row + y][column + x]);
            }
        }
    }
}
