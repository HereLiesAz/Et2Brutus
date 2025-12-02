package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import kotlinx.coroutines.Job

data class CyclerTransientState(
    val displayedOption: String,
    val job: Job? = null
)
