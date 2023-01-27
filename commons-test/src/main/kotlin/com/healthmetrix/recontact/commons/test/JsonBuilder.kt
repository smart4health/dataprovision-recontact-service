package com.healthmetrix.recontact.commons.test

import org.json.JSONArray
import org.json.JSONObject

fun json(builder: JsonObjectBuilder.() -> Unit) = with(
    JsonObjectBuilder(),
) {
    builder()
    json
}

fun array(builder: JsonArrayBuilder.() -> Unit) = with(
    JsonArrayBuilder(),
) {
    builder()
    json
}

class JsonObjectBuilder {
    val json = JSONObject()

    infix fun <T> String.to(value: T) {
        json.put(this, value)
    }

    fun json(builder: JsonObjectBuilder.() -> Unit) =
        com.healthmetrix.recontact.commons.test.json(builder)
}

class JsonArrayBuilder {
    val json = JSONArray()

    fun json(builder: JsonObjectBuilder.() -> Unit) {
        json.put(com.healthmetrix.recontact.commons.test.json(builder))
    }

    operator fun Any.unaryPlus() {
        json.put(this)
    }
}
