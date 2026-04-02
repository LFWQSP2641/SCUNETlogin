package com.lfwqsp2641.scunet_login.helper

import java.math.BigInteger

object LegacyCampusRsaEncryptor {
    private val PUBLIC_EXPONENT_HEX = "10001"
    private val MODULUS_HEX =
        "94dd2a8675fb779e6b9f7103698634cd400f27a154afa67af6166a43fc26417222a79506d34cacc7641946abda1785b7acf9910ad6a0978c91ec84d40b71d2891379af19ffb333e7517e390bd26ac312fe940c340466b4a5d4af1d65c3b5944078f96a1a51a5a53e4bc302818b7c9f63c4a1b07bd7d874cef1c3d4b2f5eb7871"

    private val e = BigInteger(PUBLIC_EXPONENT_HEX, 16)
    private val n = BigInteger(MODULUS_HEX, 16)

    private val chunkSize = 2 * ((n.bitLength() + 15) / 16)
    private val modulusHexLength = (n.bitLength() + 3) / 4

    fun encryptedPassword(password: String, mac: String): String {
        val plain = ("$password>$mac").reversed()
        return encryptedStringLegacy(plain)
    }

    private fun encryptedStringLegacy(s: String): String {
        val a = s.map { it.code }.toMutableList()

        while (a.size % chunkSize != 0) a.add(0)

        val out = ArrayList<String>(a.size / chunkSize)

        var i = 0
        while (i < a.size) {
            var block = BigInteger.ZERO
            var j = 0
            var k = i
            while (k < i + chunkSize && k < a.size) {
                val low = a[k]
                val high = if (k + 1 < a.size) a[k + 1] else 0
                val v = low or (high shl 8) // 与 Python 逻辑一致
                block = block.or(BigInteger.valueOf(v.toLong()).shiftLeft(16 * j))
                j++
                k += 2
            }

            val crypt = block.modPow(e, n)
            val hex = crypt.toString(16).padStart(modulusHexLength, '0')
            out.add(hex)

            i += chunkSize
        }

        return out.joinToString(" ")
    }
}