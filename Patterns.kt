package com.autoexpense.parser

/**
 * Every pattern the parser uses lives here, so supporting a new bank format means
 * editing this one file and adding a test case.
 */
object Patterns {
    private val I = setOf(RegexOption.IGNORE_CASE)

    /**
     * Indian commercial SMS headers: two letters, a dash, six characters, optional
     * TRAI suffix (VM-HDFCBK, AX-ICICIB-S). Some phones drop the prefix (HDFCBK).
     */
    val BANK_SENDER = Regex("""^([A-Z]{2}-)?[A-Z0-9]{6}(-[A-Z])?$""", I)

    /** Apps whose notifications we read. Messaging apps are included so SMS alerts
     *  are still captured when the SMS permission is not granted. */
    val FINANCE_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.dreamplug.androidapp",               // CRED
        "com.sbi.lotusintouch",                   // SBI YONO
        "com.csam.icici.bank.imobile",            // ICICI iMobile
        "com.snapwork.hdfc",                      // HDFC Bank
        "com.axis.mobile",                        // Axis Mobile
        "com.msf.kbank.mobile",                   // Kotak
        "com.google.android.apps.messaging",      // Google Messages
        "com.samsung.android.messaging",          // Samsung Messages
        "com.android.mms",                        // AOSP / Xiaomi messaging
    )

    val MESSAGING_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
    )

    /** Messages that mention money but are not a completed transaction. */
    val IGNORE = Regex(
        listOf(
            """\botp\b""", "one[- ]time password", "verification code",
            """\bis due\b""", """\bdue (on|by|date)\b""", "minimum (amount )?due", "total due",
            """\brequest(ed)?\b""", "collect request",
            """\bfailed\b""", """\bdeclined\b""", "unsuccessful", "could not be processed",
            """will be (debited|deducted|charged)""", "scheduled for", "autopay (is )?set",
            "pre-?approved", "loan offer", """\boffer\b""", "cashback (of )?up ?to", "get up ?to", "apply now",
            "statement (is|has been) (generated|sent)",
        ).joinToString("|"),
        I,
    )

    /** Rs 1,250.50 / INR 500 / ₹99 */
    val AMOUNT = Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", I)

    /** SBI-style "debited by 120.0" with no currency word. */
    val AMOUNT_FALLBACK = Regex("""(?:debited|credited|withdrawn)\s+(?:by|for|with)\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", I)

    /** ICICI-style "...; AMAZON PAY credited." where the payee comes before "credited". */
    val MERCHANT_BEFORE_CREDITED = Regex("""[;.]\s*([A-Za-z][A-Za-z0-9 .&'@_\-]{1,40}?)\s+credited\b""", I)

    /** Words just before an amount that mean it is a balance or limit, not the spend. */
    val BALANCE_HINT = Regex("""(avl\.?|avail(able)?|bal(ance)?\.?|limit|o/s|outstanding)[\s:a-z/.]*$""", I)

    val DEBIT_WORDS = Regex(
        """\b(debited|debit|spent|paid|sent|withdrawn|withdrawal|purchase|deducted|txn of|payment of|dr\.?)\b""",
        I,
    )
    val CREDIT_WORDS = Regex(
        """\b(credited|credit|received|deposited|refund(ed)?|reversed|cashback|cr\.?)\b""",
        I,
    )

    val ACCOUNT = Regex(
        """(?:a/c|acct|account|card|\bac)(?:\s*no\.?)?(?:\s*ending(?:\s*(?:with|in))?)?[\s:.\-]*[x*]*\s*(\d{3,6})\b""",
        I,
    )

    val REFERENCE = Regex(
        """(?:upi\s*(?:ref|txn|transaction)?\.?\s*(?:no|id|#)?|ref(?:erence)?\.?\s*(?:no|id|#)?|rrn|utr|txn\s*id|transaction\s*id)[\s.:#\-]*([0-9]{8,})""",
        I,
    )

    /** "UPI/P2M/431298765412/SWIGGY" style info strings. */
    val UPI_INFO = Regex("""UPI/(?:P2[AM]|DR|CR)?/?\d{6,}/([A-Za-z0-9 .&'*-]{2,40})""", I)

    /** Merchant after a preposition, stopping at the next keyword or punctuation. */
    val MERCHANT = Regex(
        """\b(to|at|towards|from|by)\s+(?:vpa\s+|merchant\s+)?([A-Za-z0-9@._&'*\- ]{2,40}?)(?=\s+(?:on|via|ref|refno|upi|using|from|for|avl|avl\.|bal|dated|with|in|is|has|was|thru|through)\b|[.,;(]|\s*$)""",
        I,
    )

    /** Candidate merchants that are really the user's own account or filler words. */
    val NOT_A_MERCHANT = Regex(
        """^(your|ur|a/c|ac|acct|account|card|bank|you|self|the|beneficiary|mobile|xx|neft|imps|rtgs|upi)\b|^\d|^[x*]+\d""",
        I,
    )

    val UPI_HINT = Regex("""\bupi\b|\bvpa\b|@[a-z]{2,}|\bgpay\b|google pay|phonepe|paytm|bhim""", I)
    val CARD_HINT = Regex("""\bcard\b|\bpos\b""", I)
    val ATM_HINT = Regex("""\batm\b|withdrawn|cash withdrawal""", I)
    val NETBANKING_HINT = Regex("""\bneft\b|\bimps\b|\brtgs\b|net ?banking""", I)
}
