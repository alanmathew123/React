package com.autoexpense.parser

/** A keyword the user taught the app, e.g. "ramesh kirana" -> "Groceries". */
data class CategoryRule(val keyword: String, val category: String)

object Categorizer {
    const val UNCATEGORISED = "Uncategorised"
    const val INCOME = "Income"

    val ALL_CATEGORIES = listOf(
        "Food", "Groceries", "Shopping", "Travel", "Fuel", "Bills & Utilities", "Rent",
        "Entertainment", "Health", "Education", "Investments", "EMI & Loans", "Transfers",
        "Cash", INCOME, UNCATEGORISED,
    )

    private val BUILT_IN: Map<String, List<String>> = mapOf(
        "Food" to listOf("swiggy", "zomato", "dominos", "domino's", "mcdonald", "kfc", "pizza", "starbucks",
            "cafe", "restaurant", "eatsure", "burger", "chaayos", "haldiram"),
        "Groceries" to listOf("bigbasket", "blinkit", "zepto", "instamart", "dmart", "jiomart", "grofers",
            "more retail", "reliance fresh", "kirana", "supermarket", "nature's basket"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tata cliq",
            "decathlon", "ikea", "croma", "reliance digital"),
        "Travel" to listOf("uber", "ola", "rapido", "irctc", "makemytrip", "goibibo", "redbus", "indigo",
            "air india", "akasa", "metro", "cleartrip", "yatra", "ixigo", "fastag"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "bpcl", "indian oil", "iocl", "shell", "nayara"),
        "Bills & Utilities" to listOf("electricity", "bescom", "tata power", "adani electricity", "msedcl",
            "airtel", "jio", "vodafone", "vi ", "bsnl", "act fibernet", "broadband", "gas", "water bill",
            "recharge", "dth", "tata play"),
        "Entertainment" to listOf("netflix", "hotstar", "prime video", "spotify", "bookmyshow", "pvr",
            "inox", "youtube", "sonyliv", "zee5", "apple.com", "google play"),
        "Health" to listOf("pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "hospital",
            "clinic", "diagnostic", "practo", "cult.fit", "cultfit"),
        "Education" to listOf("udemy", "coursera", "byju", "unacademy", "school", "college", "university"),
        "Investments" to listOf("zerodha", "groww", "upstox", "mutual fund", "sip", "nps", "ppf",
            "indian clearing corp", "kuvera", "smallcase"),
        "EMI & Loans" to listOf("emi", "loan", "bajaj finance", "home credit"),
        "Rent" to listOf("rent", "nobroker", "housing.com"),
        "Cash" to listOf("atm", "cash withdrawal"),
    )

    /**
     * User rules win over built-in keywords. Credits default to Income; ATM withdrawals to Cash.
     */
    fun categorize(txn: ParsedTransaction, userRules: List<CategoryRule> = emptyList()): String {
        val haystack = listOfNotNull(txn.merchant, txn.rawText).joinToString(" ").lowercase()
        val merchant = txn.merchant?.lowercase().orEmpty()

        userRules.firstOrNull { r -> r.keyword.isNotBlank() && haystack.contains(r.keyword.lowercase()) }
            ?.let { return it.category }

        if (txn.type == TxnType.CREDIT) return INCOME
        if (txn.mode == PaymentMode.ATM) return "Cash"

        // Match the merchant name first (more precise), then the full message.
        for (text in listOf(merchant, haystack)) {
            if (text.isBlank()) continue
            for ((category, words) in BUILT_IN) {
                if (words.any { w -> containsWord(text, w) }) return category
            }
        }
        return UNCATEGORISED
    }

    private fun containsWord(text: String, word: String): Boolean {
        val w = word.trim()
        // Short keywords (ola, jio, sip) must be whole words so "Motorola" is not Travel.
        return if (w.length <= 4) Regex("""(^|[^a-z0-9])${Regex.escape(w)}([^a-z0-9]|$)""").containsMatchIn(text)
        else text.contains(w)
    }
}
