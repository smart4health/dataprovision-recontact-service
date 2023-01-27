package com.healthmetrix.recontact.buildlogic.localdriver

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.utils.Key
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class EncryptFileTask : DefaultTask() {

    @Input
    @set:Option(option = "inputFilePath", description = "input file location to encrypt")
    var inputFilePath: String = "build-logic/local-driver/src/main/resources/request.json"

    @Input
    @set:Option(option = "outputFileName", description = "output file name for ciphertext")
    var outputFileName: String = "request.json.enc"

    @Input
    @set:Option(
        option = "key",
        description = "32 byte symmetric key as hex string for encryption",
    )
    var key: String = "0000000000000000000000000000000000000000000000000000000000000000"

    private val encryptionMethod: AEAD.Method = AEAD.Method.CHACHA20_POLY1305_IETF
    private val nonceSize: Int = AEAD.CHACHA20POLY1305_IETF_NPUBBYTES
    private val lazySodium = LazySodiumJava(SodiumJava())

    @TaskAction
    fun encryptFile() {
        val inputFileText = try {
            project.file(inputFilePath).readText()
        } catch (ex: Exception) {
            throw GradleException("Failed to parse input file at $inputFilePath", ex)
        }

        val nonce = lazySodium.nonce(nonceSize)
        val key = Key.fromHexString(key)
        val cipherText =
            lazySodium.encrypt(inputFileText, null, nonce, key, encryptionMethod)
        val cipherIncludingNonce = lazySodium.toHexStr(nonce) + cipherText

        println("ciphertext [hex]:\n$cipherIncludingNonce")
        try {
            buildFile(outputFileName).writeBytes(lazySodium.toBinary(cipherIncludingNonce))
        } catch (ex: Exception) {
            throw GradleException("Failed to write encrypted file to ${project.buildDir}/$outputFileName", ex)
        }
        println("ciphertext [binary] written to ${project.buildDir}/$outputFileName")
    }

    private fun buildFile(name: String) = File(project.buildDir, name).apply {
        parentFile.mkdirs()
    }
}
