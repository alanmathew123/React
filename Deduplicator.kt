package com.autoexpense.parser

/** The fields of an already-saved transaction that duplicate detection needs. */
data class ExistingTxn(
    val amountPaise: Long,
    val type: TxnType,
    val timestamp: Long,
    val source: CaptureSource,
    val sender: String,
    val reference: String?,
    val rawText: String,
)

/**
 * One payment usually produces several alerts: a bank SMS, a GPay notification, and the
 * messaging app's notification for that same SMS. This decides whether a new alert is
 * one we already saved.
 */
object Deduplicator {
    const val WINDOW_MS = 10 * 60 * 1000L

    fun isDuplicate(new: ParsedTransaction, recent: List<ExistingTxn>): Boolean = recent.any { old ->
        when {
            old.amountPaise != new.amountPaise || old.type != new.type -> false
            // Same UPI/bank reference = same payment, whatever the time gap.
            old.reference != null && new.reference != null -> old.reference == new.reference
            kotlin.math.abs(old.timestamp - new.timestamp) > WINDOW_MS -> false
            // Exact same text again (re-posted notification, inbox re-scan).
            normalize(old.rawText) == normalize(new.rawText) -> true
            // Same amount in the window from a *different* channel (SMS vs app notification,
            // or bank SMS vs GPay notification) = the same payment. Two alerts from the same
            // channel are kept: you might really pay ₹20 twice in ten minutes.
            else -> old.source != new.source || !old.sender.equals(new.sender, ignoreCase = true)
        }
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("""\s+"""), " ").trim()
}
