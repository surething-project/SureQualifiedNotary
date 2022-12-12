package main.kotlin.notary.security

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object SecurityUtils {
    fun getPublicKey(fileName: String): PublicKey {
        val file =
            if (System.getenv("HEROKU_POSTGRESQL_COBALT_URL") != null)
                File("resources/$fileName")
            else
                File(this::class.java.classLoader.getResource(fileName)!!.file)

        return readPublicKey(file)
    }

    fun getKioskPrivateKey(): PrivateKey {
        val kioskFile = File(this::class.java.classLoader.getResource("kiosk_private_key.pem")!!.file)
        return readPrivateKey(kioskFile)
    }

    fun getProverPrivateKey(): PrivateKey {
        val proverFile = File(this::class.java.classLoader.getResource("prover_private_key.pem")!!.file)
        return readPrivateKey(proverFile)
    }

    private fun readPublicKey(file: File): PublicKey {
        val key = String(Files.readAllBytes(file.toPath()), Charset.defaultCharset())
        val publicKeyPEM = key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PUBLIC KEY-----", "")
        val encoded: ByteArray = Base64.getDecoder().decode(publicKeyPEM)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(encoded)
        return keyFactory.generatePublic(keySpec) as PublicKey
    }

    private fun readPrivateKey(file: File): PrivateKey {
        val key = String(Files.readAllBytes(file.toPath()), Charset.defaultCharset())
        val privateKeyPEM = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PRIVATE KEY-----", "")
        val encoded: ByteArray = Base64.getDecoder().decode(privateKeyPEM)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        return keyFactory.generatePrivate(keySpec) as PrivateKey
    }
}