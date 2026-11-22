package com.autoexpense.parser

enum class TxnType { DEBIT, CREDIT }

enum class PaymentMode { UPI, CARD, NETBANKING, ATM, OTHER }

enum class CaptureSource { SMS, NOTIFICATION, MANUAL }

/**
 * One transaction extracted from a bank SMS or a finance-app notification.
 * Amounts are in paise (₹1 = 100 paise) so sums never have floating-point errors.
 */
data class ParsedTransaction(
    val amountPaise: Long,
    val type: TxnType,
    val merchant: String?,
    val account: String?,
    val mode: PaymentMode,
    val reference: String?,
    val balancePaise: Long?,
    val timestamp: Long,
    val source: CaptureSource,
    val sender: String,
    val rawText: String,
) {
    /** Stable id for this exact message, used to skip re-imports of the same SMS. */
    val fingerprint: String
        get() = "${source.name}|$sender|${rawText.trim().hashCode()}|${timestamp / 60_000}"
}
