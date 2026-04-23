package io.github.zzzyyylllty.attribute.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

val jsonUtils: Gson by lazy {
    val builder = GsonBuilder()
        .setVersion(1.0)
        .disableHtmlEscaping()
        .disableInnerClassSerialization()
        // .setPrettyPrinting()
        .excludeFieldsWithModifiers()
        .serializeNulls()
        .setLenient()

    val tempGson = builder.create()

    builder.create()
}

//
//val mcClassLoader: ClassLoader by lazy {
//    Class.forName("net.minecraft.server.MinecraftServer").classLoader
//}
//
//fun parseStringToMinecraftJsonElement(jsonString: String): JsonElement {
//    val mcClassLoader = mcClassLoader
//    val jsonParserClass = Class.forName("com.google.gson.JsonParser", true, mcClassLoader)
//    val parseStringMethod = jsonParserClass.getMethod("parseString", String::class.java)
//    return parseStringMethod.invoke(null, jsonString) as JsonElement
//}
//
//fun parseJsonStringWithMCGson(json: String): JsonElement {
//    //val mcClassLoader = mcClassLoader
//    //val jsonParserClass = Class.forName("com.google.gson.JsonParser", true, mcClassLoader)
//    //val parseStringMethod = jsonParserClass.getMethod("parseString", String::class.java)
//    return (jsonUtils.toJsonTree(json) as com.google.gson.JsonElement)
//
//}
//
//
//fun unwrapJson(value: Any?): Any? {
//    when (value) {
//        is JsonObject -> {
//            val map = mutableMapOf<String, Any?>()
//            value.entrySet().forEach { (key, element) ->
//                map[key] = unwrapJson(element)
//            }
//            return map
//        }
//        is JsonArray -> {
//            val list = mutableListOf<Any?>()
//            value.forEach { element ->
//                list.add(unwrapJson(element))
//            }
//            return list
//        }
//        is JsonPrimitive -> {
//            when {
//                value.isBoolean -> return value.asBoolean
//                value.isNumber -> {
//                    val numberString = value.asString
//                    try {
//                        val asDouble = numberString.toDouble()
//
//                        // Special handling for Byte (only 0 and 1)
//                        if (asDouble == 0.0 || asDouble == 1.0) {
//                            return asDouble.toInt().toByte()
//                        }
//                        // Try to convert to Long
//                        try {
//                            if (asDouble == round(asDouble)) { // Ensure Double value is equal to Long value (no decimal part)
//                                val asLong = asDouble.toLong()
//
//                                // Try to convert to Int
//                                if (asLong in Int.MIN_VALUE..Int.MAX_VALUE) {
//                                    val asInt = asLong.toInt()
//
//                                    // Try to convert to Short
//                                    if (asInt in Short.MIN_VALUE..Short.MAX_VALUE) {
//                                        val asShort = asInt.toShort()
//                                        return asShort
//                                    }
//
//                                    return asInt
//                                } else {
//                                    return asLong
//                                }
//                            }
//                        } catch (e: Exception) {
//                            // Ignore, it's not a Long
//                        }
//
//
//                        // Try to convert to Float (check for precision loss)
//                        try {
//                            val asFloat = value.asFloat
//                            devLog("float: $asFloat, double: $asDouble")
//                            if (asFloat.toString() == asDouble.toString()) {
//                                return asFloat
//                            }
//                        } catch (_: Exception) {
//                        }
//
//                        return asDouble // If none of the above conversions are satisfied, return Double
//                    } catch (e: NumberFormatException) {
//                        return numberString // If it cannot be parsed into Double, return String
//                    }
//                }
//                value.isString -> return value.asString
//                else -> return value.toString() // Handle other primitive types if needed
//            }
//        }
//        is JsonElement -> return value.toString() // Fallback for other JsonElement types
//        null -> return null
//        else -> return value // Return the original value if it's not a Json type
//    }
//}