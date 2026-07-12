package com.egabel.daddont.data.model

enum class Tier { LOW, MEDIUM, HIGH }

enum class Category { PURCHASE, IDEA, COMMUNICATION, COMMITMENT, OTHER }

/**
 * What kind of contract this impulse is:
 * DECISION — cool it down, then decide (did it / killed).
 * HOLD — resist until a hard stop, then it's allowed. RED until the end
 *        time, GREEN when open. Making it to the end is the win.
 */
enum class ImpulseKind { DECISION, HOLD }

/**
 * Terminal outcome on an impulse. Nothing archives without one.
 * BROKE = gave up before it finished — recorded, not hidden. (Slips that
 * DON'T end the impulse are BreachEvents, not verdicts.)
 * HELD = survived a HOLD to its end time.
 */
enum class Verdict { DID_IT, KILLED, BROKE, HELD }

/** What the user predicted at capture time they'd feel when it cooled. */
enum class Prediction { STILL_WANT, MOVED_ON }

enum class ImpulseState { PENDING, RED, YELLOW, GREEN, GRAY }
