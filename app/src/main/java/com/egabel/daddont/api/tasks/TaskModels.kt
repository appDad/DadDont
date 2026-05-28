package com.egabel.daddont.api.tasks

import kotlinx.serialization.Serializable

@Serializable
data class TaskCreateRequest(
    val title: String
)

@Serializable
data class TaskCreateResult(
    val id: String? = null,
    val title: String? = null,
    val selfLink: String? = null
)
