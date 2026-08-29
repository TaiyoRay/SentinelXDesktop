package com.nikhil.sentinelx.desktop.core.format

/**
 * Where the Cash Book and Bank Book live now. The one place the name appears —
 * rename it here once the standalone app is named.
 */
const val HANDOFF_APP_NAME = "the new finance app"

/**
 * The two books moved out of SentinelX into a standalone app.
 *
 * Only the panes left. `MasterBackup.cashBook` / `bankTxns`, their `VaultSection`
 * tags and the `VaultMerge` identities all stay, so every archive still round-trips
 * and nothing here deletes a record. What remains is the one door out: a
 * password-sealed archive of exactly those two sections, which the other app imports.
 */
object CashBankHandoff {
    val SECTIONS = listOf(VaultSection.CASHBOOK, VaultSection.BANK)

    const val LABEL = "Cash & Bank Books"

    /** Names the file after what it holds — `Sentinel_CashBank_<millis>.sxv`. */
    const val FILE_STEM = "CashBank"
}
