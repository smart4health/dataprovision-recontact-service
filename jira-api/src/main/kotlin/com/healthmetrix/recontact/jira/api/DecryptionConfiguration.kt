package com.healthmetrix.recontact.jira.api

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import com.healthmetrix.recontact.commons.Secrets
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("jira")
class DecryptionConfiguration {

    @Bean
    fun provideLazySodium(): LazySodiumJava = LazySodiumJava(SodiumJava())

    @Bean
    fun provideSharedCohortKey(
        secrets: Secrets,
        @Value("\${secrets.shared-cohort-key}")
        decryptionKeyLocation: String,
    ): Key = Key.fromHexString(secrets[decryptionKeyLocation])
}
