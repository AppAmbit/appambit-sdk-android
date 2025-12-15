package com.appambitsdk.test.unit.utils

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DateUtils {
    companion object{
        fun getUtcNow(): Date {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            return calendar.getTime()
        }
    }
}