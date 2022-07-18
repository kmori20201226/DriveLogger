package com.kmoriproj.drivelogger.common

import java.text.SimpleDateFormat
import java.util.*

class DateTimeString {
    companion object {
        fun formatDateTime(ms:Long): String = SimpleDateFormat("MM/dd HH:mm").localTime().format(Date(ms))
        fun formatDate(ms:Long): String = SimpleDateFormat("yyyy-MM-dd").localTime().format(Date(ms))
        fun elapsed(t2:Long, t1:Long=0): String {
            val e = ((t2 - t1) / 1000) / 60
            val min = e % 60
            val hour = e / 60
            return if (hour == 0L) {
                    "%dmin".format(min)
                } else {
                    val days = hour / 24
                    if (days == 0L) {
                        "%dhr%dmin".format(hour, min)
                    } else if (days == 1L) {
                        "1day%dhr%dmin".format(hour, min)
                    } else {
                        "%ddays%dhr%dmin".format(days, hour, min)
                    }
                }
        }
    }
}

private fun SimpleDateFormat.localTime() : SimpleDateFormat {
    this.timeZone = TimeZone.getDefault()
    return this
}
