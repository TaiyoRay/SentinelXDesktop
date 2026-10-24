package com.nikhil.sentinelx.desktop.core.format

/**
 * Makes every record id unique within its own collection — the desktop's stand-in for
 * Room's autoincrement.
 *
 * DESKTOP-ONLY, deliberately NOT in the mirrored [VaultMerge] / [MasterBackup] files.
 * The phone never needs it: its import maps every row through `copy(id = 0)` and lets
 * Room hand out fresh keys. The desktop has no database, so a merged archive arrives
 * with the phone's ids intact, next to the desktop's own — and both sides count from 1.
 * A phone bill `id = 3` and a desktop bill `id = 3` then sit in one list, which:
 *  - crashes any LazyColumn keyed on `id` ("Key "3" was already used") — the Bills
 *    filter chips, reported 24 Oct 2026;
 *  - makes every by-id edit hit BOTH twins: ticking one bill paid ticks the other,
 *    an edit replaces the wrong row, and the bridge can fill the wrong login.
 *
 * Rules:
 *  - The first record holding an id keeps it; a later duplicate (or a 0 / negative id,
 *    which `upsert*` would treat as "new") gets the next free id, max + 1 upward.
 *  - Nothing else in a record changes, and the order of every list is kept.
 *  - **Accounts are left alone.** Ledger transactions point at them by `accountId`, so
 *    renumbering one would silently move its transactions to a stranger; the ledger
 *    merge already guarantees unique account ids. Folders join notes by *name*, and
 *    nothing points at any other record's id, so those are safe to renumber.
 *  - Returns the very same instance when nothing needed fixing, so callers can tell
 *    whether a save is warranted (`repaired !== original`).
 */
fun MasterBackup.withUniqueIds(): MasterBackup {
    val fixedLogins = logins.uniqueInt({ it.id }) { r, id -> r.copy(id = id) }
    val fixedArtifacts = artifacts.uniqueInt({ it.id }) { r, id -> r.copy(id = id) }
    val fixedChronicles = chronicles.uniqueInt({ it.id }) { r, id -> r.copy(id = id) }
    val fixedNotes = prophecies.uniqueInt({ it.id }) { r, id -> r.copy(id = id) }
    val fixedFolders = noteFolders.uniqueInt({ it.id }) { r, id -> r.copy(id = id) }
    val fixedLedger = ledger.uniqueLong({ it.id }) { r, id -> r.copy(id = id) }
    val fixedCash = cashBook.uniqueLong({ it.id }) { r, id -> r.copy(id = id) }
    val fixedBank = bankTxns.uniqueLong({ it.id }) { r, id -> r.copy(id = id) }
    val fixedBills = bills.uniqueLong({ it.id }) { r, id -> r.copy(id = id) }

    val unchanged = fixedLogins === logins && fixedArtifacts === artifacts &&
        fixedChronicles === chronicles && fixedNotes === prophecies &&
        fixedFolders === noteFolders && fixedLedger === ledger &&
        fixedCash === cashBook && fixedBank === bankTxns && fixedBills === bills
    if (unchanged) return this

    return copy(
        logins = fixedLogins,
        artifacts = fixedArtifacts,
        chronicles = fixedChronicles,
        prophecies = fixedNotes,
        noteFolders = fixedFolders,
        ledger = fixedLedger,
        cashBook = fixedCash,
        bankTxns = fixedBank,
        bills = fixedBills
    )
}

/** Same list instance back when every id is already positive and unique. */
private inline fun <T> List<T>.uniqueInt(id: (T) -> Int, withId: (T, Int) -> T): List<T> {
    val seen = HashSet<Int>(size * 2)
    if (all { id(it) > 0 && seen.add(id(it)) }) return this
    seen.clear()
    var next = (maxOfOrNull(id) ?: 0).coerceAtLeast(0) + 1
    return map { record ->
        val current = id(record)
        if (current > 0 && seen.add(current)) record
        else withId(record, next).also { seen.add(next); next++ }
    }
}

/** Same list instance back when every id is already positive and unique. */
private inline fun <T> List<T>.uniqueLong(id: (T) -> Long, withId: (T, Long) -> T): List<T> {
    val seen = HashSet<Long>(size * 2)
    if (all { id(it) > 0L && seen.add(id(it)) }) return this
    seen.clear()
    var next = (maxOfOrNull(id) ?: 0L).coerceAtLeast(0L) + 1L
    return map { record ->
        val current = id(record)
        if (current > 0L && seen.add(current)) record
        else withId(record, next).also { seen.add(next); next++ }
    }
}
