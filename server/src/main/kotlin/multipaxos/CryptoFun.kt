package multipaxos

import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import java.security.spec.X509EncodedKeySpec
import java.io.IOException
import java.security.*
import java.util.*

class RSAFun {
    var privateKey: PrivateKey
    var publicKey: PublicKey
    var privateKeyString : String = ""
    var publicKeyString : String = ""

        // convert String publickey to Key object
        @Throws(GeneralSecurityException::class, IOException::class)
        fun loadPublicKey(stored: String): Key {
            val data: ByteArray = Base64.getDecoder().
            decode(stored.toByteArray())
            val spec = X509EncodedKeySpec(data)
            val fact = KeyFactory.getInstance("RSA")
            return fact.generatePublic(spec)
        }

        // Encrypt using publickey
        @Throws(Exception::class)
        fun encryptMessage(plainText: String, publickey: String): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKey(publickey))
            return Base64.getEncoder().encodeToString(cipher.doFinal
                (plainText.toByteArray()))
        }

        @Throws(Exception::class)
        fun signMessage(plainText: String, privatekey: String): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, loadPrivateKey(privatekey))
            return Base64.getEncoder().encodeToString(cipher.doFinal
                (plainText.toByteArray()))
        }

        // Decrypt using privatekey
        @Throws(Exception::class)
        fun decryptMessage(encryptedText: String?, privatekey: String):
                String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, loadPrivateKey(privatekey))
            return String(cipher.doFinal(Base64.getDecoder().
            decode(encryptedText)))
        }

        @Throws(Exception::class)
        fun unsignMessage(encryptedText: String?, publickey: String):
                String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, loadPublicKey(publickey))
            return String(cipher.doFinal(Base64.getDecoder().
            decode(encryptedText)))
        }

        // Convert String private key to privateKey object
        @Throws(GeneralSecurityException::class)
        fun loadPrivateKey(key64: String): PrivateKey {
            val clear: ByteArray = Base64.getDecoder().
            decode(key64.toByteArray())
            val keySpec = PKCS8EncodedKeySpec(clear)
            val fact = KeyFactory.getInstance("RSA")
            val priv = fact.generatePrivate(keySpec)
            Arrays.fill(clear, 0.toByte())
            return priv
        }


        /*@Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val secretText = "www.knowledgefactory.net"
            val keyPairGenerator = RSAFun()
            // Generate private and public key
            val privateKey: String = Base64.getEncoder().
            encodeToString(keyPairGenerator.privateKey.encoded)
            val publicKey: String = Base64.getEncoder().
            encodeToString(keyPairGenerator.publicKey.encoded)
            println("Private Key: $privateKey")
            println("Public Key: $publicKey")
            // Encrypt secret text using public key
            val encryptedValue = encryptMessage(secretText, publicKey)
            println("Encrypted Value: $encryptedValue")
            // Decrypt
            val decryptedText = decryptMessage(encryptedValue, privateKey)
            println("Decrypted output: $decryptedText")
        }*/

    init {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        val pair = keyGen.generateKeyPair()
        privateKey = pair.private
        publicKey = pair.public
    }
}