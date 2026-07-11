package pme123.weather.config

import org.scalajs.dom
import org.scalajs.dom.IDBTransactionMode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.thenable2future
import scala.scalajs.js.annotation.JSGlobal

// Minimal facade for the File System Access API - scalajs-dom doesn't cover it (only
// IndexedDB, used below to remember the picked directory across reloads).
@js.native
trait FileSystemFileHandle extends js.Object:
  def getFile(): js.Promise[FsSync.JsFile]             = js.native
  def createWritable(): js.Promise[FsSync.Writable]    = js.native

@js.native
trait FileSystemDirectoryHandle extends js.Object:
  val name: String                                                                       = js.native
  def getFileHandle(name: String, options: js.UndefOr[js.Object] = js.native): js.Promise[FileSystemFileHandle] = js.native
  def queryPermission(options: js.Object): js.Promise[String]                            = js.native
  def requestPermission(options: js.Object): js.Promise[String]                           = js.native

@js.native
@JSGlobal("window")
object FsWindow extends js.Object:
  def showDirectoryPicker(options: js.Object): js.Promise[FileSystemDirectoryHandle] = js.native

object FsSync:

  @js.native
  trait JsFile extends js.Object:
    def text(): js.Promise[String] = js.native

  @js.native
  trait Writable extends js.Object:
    def write(data: String): js.Promise[Unit] = js.native
    def close(): js.Promise[Unit]             = js.native

  // Holds the whole list of custom configurations (see ConfigStore), not just one.
  val CONFIG_FILENAME = "weather-configs.json"

  private val DB_NAME    = "pme123-weather-fs"
  private val STORE_NAME = "handles"
  private val HANDLE_KEY = "configDir"

  def isSupported: Boolean =
    js.typeOf(js.Dynamic.global.window.selectDynamic("showDirectoryPicker")) != "undefined"

  def pickDirectory(): Future[FileSystemDirectoryHandle] =
    FsWindow
      .showDirectoryPicker(js.Dynamic.literal(id = "pme123-weather-config", mode = "readwrite").asInstanceOf[js.Object])
      .toFuture
      .flatMap(handle => storeHandle(handle).map(_ => handle))

  def verifyPermission(handle: FileSystemDirectoryHandle, requestIfNeeded: Boolean): Future[Boolean] =
    val opts = js.Dynamic.literal(mode = "readwrite").asInstanceOf[js.Object]
    handle.queryPermission(opts).toFuture.flatMap:
      case "granted" => Future.successful(true)
      case _ =>
        if requestIfNeeded then handle.requestPermission(opts).toFuture.map(_ == "granted")
        else Future.successful(false)

  def readConfigFile(handle: FileSystemDirectoryHandle): Future[Option[String]] =
    handle
      .getFileHandle(CONFIG_FILENAME)
      .toFuture
      .flatMap(_.getFile().toFuture)
      .flatMap(_.text().toFuture)
      .map(Some(_))
      .recover { case _ => None }

  def writeConfigFile(handle: FileSystemDirectoryHandle, text: String): Future[Unit] =
    for
      fileHandle <- handle
        .getFileHandle(CONFIG_FILENAME, js.Dynamic.literal(create = true).asInstanceOf[js.Object])
        .toFuture
      writable   <- fileHandle.createWritable().toFuture
      _          <- writable.write(text).toFuture
      _          <- writable.close().toFuture
    yield ()

  // --- Directory handle persistence (IndexedDB, via scalajs-dom's typed API) ---

  private def openDb(): Future[dom.IDBDatabase] =
    val p   = Promise[dom.IDBDatabase]()
    val req = dom.window.indexedDB.get.open(DB_NAME, 1)
    req.onupgradeneeded = _ => req.result.createObjectStore(STORE_NAME)
    req.onsuccess = _ => p.success(req.result)
    req.onerror = _ => p.failure(new Exception("IndexedDB: konnte Datenbank nicht öffnen"))
    p.future

  def storeHandle(handle: FileSystemDirectoryHandle): Future[Unit] =
    openDb().flatMap: db =>
      val p  = Promise[Unit]()
      val tx = db.transaction(STORE_NAME, IDBTransactionMode.readwrite)
      tx.objectStore(STORE_NAME).put(handle, HANDLE_KEY)
      tx.oncomplete = _ => p.success(())
      tx.onerror = _ => p.failure(new Exception("IndexedDB: konnte Handle nicht speichern"))
      p.future

  def getStoredHandle(): Future[Option[FileSystemDirectoryHandle]] =
    openDb().flatMap: db =>
      val p   = Promise[Option[FileSystemDirectoryHandle]]()
      val tx  = db.transaction(STORE_NAME, IDBTransactionMode.readonly)
      val req = tx.objectStore(STORE_NAME).get(HANDLE_KEY)
      req.onsuccess = _ =>
        val result = req.result
        p.success(if js.isUndefined(result) then None else Some(result.asInstanceOf[FileSystemDirectoryHandle]))
      req.onerror = _ => p.failure(new Exception("IndexedDB: konnte Handle nicht lesen"))
      p.future

  def clearStoredHandle(): Future[Unit] =
    openDb().flatMap: db =>
      val p  = Promise[Unit]()
      val tx = db.transaction(STORE_NAME, IDBTransactionMode.readwrite)
      tx.objectStore(STORE_NAME).delete(HANDLE_KEY)
      tx.oncomplete = _ => p.success(())
      tx.onerror = _ => p.failure(new Exception("IndexedDB: konnte Handle nicht löschen"))
      p.future

end FsSync
