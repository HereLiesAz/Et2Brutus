package com.hereliesaz.et2bruteforce.model

import android.graphics.Point
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

@Serializable
private data class SerializablePoint(val x: Int, val y: Int)

object PointSerializer : KSerializer<Point> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Point") {
        element<Int>("x")
        element<Int>("y")
    }

    override fun serialize(encoder: Encoder, value: Point) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
        }
    }

    override fun deserialize(decoder: Decoder): Point {
        return decoder.decodeStructure(descriptor) {
            var x: Int? = null
            var y: Int? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeIntElement(descriptor, 0)
                    1 -> y = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            require(x != null && y != null) { "Missing x or y coordinate" }
            Point(x, y)
        }
    }
}
