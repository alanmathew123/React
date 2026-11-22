package com.autoexpense.parser

/**
 * Turns the text of one bank SMS or finance-app notification into a [ParsedTransaction],
 * or returns null when the message is not a completed money movement.
 */
object TransactionParser {

    /** True when an SMS sender id looks like a bank/business header rather than a person. */
    fun isLikelyBankSender(sender: String): Boolean =
        Patterns.BANK_SENDER.matches(sender.trim())

    /** True when notifications from this package should be read at all. */
    fun isFinancePackage(packageName: String): Boolean =
        packageName in Patterns.FINANCE_PACKAGES

    fun parse(
        text: String,
        sender: String,
        timestamp: Long,
        source: CaptureSource,
    ): ParsedTransaction? {
        val body = text.replace(Regex("""\s+"""), " ").trim()
        if (body.isEmpty()) return null
        if (Patterns.IGNORE.containsMatchIn(body)) return null

        val (amount, balance) = findAmounts(body) ?: return null
        if (amount <= 0) return null
        val type = findType(body) ?: return null

        return ParsedTransaction(
            amountPaise = amount,
            type = type,
            merchant = findMerchant(body, type),
            account = Patterns.ACCOUNT.find(body)?.groupValues?.get(1),
            mode = findMode(body),
            reference = Patterns.REFERENCE.find(body)?.groupValues?.get(1),
            balancePaise = balance,
            timestamp = timestamp,
            source = source,
            sender = sender,
            rawText = text,
        )
    }

    /** First amount that is not a balance/limit, plus the balance if one is stated. */
    private fun findAmounts(body: String): Pair<Long, Long?>? {
        var amount: Long? = null
        var balance: Long? = null
        for (m in Patterns.AMOUNT.findAll(body)) {
            val value = toPaise(m.groupValues[1]) ?: continue
            val before = body.substring(maxOf(0, m.range.first - 22), m.range.first)
            if (Patterns.BALANCE_HINT.containsMatchIn(before)) {
                if (balance == null) balance = value
            } else if (amount == null) {
                amount = value
            }
        }
        if (amount == null) {
            amount = Patterns.AMOUNT_FALLBACK.find(body)?.groupValues?.get(1)?.let(::toPaise)
        }
        return amount?.let { it to balance }
    }

    internal fun toPaise(raw: String): Long? {
        val clean = raw.replace(",", "").trimEnd('.')
        val parts = clean.split(".")
        val rupees = parts[0].toLongOrNull() ?: return null
        val paise = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0
        return rupees * 100 + paise
    }

    /** Whichever of debit/credit wording appears first decides the direction. */
    private fun findType(body: String): TxnType? {
        // "Credit Card" / "Debit Card" name a product, not a direction.
        val neutral = body.replace(Regex("""(credit|debit)\s+card""", RegexOption.IGNORE_CASE), "card")
            .replace(Regex("""\b(has been|was|is) used\b""", RegexOption.IGNORE_CASE), "spent")
        val debit = Patterns.DEBIT_WORDS.find(neutral)?.range?.first
        val credit = Patterns.CREDIT_WORDS.find(neutral)?.range?.first
        return when {
            debit == null && credit == null -> null
            credit == null -> TxnType.DEBIT
            debit == null -> TxnType.CREDIT
            debit < credit -> TxnType.DEBIT
            else -> TxnType.CREDIT
        }
    }

    private fun findMode(body: String): PaymentMode = when {
        Patterns.UPI_HINT.containsMatchIn(body) -> PaymentMode.UPI
        Patterns.ATM_HINT.containsMatchIn(body) -> PaymentMode.ATM
        Patterns.CARD_HINT.containsMatchIn(body) -> PaymentMode.CARD
        Patterns.NETBANKING_HINT.containsMatchIn(body) -> PaymentMode.NETBANKING
        else -> PaymentMode.OTHER
    }

    private fun findMerchant(body: String, type: TxnType): String? {
        Patterns.UPI_INFO.find(body)?.let { m ->
            cleanMerchant(m.groupValues[1])?.let { return it }
        }
        if (type == TxnType.DEBIT) {
            Patterns.MERCHANT_BEFORE_CREDITED.find(body)?.let { m ->
                val c = m.groupValues[1].trim()
                if (!Patterns.NOT_A_MERCHANT.containsMatchIn(c)) cleanMerchant(c)?.let { return it }
            }
        }
        // For a debit the payee follows to/at/towards; for a credit the payer follows from/by.
        val wanted = if (type == TxnType.DEBIT) setOf("to", "at", "towards") else setOf("from", "by")
        for (m in Patterns.MERCHANT.findAll(body)) {
            if (m.groupValues[1].lowercase() !in wanted) continue
            val candidate = m.groupValues[2].trim()
            if (Patterns.NOT_A_MERCHANT.containsMatchIn(candidate)) continue
            cleanMerchant(candidate)?.let { return it }
        }
        return null
    }

    internal fun cleanMerchant(raw: String): String? {
        var s = raw.trim().trim('.', '-', '*', ' ')
        if (s.contains('@')) {
            // A VPA: keep the handle for phone-number VPAs, the readable name otherwise.
            val handle = s.substringBefore('@')
            s = if (handle.all { it.isDigit() }) s else handle
        }
        s = s.replace(Regex("""[._]+"""), " ").replace(Regex("""\s+"""), " ").trim()
        s = s.split(' ').filterNot { it.equals("upi", ignoreCase = true) }.joinToString(" ")
        if (s.length < 2 || s.all { it.isDigit() || it == ' ' } && !raw.contains('@')) return null
        return s.split(' ').joinToString(" ") { word ->
            if (word.contains('@')) word
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
