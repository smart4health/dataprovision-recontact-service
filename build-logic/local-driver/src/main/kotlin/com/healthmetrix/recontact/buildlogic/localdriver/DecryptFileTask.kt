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

open class DecryptFileTask : DefaultTask() {

    @Input
    @set:Option(option = "inputFilePath", description = "input file location for ciphertext")
    var inputFilePath: String = "build-logic/local-driver/src/main/resources/request.json.enc"

    @Input
    @set:Option(option = "outputFileName", description = "output file name for plaintext result")
    var outputFileName: String = "request.json"

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
    fun decryptFile() {
        val inputFileBytes = try {
            project.file(inputFilePath).readBytes()
        } catch (ex: Exception) {
            throw GradleException("Failed to parse input file at $inputFilePath", ex)
        }

        val key = Key.fromHexString(key)
        val nonce = inputFileBytes.sliceArray(0 until nonceSize)
        val ciphertext = inputFileBytes.sliceArray(nonceSize until inputFileBytes.size)
        val plaintext = lazySodium.decrypt(lazySodium.toHexStr(ciphertext), null, nonce, key, encryptionMethod)

        println("plaintext:\n$plaintext")

        try {
            buildFile(outputFileName).writeText(plaintext)
        } catch (ex: Exception) {
            throw GradleException("Failed to write output file to ${project.buildDir}/$outputFileName", ex)
        }
        println("plaintext written to ${project.buildDir}/$outputFileName")
    }

    private fun buildFile(name: String) = File(project.buildDir, name).apply {
        parentFile.mkdirs()
    }
}
