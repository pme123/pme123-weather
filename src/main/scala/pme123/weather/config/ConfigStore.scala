package pme123.weather.config

import com.raquo.laminar.api.L.{Signal, Var}
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.scalajs.dom
import pme123.weather.{WeatherLogger, defaultStationDiffs}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

enum FolderStatus:
  case Disconnected, Connected, NeedsPermission

// Manages weather configurations, analogous to pme123-windspotter's ConfigService:
// - The default configuration is derived from the hardcoded data in data.scala (read-only).
// - Custom configurations are named, user-created, stored as JSON in localStorage, and
//   optionally mirrored as one weather-configs.json file in a user-picked folder.
// - Exactly one configuration is "active" at a time (see Main.scala, which reactively
//   rebuilds the tabs/map from activeConfigVar - no page reload needed to switch).
object ConfigStore:

  private val CONFIGS_KEY = "weatherConfigs"
  private val ACTIVE_KEY  = "weatherActiveConfig"

  val defaultConfigName = "Default"

  lazy val defaultConfig: WeatherConfig = WeatherConfig.toConfig(defaultConfigName, defaultStationDiffs)

  val folderStatus: Var[FolderStatus]   = Var(FolderStatus.Disconnected)
  val folderName:   Var[Option[String]] = Var(None)

  private var dirHandle: Option[FileSystemDirectoryHandle] = None

  val customConfigsVar: Var[List[WeatherConfig]] = Var(loadCustomConfigs())

  val activeConfigVar: Var[WeatherConfig] = Var {
    Option(dom.window.localStorage.getItem(ACTIVE_KEY))
      .flatMap(findConfig)
      .getOrElse(defaultConfig)
  }

  val allConfigsSignal: Signal[List[WeatherConfig]] = customConfigsVar.signal.map(defaultConfig :: _)

  def allConfigs: List[WeatherConfig] = defaultConfig :: customConfigsVar.now()

  def findConfig(name: String): Option[WeatherConfig] = allConfigs.find(_.name == name)

  def isDefault(name: String): Boolean = name == defaultConfigName

  // Returns a name that does not collide with any existing configuration.
  def uniqueName(base: String): String =
    val names = allConfigs.map(_.name).toSet
    if !names.contains(base) then base
    else
      LazyList.from(2).map(i => s"$base $i").find(n => !names.contains(n)).get

  // Adds a new custom configuration or replaces the one with the same name.
  def saveConfig(config: WeatherConfig): Unit =
    if !isDefault(config.name) then
      val current = customConfigsVar.now()
      val updated =
        if current.exists(_.name == config.name) then current.map(c => if c.name == config.name then config else c)
        else current :+ config
      customConfigsVar.set(updated)
      persist()
      // Keep the active configuration in sync when it was edited
      if activeConfigVar.now().name == config.name then activeConfigVar.set(config)

  // Replaces the configuration stored under oldName (handles renaming).
  def saveConfigAs(oldName: String, config: WeatherConfig): Unit =
    if oldName != config.name && !isDefault(oldName) then
      customConfigsVar.update(_.filterNot(_.name == oldName))
    saveConfig(config)

  def deleteConfig(name: String): Unit =
    if !isDefault(name) then
      customConfigsVar.update(_.filterNot(_.name == name))
      persist()
      if activeConfigVar.now().name == name then activate(defaultConfig)

  def activate(config: WeatherConfig): Unit =
    dom.window.localStorage.setItem(ACTIVE_KEY, config.name)
    activeConfigVar.set(config)

  private def persist(): Unit =
    persistLocal()
    writeToFolder()

  private def persistLocal(): Unit =
    dom.window.localStorage.setItem(CONFIGS_KEY, customConfigsVar.now().asJson.noSpaces)

  private def loadCustomConfigs(): List[WeatherConfig] =
    Option(dom.window.localStorage.getItem(CONFIGS_KEY))
      .flatMap: json =>
        decode[List[WeatherConfig]](json) match
          case Right(configs) => Some(configs.filterNot(c => isDefault(c.name)))
          case Left(err) =>
            WeatherLogger.warn(s"Konfigurationen konnten nicht gelesen werden: ${err.getMessage}")
            None
      .getOrElse(List.empty)

  // ── Folder sync (File System Access API) ────────────────────────────────

  // Silent reconnect at app start: only succeeds without a click if the browser still
  // grants permission for the previously-picked folder; otherwise it just needs a click
  // later ("Verbinden") since requesting permission requires a user gesture. Runs in the
  // background - the app already rendered from localStorage by the time this resolves.
  def initFolderSync(): Future[Unit] =
    if !FsSync.isSupported then Future.successful(())
    else
      FsSync
        .getStoredHandle()
        .flatMap:
          case Some(handle) =>
            dirHandle = Some(handle)
            folderName.set(Some(handle.name))
            FsSync.verifyPermission(handle, requestIfNeeded = false).flatMap: granted =>
              if granted then
                folderStatus.set(FolderStatus.Connected)
                syncFromFolder(handle)
              else
                folderStatus.set(FolderStatus.NeedsPermission)
                Future.successful(())
          case None =>
            Future.successful(())
        .recover:
          case ex =>
            WeatherLogger.warn(s"Ordner-Sync: Reconnect fehlgeschlagen: ${ex.getMessage}")

  // Opens the picker (requires a click), remembers the handle, and merges configs found
  // there with local ones (folder wins on name collisions), seeding the file if it's new.
  def pickFolder(): Future[Unit] =
    FsSync
      .pickDirectory()
      .flatMap: handle =>
        dirHandle = Some(handle)
        folderName.set(Some(handle.name))
        folderStatus.set(FolderStatus.Connected)
        FsSync.readConfigFile(handle).flatMap:
          case Some(json) =>
            mergeFromFolder(json)
            Future.successful(())
          case None =>
            FsSync.writeConfigFile(handle, customConfigsVar.now().asJson.spaces2)

  def reconnectFolder(): Future[Unit] =
    dirHandle match
      case Some(handle) =>
        FsSync.verifyPermission(handle, requestIfNeeded = true).flatMap: granted =>
          if granted then
            folderStatus.set(FolderStatus.Connected)
            syncFromFolder(handle)
          else Future.successful(())
      case None => Future.successful(())

  def disconnectFolder(): Unit =
    FsSync.clearStoredHandle()
    dirHandle = None
    folderName.set(None)
    folderStatus.set(FolderStatus.Disconnected)

  private def syncFromFolder(handle: FileSystemDirectoryHandle): Future[Unit] =
    FsSync.readConfigFile(handle).map:
      case Some(json) => mergeFromFolder(json)
      case None       => ()

  // Folder configs win over local ones with the same name; local-only configs are kept
  // (and get written out below via writeToFolder from any subsequent save).
  private def mergeFromFolder(json: String): Unit =
    decode[List[WeatherConfig]](json) match
      case Left(err) =>
        WeatherLogger.warn(s"Ordner-Konfiguration konnte nicht gelesen werden: ${err.getMessage}")
      case Right(folderConfigs) =>
        val valid = folderConfigs.filterNot(c => c.name.trim.isEmpty || isDefault(c.name))
        if valid.nonEmpty then
          val current = customConfigsVar.now()
          val merged =
            current.map(c => valid.find(_.name == c.name).getOrElse(c)) ++
              valid.filterNot(c => current.exists(_.name == c.name))
          customConfigsVar.set(merged)
          persistLocal()
          val active = activeConfigVar.now()
          if !isDefault(active.name) then
            merged.find(_.name == active.name).foreach: updated =>
              if updated != active then activeConfigVar.set(updated)

  private def writeToFolder(): Unit =
    if folderStatus.now() == FolderStatus.Connected then
      dirHandle.foreach: handle =>
        FsSync
          .writeConfigFile(handle, customConfigsVar.now().asJson.spaces2)
          .recover:
            case ex => WeatherLogger.warn(s"Ordner-Sync: Schreiben fehlgeschlagen: ${ex.getMessage}")

end ConfigStore
