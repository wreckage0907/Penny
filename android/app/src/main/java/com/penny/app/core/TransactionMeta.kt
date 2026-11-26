package com.penny.app.core

/**
 * Per-transaction display metadata (artboard frame 07 rows: "18 Nov ·
 * Groceries · UPI"). The v1 wire model carries name/amount/category only —
 * the backend has no day/subcategory/payment columns — so this is a
 * separate, OPTIONAL seam: the day Quick Log collects is kept here, and
 * entries without metadata render their month/category instead.
 *
 * **Needs maintainer eyes:** a real implementation needs either backend
 * columns or a local ledger; until then only the in-memory fake implements
 * it (screens degrade gracefully when it returns [TxDetail.ABSENT]).
 */
data class TxDetail(
    val day: Int? = null,
    val subcategory: String? = null,
    val payment: String = "UPI",
) {
    companion object {
        val ABSENT = TxDetail()
    }
}

interface TransactionMeta {
    suspend fun detail(user: String, year: Int, month: String, category: String, name: String): TxDetail
    suspend fun put(user: String, year: Int, month: String, category: String, name: String, detail: TxDetail)
}

/** In-memory implementation; deleting an expense leaves a harmless orphan. */
class InMemoryTransactionMeta : TransactionMeta {
    private val details = mutableMapOf<String, TxDetail>()

    override suspend fun detail(user: String, year: Int, month: String, category: String, name: String): TxDetail =
        details["$user|$year|$month|$category|$name"] ?: TxDetail.ABSENT

    override suspend fun put(user: String, year: Int, month: String, category: String, name: String, detail: TxDetail) {
        details["$user|$year|$month|$category|$name"] = detail
    }
}
