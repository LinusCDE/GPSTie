package me.linus.gpstie

import android.util.Base64
import java.io.*
import java.net.Socket
import java.nio.charset.Charset
import java.security.KeyPairGenerator
import java.security.PublicKey
import javax.crypto.*

// TODO: Enhance encryption. This type of encryption is not super secure but decent enough for now.

/**
 * Performs a Handshake to securely receive a SecretKey for use with AES.
 */
fun receiveSecretAESKey(s: Socket): SecretKey {
    // Generate RSA Key:
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(1024)
    val rsaKeyPair = keyGen.generateKeyPair()

    // Send public RSA Key:
    ObjectOutputStream(s.getOutputStream()).writeObject(rsaKeyPair.public)

    // Receive the encoded SecretAESKey:
    val keySize = DataInputStream(s.getInputStream()).readInt()
    val encoded = ByteArray(keySize)
    s.getInputStream().read(encoded)

    // Unwrap encrypted secretKey:
    val rsaCipher = Cipher.getInstance("RSA")
    rsaCipher.init(Cipher.UNWRAP_MODE, rsaKeyPair.private)
    val secretAesKey = rsaCipher.unwrap(encoded, "RSA", Cipher.SECRET_KEY)
    return secretAesKey as SecretKey
}

/**
 * Performs a Handshake to securely generate and send SecretKey for use with AES.
 */
fun generateSecretKey(s: Socket): SecretKey {
    // Receive Public RSA Key
    val rsaPublicKey = ObjectInputStream(s.getInputStream()).readObject() as PublicKey


    // Generate AES SecretKey:
    val aesKeyGen = KeyGenerator.getInstance("AES")
    aesKeyGen.init(256)
    val secretKey = aesKeyGen.generateKey()

    // Wrap the AES SecretKey using the RSA Public Key:
    val rsaCipher = Cipher.getInstance("RSA")
    rsaCipher.init(Cipher.WRAP_MODE, rsaPublicKey)
    val encryptedAesSecretKey = rsaCipher.wrap(secretKey)

    // Send the encrypted AES SecretKey:
    DataOutputStream(s.getOutputStream()).writeInt(encryptedAesSecretKey.size)
    s.getOutputStream().write(encryptedAesSecretKey)
    s.getOutputStream().flush()

    return secretKey
}

/**
 * Returns 'text' as aes encrypted Base64 string.
 */
fun encryptText(text: String, secretKey: SecretKey,
                charset: Charset = Charset.forName("UTF-8")): String {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    val encryptedBytes = cipher.doFinal(text.toByteArray(charset))
    return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
}

/**
 * Returns 'text' ( = encrypted Base64 string) as decrypted string.
 */
fun decryptText(text: String, secretKey: SecretKey,
                charset: Charset = Charset.forName("UTF-8")): String {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    val encryptedBytes = Base64.decode(text, Base64.NO_WRAP)
    return cipher.doFinal(encryptedBytes).toString(charset)
}