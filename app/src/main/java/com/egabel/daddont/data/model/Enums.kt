package com.egabel.daddont.data.model

enum class Tier { LOW, MEDIUM, HIGH }

enum class Category { PURCHASE, IDEA, COMMUNICATION, COMMITMENT, OTHER }

enum class DismissalType {
    DONE,
    NO_LONGER_WANT,
    PARTNER_APPROVED,
    PARTNER_DECLINED,
    DECIDED_NOT_TO_ASK
}

enum class ImpulseState { RED, YELLOW, GREEN, GRAY }
