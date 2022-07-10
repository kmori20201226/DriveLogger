package com.kmoriproj.drivelogger.common

import java.text.SimpleDateFormat
import java.util.*

class DateTimeString {
    companion object {
        fun formatDateTime(t:Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").localTime().format(Date(t))
        fun formatDate(t:Long): String = SimpleDateFormat("yyyy-MM-dd").localTime().format(Date(t))
        fun elapsed(t2:Long, t1:Long=0): String {
            val e = (t2 - t1) / 1000
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
