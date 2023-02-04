package com.darkrockstudios.apps.hammer.common.util

import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter

actual fun LocalDateTime.format(format: String): String {
	val dateFormatter = NSDateFormatter()
	dateFormatter.dateFormat = format
	return dateFormatter.stringFromDate(
		toNSDate(NSCalendar.currentCalendar)
			?: throw IllegalStateException("Could not convert kotlin date to NSDate $this")
	)
}