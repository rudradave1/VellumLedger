package com.vellum.ledger.sync

/**
 * Schedules a durable sync attempt on platforms that support background work.
 * Android enqueues WorkManager jobs; other platforms can no-op.
 */
expect fun scheduleLedgerSync()
