package com.vellum.ledger.ui.provider

interface StringProvider {
    fun get(id: String): String
    fun formatLastSync(lastSyncAtMillis: Long?): String
    fun formatMoney(amountCents: Long, currency: String): String
}

class CommonStringProvider : StringProvider {
    override fun get(id: String): String {
        // In a real app, this would use multiplatform resource logic
        return id 
    }

    override fun formatLastSync(lastSyncAtMillis: Long?): String {
        if (lastSyncAtMillis == null) return "Never"
        val nowMillis = com.vellum.ledger.data.currentTimeMillis()
        val diff = (nowMillis - lastSyncAtMillis).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
            else -> "${diff / 86_400_000L}d ago"
        }
    }

    override fun formatMoney(amountCents: Long, currency: String): String {
        return com.vellum.ledger.ui.util.formatMoney(amountCents, currency)
    }
}
