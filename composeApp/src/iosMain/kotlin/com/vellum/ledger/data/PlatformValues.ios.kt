package com.vellum.ledger.data

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000).toLong()

actual fun newLedgerId(): String = NSUUID().UUIDString()

actual val appVersion: String = "1.2.0-ios"

actual fun shareText(text: String, title: String) {
    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController
    
    val activityViewController = UIActivityViewController(listOf(text), null)
    rootViewController?.presentViewController(activityViewController, true, null)
}
