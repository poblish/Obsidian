package org.hiatusuk.obsidian.snaps

import java.awt.Color
import java.awt.image.BufferedImage

object ImageComparisons {

    // Copied from http://stackoverflow.com/a/25024344/954442
    fun getDifferenceImage(img1: BufferedImage, img2: BufferedImage): BufferedImage {
        val width1 = img1.width // Change - getWidth() and getHeight() for BufferedImage
        val width2 = img2.width // take no arguments
        val height1 = img1.height
        val height2 = img2.height
        if (width1 != width2 || height1 != height2) {
            System.err.println("Error: Images dimensions mismatch")
            System.exit(1)
        }

        // NEW - Create output Buffered image of type RGB
        val outImg = BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB)

        // Modified - Changed to int as pixels are ints
        var diff: Int
        var result: Int // Stores output pixel
        for (i in 0 until height1) {
            for (j in 0 until width1) {
                val rgb1 = img1.getRGB(j, i)
                val rgb2 = img2.getRGB(j, i)
                val r1 = rgb1 shr 16 and 0xff
                val g1 = rgb1 shr 8 and 0xff
                val b1 = rgb1 and 0xff
                val r2 = rgb2 shr 16 and 0xff
                val g2 = rgb2 shr 8 and 0xff
                val b2 = rgb2 and 0xff
                diff = Math.abs(r1 - r2) // Change
                diff += Math.abs(g1 - g2)
                diff += Math.abs(b1 - b2)
                diff /= 3 // Change - Ensure result is between 0 - 255
                // Make the difference image gray scale
                // The RGB components are all the same
                result = diff shl 16 or (diff shl 8) or diff
                outImg.setRGB(j, i, result) // Set result
            }
        }

        // Now return
        return outImg
    }

    // Copied from http://stackoverflow.com/a/25151302/954442
    // => 70-80% faster in my test
    fun getDifferenceImage2(img1: BufferedImage, img2: BufferedImage): BufferedImage? {
        var same = true
        val w = Math.min(img1.width, img2.width)  // Meaningless, but have to do to prevent ArrayIndexOutOfBoundsException on diff-sized images
        val h = Math.min(img1.height, img2.height)  // " " "
        val highlight = Color.MAGENTA.rgb
        val p1 = img1.getRGB(0, 0, w, h, null, 0, w)
        val p2 = img2.getRGB(0, 0, w, h, null, 0, w)

        for (i in p1.indices) {
            if (p1[i] != p2[i]) {
                p1[i] = highlight  // Well, what about fuzz? Back to the other algo?
                same = false
            }
        }

        if (same) {
            return null
        }

        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, w, h, p1, 0, w)
        return out
    }
}
