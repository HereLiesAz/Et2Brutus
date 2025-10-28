package com.hereliesaz.et2bruteforce.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val name: String,
    val buttonConfigs: Map<NodeType, ButtonConfig>
)
