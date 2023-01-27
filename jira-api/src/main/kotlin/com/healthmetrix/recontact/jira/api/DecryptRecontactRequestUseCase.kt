package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.utils.Key
import com.healthmetrix.recontact.commons.RecontactRequest
import com.healthmetrix.recontact.commons.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Decryption of the file generated by the research platform.
 * The encryption method is authenticated encryption with additional data using the cipher CHACHA20_POLY1305_IETF.
 * Input byte array: 12 bytes of nonce followed by the ciphertext having the 16 bytes MAC at its end. No add. data (null)
 * See com.healthmetrix.recontact.buildlogic.conventions.EncryptFileTask for a sample encryption.
 */
@Component
@Profile("jira")
class DecryptRecontactRequestUseCase(
    private val lazySodium: LazySodiumJava,
    private val sharedCohortKey: Key,
    private val objectMapper: ObjectMapper,
) {

    operator fun invoke(cipherBytes: ByteArray): Result<RecontactRequest, DecryptionError> = binding {
        val nonceSize: Int = AEAD.CHACHA20POLY1305_IETF_NPUBBYTES
        val nonce = cipherBytes.sliceArray(0 until nonceSize)
        val ciphertext = cipherBytes.sliceArray(nonceSize until cipherBytes.size)

        val decryptedJson = lazySodium
            .runCatching {
                decrypt(
                    lazySodium.toHexStr(ciphertext),
                    null,
                    nonce,
                    sharedCohortKey,
                    AEAD.Method.CHACHA20_POLY1305_IETF,
                )
            }
            .mapError { DecryptionError.LazySodium }
            .bind()

        objectMapper
            .runCatching { readValue(decryptedJson, RecontactRequest::class.java) }
            .onFailure { logger.error(it.message) }
            .mapError { DecryptionError.ObjectMapper }
            .bind()
    }
}

sealed class DecryptionError {
    object LazySodium : DecryptionError()
    object ObjectMapper : DecryptionError()
}
