package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import com.healthmetrix.recontact.commons.RecontactRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier

class DecryptRecontactRequestUseCaseTest {

    private val lazySodium: LazySodiumJava = LazySodiumJava(SodiumJava())

    @Qualifier("sharedCohortKey")
    private val sharedCohortKey: Key =
        Key.fromHexString("0000000000000000000000000000000000000000000000000000000000000000")
    private val objectMapper: ObjectMapper = mockk()
    private val underTest = DecryptRecontactRequestUseCase(lazySodium, sharedCohortKey, objectMapper)

    @Test
    fun `decrypt the file`() {
        val cipherText = javaClass.classLoader.getResource("request.json.enc")!!.readBytes()
        val expected = RecontactRequest(
            cohort = RecontactRequest.Cohort(
                name = "some-rp-specific-value",
                datasetVersion = "1.0",
                distribution = RecontactRequest.Cohort.Distribution(age = listOf(), gender = listOf()),
                pseudonymIds = listOf(),
                queryParams = listOf(),
            ),
        )
        every { objectMapper.readValue(ofType(String::class), eq(RecontactRequest::class.java)) } returns expected

        val result = underTest.invoke(cipherText)

        assertThat(result.get()).isEqualTo(expected)
    }

    @Test
    fun `decrypt the file with encryption errors`() {
        val cipherText = javaClass.classLoader.getResource("request.json.enc")!!.readBytes()
        val expected = RecontactRequest(
            cohort = RecontactRequest.Cohort(
                name = "some-rp-specific-value",
                datasetVersion = "1.0",
                distribution = RecontactRequest.Cohort.Distribution(age = listOf(), gender = listOf()),
                pseudonymIds = listOf(),
                queryParams = listOf(),
            ),
        )
        every { objectMapper.readValue(ofType(String::class), eq(RecontactRequest::class.java)) } returns expected

        val result = DecryptRecontactRequestUseCase(
            lazySodium,
            Key.fromHexString("1111111111111111111111111111111111111111111111111111111111111111"),
            objectMapper,
        ).invoke(cipherText)

        assertThat(result.getError()).isEqualTo(DecryptionError.LazySodium)
    }

    @Test
    fun `decrypt the file with mapping errors`() {
        val cipherText = javaClass.classLoader.getResource("request.json.enc")!!.readBytes()
        every { objectMapper.readValue(ofType(String::class), eq(RecontactRequest::class.java)) } throws Exception()
        val result = underTest.invoke(cipherText)
        assertThat(result.getError()).isEqualTo(DecryptionError.ObjectMapper)
    }
}
