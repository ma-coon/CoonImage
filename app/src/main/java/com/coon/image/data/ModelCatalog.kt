package com.coon.image.data

/**
 * APP 内可选的大模型。用户在下拉框中选择其一，决定本次调用 DashScope 上的哪个能力。
 */
data class AiModel(
    val id: String,
    val displayName: String,
    val endpointKind: EndpointKind,
    val description: String
)

enum class EndpointKind { IMAGE_EDIT, TEXT_TO_IMAGE, QWEN_ROUTE }

object ModelCatalog {
    val models = listOf(
        AiModel(
            "wanx-x-painting",
            "通义万相·图像编辑",
            EndpointKind.IMAGE_EDIT,
            "基于原图+关键字指令编辑（换天空/换装/抠图/去人等）"
        ),
        AiModel(
            "wanx2.1-t2i-plus",
            "通义万相·文生图",
            EndpointKind.TEXT_TO_IMAGE,
            "忽略原图，根据关键字重新生成一张图"
        ),
        AiModel(
            "qwen-route",
            "通义千问·智能路由",
            EndpointKind.QWEN_ROUTE,
            "先用通义千问理解关键字，再调用图像编辑"
        )
    )

    val default: AiModel get() = models.first()
}
