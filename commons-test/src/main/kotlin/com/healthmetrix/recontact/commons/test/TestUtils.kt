package com.healthmetrix.recontact.commons.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

class TestUtils {
    inline fun <reified T : Any> fromJson(file: String): T {
        val kMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return kMapper.readValue(javaClass.classLoader.getResource(file)!!.readText(), T::class.java)
    }
}
