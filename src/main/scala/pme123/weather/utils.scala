package pme123.weather

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val fromFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

def toDate(time: String): LocalDateTime =
  LocalDateTime.parse(time, fromFormatter)
  
def formatTime(time: String): String =
  val to = DateTimeFormatter.ofPattern("EEE d. MMM HH:mm")
  val date = LocalDateTime.parse(time, fromFormatter)
  date.format(to)