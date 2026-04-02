package com.android.messaging.util

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUtilsTest {

    @Test
    fun getOrientation_returnsDefaultForNonJpegStream() {
        val inputStream: InputStream = ByteArrayInputStream(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
        )

        assertEquals(0, ImageUtils.getOrientation(inputStream))
    }

    @Test
    fun getOrientation_handlesInputStreamWithoutMarkSupport() {
        val inputStream: InputStream = NonMarkSupportingInputStream(
            buffer = byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
            ),
        )

        assertEquals(0, ImageUtils.getOrientation(inputStream))
    }

    private class NonMarkSupportingInputStream(
        buffer: ByteArray,
    ) : ByteArrayInputStream(buffer) {

        override fun markSupported(): Boolean {
            return false
        }
    }
}
