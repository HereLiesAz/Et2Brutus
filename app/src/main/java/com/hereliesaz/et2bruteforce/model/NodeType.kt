package com.hereliesaz.et2bruteforce.model

import kotlinx.serialization.Serializable

/**
 * Defines the type of UI element being targeted for identification or interaction.
 */
@Serializable
enum class NodeType {
    INPUT,
    SUBMIT,
    POPUP
}