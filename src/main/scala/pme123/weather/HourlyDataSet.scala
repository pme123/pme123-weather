package pme123.weather

case class HourlyDataSet(
    time: String,
    temperature_2m: Double,
    pressure_msl: Double,
    surface_pressure: Double,
    wind_speed_10m: Double,
    wind_gusts_10m: Double,
    wind_direction_10m: Double
)

