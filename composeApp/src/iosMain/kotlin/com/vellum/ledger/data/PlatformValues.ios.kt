package com.vellum.ledger.data

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000).toLong()

actual fun newLedgerId(): String = NSUUID().UUIDString()
