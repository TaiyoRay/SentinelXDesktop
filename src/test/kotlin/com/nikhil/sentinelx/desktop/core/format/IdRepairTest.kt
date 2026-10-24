package com.nikhil.sentinelx.desktop.core.format

import com.nikhil.sentinelx.desktop.core.format.VaultMerge.DuplicatePolicy
import com.nikhil.sentinelx.desktop.core.format.VaultMerge.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins [withUniqueIds]. The bug it fixes (24 Oct 2026): a phone archive merged into the
 * desktop kept the phone's bill ids next to the desktop's own, so two bills were both
 * `id = 3`. The Bills list (a LazyColumn keyed on id) crashed with `Key "3" was already
 * used` the moment a filter chip brought the twins near each other, and every by-id
 * edit — tick paid, edit, delete — hit both rows.
 */
class IdRepairTest {

    private fun bill(id: Long, provider: String) = BillEntity(
        id = id, billType = Bills.TYPE_OTHER, provider = provider,
        amount = 100.0, dueDate = 1_760_000_000_000
    )

    private fun ids(b: MasterBackup) = b.bills.map { it.id }

    @Test
    fun `merging a phone archive no longer leaves two bills sharing an id`() {
        val desktop = MasterBackup(bills = listOf(bill(1, "Airtel"), bill(2, "Jio"), bill(3, "BSNL")))
        val phone = MasterBackup(bills = listOf(bill(3, "AVVNL Meena Ka Mohala"), bill(4, "JVVNL Jaipur")))

        val merged = VaultMerge.apply(desktop, phone, Mode.MERGE, DuplicatePolicy.KEEP_BOTH).vault
        // The engine itself is mirrored with the phone and keeps incoming ids as-is.
        assertTrue(ids(merged).groupBy { it }.any { it.value.size > 1 }, "precondition: merge collides")

        val fixed = merged.withUniqueIds()
        assertEquals(ids(fixed).size, ids(fixed).toSet().size, "every bill id is unique")
        assertEquals(listOf("Airtel", "Jio", "BSNL", "AVVNL Meena Ka Mohala", "JVVNL Jaipur"),
            fixed.bills.map { it.provider }, "order and content are kept")
    }

    @Test
    fun `the first holder keeps its id and later twins get max plus one`() {
        val b = MasterBackup(bills = listOf(bill(3, "A"), bill(7, "B"), bill(3, "C"), bill(7, "D")))
        assertEquals(listOf(3L, 7L, 8L, 9L), ids(b.withUniqueIds()))
    }

    @Test
    fun `zero and negative ids are re-keyed, since upsert would treat them as new`() {
        val b = MasterBackup(bills = listOf(bill(0, "A"), bill(2, "B"), bill(-1, "C")))
        assertEquals(listOf(3L, 2L, 4L), ids(b.withUniqueIds()))
    }

    @Test
    fun `a healthy vault comes back as the very same instance`() {
        val b = MasterBackup(
            logins = listOf(LoginEntity(id = 1, siteName = "a"), LoginEntity(id = 2, siteName = "b")),
            bills = listOf(bill(1, "A"), bill(2, "B"))
        )
        assertSame(b, b.withUniqueIds(), "no change means no save on unlock")
    }

    @Test
    fun `every id-keyed section is repaired, not only bills`() {
        val b = MasterBackup(
            logins = listOf(LoginEntity(id = 3, siteName = "a"), LoginEntity(id = 3, siteName = "b")),
            ledger = listOf(TransactionEntity(id = 5, accountId = 1), TransactionEntity(id = 5, accountId = 1))
        ).withUniqueIds()
        assertEquals(listOf(3, 4), b.logins.map { it.id })
        assertEquals(listOf(5L, 6L), b.ledger.map { it.id })
    }

    @Test
    fun `accounts are never renumbered, so their transactions stay attached`() {
        val accounts = listOf(AccountEntity(id = 1, name = "Cash"), AccountEntity(id = 1, name = "Bank"))
        val b = MasterBackup(accounts = accounts, ledger = listOf(TransactionEntity(id = 1, accountId = 1)))
        val fixed = b.withUniqueIds()
        assertSame(b.accounts, fixed.accounts)
        assertEquals(1L, fixed.ledger.single().accountId)
    }
}
