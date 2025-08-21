package pme123.weather.meteoschweiz

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

// MeteoSwiss API endpoints
val meteoSwissApiUrl = "https://data.geo.admin.ch/api/stac/v1/search"
val meteoSwissApiUI = "https://opendatadocs.meteoswiss.ch/e-forecast-data/e2-e3-numerical-weather-forecasting-model"

// ICON-CH2 collection identifier
val iconCh2Collection = "ch.meteoschweiz.ogd-forecasting-icon-ch2"

// Available parameters for ICON-CH2
object MeteoSwissParameters:
  val TEMPERATURE_2M = "T_2M"           // Temperature at 2m above ground
  val PRESSURE_MSL = "PMSL"             // Mean sea level pressure
  val SURFACE_PRESSURE = "PS"           // Surface pressure
  val WIND_SPEED_10M = "FF_10M"         // Wind speed at 10m
  val WIND_GUST_10M = "FX_10M"          // Wind gust at 10m
  val WIND_DIRECTION_10M = "DD_10M"     // Wind direction at 10m
  val TOTAL_PRECIPITATION = "TOT_PREC"  // Total precipitation
  val RELATIVE_HUMIDITY_2M = "RELHUM_2M" // Relative humidity at 2m
  val CLOUD_COVER = "CLCT"              // Total cloud cover

// Time formatters
val meteoSwissTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
val displayTimeFormatter = DateTimeFormatter.ofPattern("EEE d. MMM HH:mm")

def formatMeteoSwissTime(time: String): String =
  val date = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
  date.format(displayTimeFormatter)

def getLatestInitializationTime(): String =
  // ICON-CH2 runs every 6 hours at 00, 06, 12, 18 UTC
  val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
  val hour = nowUtc.getHour
  val latestInitHour = (hour / 6) * 6
  val referenceTime = nowUtc.withHour(latestInitHour).withMinute(0).withSecond(0).withNano(0)
  referenceTime.format(meteoSwissTimeFormatter)

def getHorizonString(hoursAhead: Int): String =
  // Convert hours to ISO 8601 duration format
  f"P0DT${hoursAhead}%02dH00M00S"

// Helper to get multiple forecast horizons
def getForecastHorizons(maxHours: Int = 120): Seq[String] =
  (0 until maxHours).map(getHorizonString)
