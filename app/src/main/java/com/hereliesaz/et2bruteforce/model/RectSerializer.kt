package com.hereliesaz.et2bruteforce.model

import android.graphics.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

object RectSerializer : KSerializer<Rect> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rect") {
        element<Int>("left")
        element<Int>("top")
        element<Int>("right")
        element<Int>("bottom")
    }

    override fun serialize(encoder: Encoder, value: Rect) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.left)
            encodeIntElement(descriptor, 1, value.top)
            encodeIntElement(descriptor, 2, value.right)
            encodeIntElement(descriptor, 3, value.bottom)
        }
    }

    override fun deserialize(decoder: Decoder): Rect {
        return decoder.decodeStructure(descriptor) {
            var left = 0
            var top = 0
            var right = 0
            var bottom = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> left = decodeIntElement(descriptor, 0)
                    1 -> top = decodeIntElement(descriptor, 1)
                    2 -> right = decodeIntElement(descriptor, 2)
                    3 -> bottom = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Rect(left, top, right, bottom)
        }
    }
}
