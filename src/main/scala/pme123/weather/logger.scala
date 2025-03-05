package pme123.weather

import scala.scalajs.js.Date

enum LogLevel:
  case DEBUG, INFO, WARN, ERROR
  
  def value: Int = this match
    case DEBUG => 0
    case INFO  => 1
    case WARN  => 2
    case ERROR => 3

object WeatherLogger {
  private var currentLevel: LogLevel = LogLevel.INFO
  
  def setLogLevel(level: LogLevel): Unit = 
    currentLevel = level
    
  private def timestamp = {
    val now = new Date()
    f"${now.getHours().toInt}%02d:${now.getMinutes().toInt}%02d:${now.getSeconds().toInt}%02d"
  }
  
  private def shouldLog(level: LogLevel): Boolean = 
    level.value >= currentLevel.value
  
  def debug(msg: String): Unit = 
    if shouldLog(LogLevel.DEBUG) then
      println(s"[DEBUG] $timestamp - $msg")
  
  def info(msg: String): Unit = 
    if shouldLog(LogLevel.INFO) then
      println(s"[INFO] $timestamp - $msg")
  
  def warn(msg: String): Unit = 
    if shouldLog(LogLevel.WARN) then
      println(s"[WARN] $timestamp - $msg")
  
  def error(msg: String, throwable: Throwable = null): Unit = {
    println(s"[ERROR] $timestamp - $msg")
    if (throwable != null) {
      println(s"[ERROR] Stack trace: ${throwable.getStackTrace.mkString("\n")}")
    }
  }
} 