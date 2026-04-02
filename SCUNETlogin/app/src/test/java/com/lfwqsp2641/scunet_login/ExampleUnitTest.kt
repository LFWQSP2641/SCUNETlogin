package com.lfwqsp2641.scunet_login

import com.lfwqsp2641.scunet_login.helper.LegacyCampusRsaEncryptor
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

class CampusRsaEncryptorTest {
    @Test
    fun testEncrypt() {
        val expectedResult = "0f8c069affb97231" +
                "446dce90164fd7d5" +
                "32b6e1a9fbe1d57e" +
                "3d9f0794cc66b38e" +
                "7161ba9985412021" +
                "89626c132989e3ba" +
                "ab7f27f361ad5b4a" +
                "ca9589d42af9046a" +
                "31a1ced0f46687f3" +
                "7175917d6563962a" +
                "95937b283b725ab2" +
                "18d90b43777db40b" +
                "0325baa07447a21b" +
                "aa892d9606324198" +
                "5df2af279928d742" +
                "ea29dd6f0174e12f"

        val password = "password9977"
        val mac = "eef900330a8987f0957c14c756513384"
        val encrypted = LegacyCampusRsaEncryptor.encryptedPassword(password, mac)

        assertEquals(expectedResult, encrypted)
    }
}