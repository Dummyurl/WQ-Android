package com.stratagile.qlink

import android.preference.PreferenceManager
import com.stratagile.qlink.Crypto.Decryptor
import com.stratagile.qlink.Crypto.EncryptedSettingsRepository
import com.stratagile.qlink.Crypto.EncryptedSettingsRepository.setProperty
import com.stratagile.qlink.Crypto.Encryptor
import com.stratagile.qlink.R.id.wif
import com.stratagile.qlink.application.AppConfig
import neoutils.Neoutils.generateFromWIF
import neoutils.Neoutils.generateFromPrivateKey
import neoutils.Wallet
import java.lang.Exception
import java.security.SecureRandom

/**
 * Created by drei on 11/22/17.
 */

object Account {
    private var wallet: Wallet? = null
    private var sharedSecretPieceOne: String? = null

    /**
     * 将加密密钥存储在设备上
     */
    private fun storeEncryptedKeyOnDevice() {
        val wif = wallet!!.wif
        val encryptor = Encryptor()
        val alias = "O3 Key"
        val encryptedWIF = encryptor.encryptText(alias, wif)!!

        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedWIF.toHex(), iv, AppConfig.instance!!)
    }

    fun storeColdStorageKeyFragmentOnDevice(keyFragment: String) {
        val alias = "Cold Storage Key Fragment"
        val encryptor = Encryptor()
        val encryptedFragment = encryptor.encryptText(alias, keyFragment)!!
        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedFragment.toHex(), iv, AppConfig.instance!!)
    }

    fun getColdStorageKeyFragmentOnDevice(): String {
        val alias = "Cold Storage Key Fragment"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, AppConfig.instance!!)
        if (storedVal?.data == null) {
            return ""
        }
        val storedEncryptedFragment = storedVal?.data?.hexStringToByteArray()
        if (storedEncryptedFragment == null || storedEncryptedFragment.size == 0) {
            return ""
        }
        val storedIv = storedVal?.iv!!
        val decrypted = Decryptor().decrypt(alias, storedEncryptedFragment, storedIv)
        return decrypted
    }

    fun removeColdStorageKeyFragment() {
        storeColdStorageKeyFragmentOnDevice("")
    }

    fun isEncryptedWalletPresent(): Boolean {
        val alias = "O3 Key"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, AppConfig.instance!!)
        if (storedVal?.data == null) {
            return false
        }
        val storedEncryptedWIF = storedVal?.data?.hexStringToByteArray()
        if (storedEncryptedWIF == null || storedEncryptedWIF.size == 0 || !Decryptor().keyStoreEntryExists("O3 Key")) {
            return false
        }
        return true
    }

    fun restoreWalletFromDevice() {
        val alias = "O3 Key"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, AppConfig.instance!!)
        val storedEncryptedWIF = storedVal?.data?.hexStringToByteArray()!!
        val storedIv = storedVal?.iv!!
        val decrypted = Decryptor().decrypt(alias, storedEncryptedWIF, storedIv)
        wallet = generateFromWIF(decrypted)
    }

    fun createNewWallet() {
        val random = SecureRandom()
        var bytes = ByteArray(32)
        random.nextBytes(bytes)
        val hex = bytes.toHex()
        wallet = generateFromPrivateKey(hex)
//        storeEncryptedKeyOnDevice()
    }

    fun deleteKeyFromDevice() {
        val alias = "O3 Key"
        setProperty(alias, "", kotlin.ByteArray(0), AppConfig.instance!!)
    }
    
    fun fromWIF(wif: String): Boolean{
        //Java does not support multiple return values from function.
        //The function that is generated by go mobile bind. If the second return value is Error type, it will throw the generic exception
        try {
            wallet = generateFromWIF(wif)
        } catch (e: Exception) {
            return false
        }

//        storeEncryptedKeyOnDevice()
        return true
    }

    fun fromHex(hex: String) : Boolean {
        try {
            wallet = generateFromPrivateKey(hex)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun getWallet(): Wallet? {
        return wallet
    }

    fun byteArray2String(byteArray: ByteArray) : String {
        return byteArray.toHex()
    }
}