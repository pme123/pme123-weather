package pme123.weather

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val openMeteoArchiveUrl = "https://archive-api.open-meteo.com/v1/archive"
val openMeteoForcastUrl = "https://api.open-meteo.com/v1/forecast"

val fromFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

def toDate(time: String): LocalDateTime =
  LocalDateTime.parse(time, fromFormatter)
  
def formatTime(time: String): String =
  val to = DateTimeFormatter.ofPattern("EEE d. MMM HH:mm")
  val date = LocalDateTime.parse(time, fromFormatter)
  date.format(to)
 
val tickformat = "%a %-d.%-m %-Hh"