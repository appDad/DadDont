package com.egabel.daddont.data.model

enum class Tier { LOW, MEDIUM, HIGH }

enum class Category { PURCHASE, IDEA, COMMUNICATION, COMMITMENT, OTHER }

/** Terminal decision on a cooled impulse. Nothing archives without one. */
enum class Verdict { DID_IT, KILLED }

/** What the user predicted at capture time they'd feel when it cooled. */
enum class Prediction { STILL_WANT, MOVED_ON }

enum class ImpulseState { PENDING, RED, YELLOW, GREEN, GRAY }
