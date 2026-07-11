package pme123.weather

import com.raquo.laminar.api.L.{*, given}
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.scalajs.dom
import pme123.weather.config.{ConfigArea, ConfigDiff, ConfigStation, ConfigStore, FolderStatus, FsSync, KnownStations, WeatherConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

// Modal editor for weather configurations, analogous to pme123-windspotter's
// ConfigEditorDialog: select / activate / duplicate / delete configurations, structured
// editing of Gebiete (areas) with their Windstationen and Druckdifferenzen, plus a raw
// JSON tab with import/export as .json files.
object ConfigEditorDialog:

  private val openVar          = Var(false)
  private val selectedNameVar  = Var(ConfigStore.defaultConfigName)
  private val draftVar         = Var(ConfigStore.defaultConfig)
  private val selectedAreaIdxVar = Var(0)
  private val jsonModeVar      = Var(false)
  private val jsonTextVar      = Var("")
  private val statusVar        = Var(Option.empty[(String, Boolean)])

  private val readOnlySignal: Signal[Boolean] =
    selectedNameVar.signal.map(ConfigStore.isDefault).distinct

  private val selectedAreaSignal: Signal[Option[ConfigArea]] =
    draftVar.signal
      .combineWith(selectedAreaIdxVar.signal)
      .map { case (cfg, idx) => cfg.areas.lift(idx) }

  // Only flips true/false when the selection becomes valid/invalid (area added/removed,
  // or the dialog just opened) - NOT on every keystroke inside the currently selected
  // area. Used to gate the outer areaDetailsPanel rebuild so the stations mini map (and
  // everything else in there) doesn't get torn down and re-created on every edit.
  private val hasSelectedAreaSignal: Signal[Boolean] =
    draftVar.signal
      .combineWith(selectedAreaIdxVar.signal)
      .map { case (cfg, idx) => cfg.areas.indices.contains(idx) }
      .distinct

  private var configMapInstance: js.UndefOr[js.Dynamic] = js.undefined

  def open(): Unit =
    selectConfig(ConfigStore.activeConfigVar.now().name)
    openVar.set(true)

  def apply(): HtmlElement =
    div(
      child <-- openVar.signal.map:
        case false => emptyNode
        case true  => dialog()
    )

  // ── state helpers ────────────────────────────────────────────────────────

  private def selectConfig(name: String): Unit =
    val cfg = ConfigStore.findConfig(name).getOrElse(ConfigStore.defaultConfig)
    selectedNameVar.set(cfg.name)
    draftVar.set(cfg)
    selectedAreaIdxVar.set(0)
    jsonModeVar.set(false)
    statusVar.set(None)

  private def setMessage(text: String, isError: Boolean): Unit =
    statusVar.set(Some(text -> isError))

  private def isDirty: Boolean =
    ConfigStore.findConfig(selectedNameVar.now()) match
      case Some(saved) => saved.asJson.noSpaces != draftVar.now().asJson.noSpaces
      case None        => true

  private def confirmDiscard(): Boolean =
    !isDirty || dom.window.confirm("Ungespeicherte Änderungen verwerfen?")

  private def trySave(): Boolean =
    val oldName = selectedNameVar.now()
    if ConfigStore.isDefault(oldName) then true // built-in, nothing to save
    else
      val name = draftVar.now().name.trim
      if name.isEmpty then
        setMessage("Der Name darf nicht leer sein.", true)
        false
      else if ConfigStore.isDefault(name) then
        setMessage(s"'$name' ist für die Standard-Konfiguration reserviert.", true)
        false
      else if name != oldName && ConfigStore.findConfig(name).isDefined then
        setMessage(s"Eine Konfiguration namens '$name' existiert bereits.", true)
        false
      else
        val draft = draftVar.now().copy(name = name)
        draftVar.set(draft)
        ConfigStore.saveConfigAs(oldName, draft)
        selectedNameVar.set(name)
        setMessage(s"'$name' gespeichert.", false)
        true

  private def activateSelected(): Unit =
    if trySave() then
      val cfg = ConfigStore.findConfig(draftVar.now().name).getOrElse(ConfigStore.defaultConfig)
      ConfigStore.activate(cfg)
      setMessage(s"'${cfg.name}' aktiviert.", false)

  private def createConfig(base: WeatherConfig, baseName: String): Unit =
    val cfg = base.copy(name = ConfigStore.uniqueName(baseName))
    ConfigStore.saveConfig(cfg)
    selectConfig(cfg.name)

  private def deleteSelected(): Unit =
    val name = selectedNameVar.now()
    if !ConfigStore.isDefault(name) && dom.window.confirm(s"Konfiguration '$name' löschen?") then
      ConfigStore.deleteConfig(name)
      selectConfig(ConfigStore.activeConfigVar.now().name)

  // ── draft update helpers ─────────────────────────────────────────────────

  private def updateAreas(f: Seq[ConfigArea] => Seq[ConfigArea]): Unit =
    draftVar.update(cfg => cfg.copy(areas = f(cfg.areas)))

  private def updateArea(idx: Int)(f: ConfigArea => ConfigArea): Unit =
    updateAreas(_.zipWithIndex.map((a, i) => if i == idx then f(a) else a))

  private def uniqueAreaId(base: String): String =
    val ids = draftVar.now().areas.map(_.id).toSet
    if !ids.contains(base) then base
    else LazyList.from(2).map(i => s"$base $i").find(n => !ids.contains(n)).get

  private def moveItem[A](list: Seq[A], idx: Int, delta: Int): Seq[A] =
    val newIdx = idx + delta
    if idx < 0 || idx >= list.length || newIdx < 0 || newIdx >= list.length then list
    else
      val item = list(idx)
      list.patch(idx, Nil, 1).patch(newIdx, List(item), 0)

  // ── import / export ──────────────────────────────────────────────────────

  private def exportSelected(): Unit =
    val cfg  = draftVar.now()
    val json = cfg.asJson.spaces2
    val bag  = js.Dynamic.literal("type" -> "application/json").asInstanceOf[dom.BlobPropertyBag]
    val blob = new dom.Blob(js.Array(json: dom.BlobPart), bag)
    val url  = dom.URL.createObjectURL(blob)
    val anchor = dom.document.createElement("a").asInstanceOf[dom.html.Anchor]
    anchor.href = url
    anchor.setAttribute("download", s"${cfg.name}.json")
    dom.document.body.appendChild(anchor)
    anchor.click()
    dom.document.body.removeChild(anchor)
    dom.URL.revokeObjectURL(url)

  private def importFile(file: dom.File): Unit =
    val reader = new dom.FileReader()
    reader.onload = _ =>
      decode[WeatherConfig](reader.result.asInstanceOf[String]) match
        case Right(cfg) =>
          val baseName =
            if cfg.name.trim.isEmpty || ConfigStore.isDefault(cfg.name) then "Importiert"
            else cfg.name.trim
          val named = cfg.copy(name = ConfigStore.uniqueName(baseName))
          ConfigStore.saveConfig(named)
          selectConfig(named.name)
          setMessage(s"'${named.name}' importiert.", false)
        case Left(error) =>
          setMessage(s"Import fehlgeschlagen: ${error.getMessage}", true)
    reader.readAsText(file)

  // ── view ──────────────────────────────────────────────────────────────────

  private def close(): Unit =
    if confirmDiscard() then openVar.set(false)

  // Suggestions for station name fields (see windStationRow/diffRow) - shared by all of
  // them via the HTML list/datalist mechanism, so typing shows matching known stations.
  private def knownStationsDatalist: HtmlElement =
    dataList(
      idAttr := "known-stations",
      KnownStations.all.map(s => option(value := s.name))
    )

  private def dialog(): HtmlElement =
    div(
      className := "info-modal-overlay",
      onClick --> { _ => close() },
      div(
        className := "info-modal config-modal",
        onClick --> { ev => ev.stopPropagation() },
        knownStationsDatalist,
        div(
          className := "info-modal-header",
          span(className := "info-modal-title", "Konfigurationen"),
          button(
            className := "info-modal-close",
            "✕",
            onClick --> { _ => close() }
          )
        ),
        div(
          className := "info-modal-body",
          toolbar(),
          folderRow(),
          nameRow(),
          tabsRow(),
          child <-- jsonModeVar.signal.distinct.map:
            case true  => jsonPanel()
            case false => editorPanel()
          ,
          footerRow()
        )
      )
    )

  private def toolbar(): HtmlElement =
    val importInput = input(
      typ     := "file",
      accept  := ".json,application/json",
      display := "none",
      onChange --> { ev =>
        val inputEl = ev.target.asInstanceOf[dom.html.Input]
        if inputEl.files.length > 0 then importFile(inputEl.files(0))
        inputEl.value = ""
      }
    )
    div(
      className := "cfg-toolbar",
      span(className := "cfg-toolbar-label", "Konfiguration:"),
      select(
        className := "cfg-select",
        onChange --> { ev =>
          val sel = ev.target.asInstanceOf[dom.html.Select]
          if confirmDiscard() then selectConfig(sel.value)
          else sel.value = selectedNameVar.now()
        },
        children <-- ConfigStore.allConfigsSignal.map(_.map { cfg =>
          option(
            value := cfg.name,
            cfg.name,
            selected <-- selectedNameVar.signal.map(_ == cfg.name)
          )
        })
      ),
      child <-- ConfigStore.activeConfigVar.signal
        .combineWith(selectedNameVar.signal)
        .map { case (active, selected) => active.name == selected }
        .distinct
        .map:
          case true  => span(className := "cfg-active-badge", "● aktiv")
          case false => emptyNode
      ,
      div(
        className := "cfg-toolbar-actions",
        button(
          className := "cfg-btn",
          "Neu",
          title := "Neue, leere Konfiguration erstellen",
          onClick --> { _ =>
            if confirmDiscard() then createConfig(WeatherConfig("", Seq.empty), "Meine Konfiguration")
          }
        ),
        button(
          className := "cfg-btn",
          "Duplizieren",
          title := "Editierbare Kopie der gewählten Konfiguration erstellen",
          onClick --> { _ => createConfig(draftVar.now(), s"${draftVar.now().name} Kopie") }
        ),
        button(
          className := "cfg-btn danger",
          "Löschen",
          disabled <-- readOnlySignal,
          onClick --> { _ => deleteSelected() }
        ),
        button(
          className := "cfg-btn",
          "Export",
          title := "Gewählte Konfiguration als JSON-Datei herunterladen",
          onClick --> { _ => exportSelected() }
        ),
        button(
          className := "cfg-btn",
          "Import",
          title := "Konfiguration aus JSON-Datei importieren",
          onClick --> { _ => importInput.ref.click() }
        ),
        importInput
      )
    )

  private def folderRow(): HtmlElement =
    if !FsSync.isSupported then
      div(
        className := "cfg-folder",
        span(
          className := "cfg-folder-status",
          span(className := "muted", "Ordner-Synchronisierung wird von diesem Browser nicht unterstützt (nur Chrome/Edge).")
        )
      )
    else
      div(
        className := "cfg-folder",
        child <-- ConfigStore.folderStatus.signal
          .combineWith(ConfigStore.folderName.signal)
          .map:
            case (FolderStatus.Disconnected, _) =>
              span(
                className := "cfg-folder-status",
                span(className := "muted", "Konfigurationen werden in diesem Browser gespeichert. "),
                button(
                  className := "cfg-btn",
                  "Ordner wählen…",
                  title := "Zusätzlich als JSON-Dateien in einem Ordner deiner Wahl speichern",
                  onClick --> { _ => ConfigStore.pickFolder().recover { case _ => () }; () }
                )
              )
            case (FolderStatus.NeedsPermission, name) =>
              span(
                className := "cfg-folder-status",
                span(className := "muted", s"Ordner '${name.getOrElse("")}' ist verknüpft, benötigt aber Berechtigung. "),
                button(
                  className := "cfg-btn",
                  "Verbinden",
                  onClick --> { _ => ConfigStore.reconnectFolder().recover { case _ => () }; () }
                ),
                button(
                  className := "cfg-btn",
                  "Trennen",
                  onClick --> { _ => ConfigStore.disconnectFolder() }
                )
              )
            case (FolderStatus.Connected, name) =>
              span(
                className := "cfg-folder-status",
                span(className := "cfg-folder-linked", s"✓ Speichert im Ordner '${name.getOrElse("")}'"),
                button(
                  className := "cfg-btn",
                  "Trennen",
                  onClick --> { _ => ConfigStore.disconnectFolder() }
                )
              )
      )

  private def nameRow(): HtmlElement =
    div(
      child <-- readOnlySignal.map:
        case true =>
          div(
            className := "cfg-hint",
            "Die Standard-Konfiguration ist fest im Code hinterlegt und schreibgeschützt. ",
            "Mit 'Duplizieren' erstellst du deine eigene editierbare Kopie."
          )
        case false =>
          div(
            className := "cfg-field cfg-name-field",
            label(className := "cfg-field-label", "Name"),
            input(
              className := "cfg-input",
              typ       := "text",
              value <-- draftVar.signal.map(_.name).distinct,
              onChange.mapToValue --> { v => draftVar.update(_.copy(name = v)) }
            )
          )
    )

  private def tabsRow(): HtmlElement =
    div(
      className := "cfg-tabs",
      button(
        className := "cfg-tab",
        cls("active") <-- jsonModeVar.signal.map(!_),
        "Editor",
        onClick --> { _ => jsonModeVar.set(false) }
      ),
      button(
        className := "cfg-tab",
        cls("active") <-- jsonModeVar.signal,
        "JSON",
        onClick --> { _ =>
          jsonTextVar.set(draftVar.now().asJson.spaces2)
          jsonModeVar.set(true)
        }
      )
    )

  // ── structured editor ────────────────────────────────────────────────────

  private def editorPanel(): HtmlElement =
    div(
      className := "cfg-editor",
      areasPanel(),
      areaDetailsPanel()
    )

  private def areasPanel(): HtmlElement =
    div(
      className := "cfg-groups",
      div(className := "cfg-panel-title", "Gebiete"),
      div(
        className := "cfg-group-list",
        children <-- draftVar.signal
          .map(_.areas.zipWithIndex)
          .split(_._2) { (idx, _, sig) => areaRow(idx, sig.map(_._1)) }
      ),
      button(
        className := "cfg-btn",
        disabled <-- readOnlySignal,
        "+ Gebiet hinzufügen",
        onClick --> { _ =>
          updateAreas(_ :+ ConfigArea(uniqueAreaId("Neues Gebiet"), "", 2, Seq.empty, Seq.empty))
          selectedAreaIdxVar.set(draftVar.now().areas.length - 1)
        }
      )
    )

  // Monochrome eye / eye-off icon (matches the ↑ ↓ ✕ icon style) instead of emoji, which
  // renders in its own color on most platforms and clashes with the rest of the UI.
  private def eyeIcon(open: Boolean) =
    val commonAttrs = Seq(
      svg.viewBox       := "0 0 24 24",
      svg.width         := "13",
      svg.height        := "13",
      svg.fill          := "none",
      svg.stroke        := "currentColor",
      svg.strokeWidth   := "2",
      svg.strokeLineCap  := "round",
      svg.strokeLineJoin := "round"
    )
    if open then
      svg.svg(
        commonAttrs,
        svg.path(svg.d := "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"),
        svg.circle(svg.cx := "12", svg.cy := "12", svg.r := "3")
      )
    else
      svg.svg(
        commonAttrs,
        svg.path(
          svg.d := "M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
        ),
        svg.line(svg.x1 := "1", svg.y1 := "1", svg.x2 := "23", svg.y2 := "23")
      )

  private def areaRow(idx: Int, areaSig: Signal[ConfigArea]): HtmlElement =
    div(
      className := "cfg-group-row",
      cls("selected") <-- selectedAreaIdxVar.signal.map(_ == idx),
      cls("cfg-hidden") <-- areaSig.map(_.hidden),
      onClick --> { _ => if selectedAreaIdxVar.now() != idx then selectedAreaIdxVar.set(idx) },
      input(
        className := "cfg-input cfg-group-name",
        typ       := "text",
        disabled <-- readOnlySignal,
        value <-- areaSig.map(_.id),
        onChange.mapToValue --> { v => updateArea(idx)(_.copy(id = v)) }
      ),
      span(
        className := "cfg-count",
        child.text <-- areaSig.map(a => s"${a.windStations.length}S/${a.diffs.length}D")
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title <-- areaSig.map(a => if a.hidden then "Gebiet einblenden" else "Gebiet ausblenden"),
        child <-- areaSig.map(a => eyeIcon(open = !a.hidden)),
        onClick.stopPropagation --> { _ => updateArea(idx)(a => a.copy(hidden = !a.hidden)) }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Nach oben",
        "↑",
        onClick.stopPropagation --> { _ => updateAreas(moveItem(_, idx, -1)) }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Nach unten",
        "↓",
        onClick.stopPropagation --> { _ => updateAreas(moveItem(_, idx, 1)) }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Gebiet löschen",
        "✕",
        onClick.stopPropagation --> { _ =>
          updateAreas(_.patch(idx, Nil, 1))
          selectedAreaIdxVar.update(i => math.max(0, math.min(i, draftVar.now().areas.length - 1)))
        }
      )
    )

  private def areaDetailsPanel(): HtmlElement =
    div(
      className := "cfg-details",
      child <-- hasSelectedAreaSignal.map:
        case false => div(className := "cfg-hint", "Kein Gebiet ausgewählt.")
        case true =>
          div(
            className := "cfg-area-body",
            areaFieldsForm(),
            div(className := "cfg-sublist-title", "Windstationen"),
            windStationsList(),
            addWindStationRow(),
            stationsMapSection(),
            div(className := "cfg-sublist-title", "Druckdifferenzen"),
            diffsList(),
            addDiffRow()
          )
    )

  // Mini map with every known station as a grey dot (analog pme123-windalert's overview
  // map) - hover shows the name, clicking adds it as a Windstation to whichever Gebiet is
  // currently selected in the left list. The map itself is only (re)created when
  // areaDetailsPanel's outer subtree is rebuilt (see hasSelectedAreaSignal), not on every
  // keystroke or area switch.
  private def stationsMapSection(): HtmlElement =
    div(
      className := "cfg-map-section",
      div(
        className := "cfg-sublist-title",
        child.text <-- selectedAreaSignal.map(a => s"Station auf der Karte zu '${a.map(_.id).getOrElse("")}' hinzufügen")
      ),
      div(
        idAttr    := "config-stations-map",
        className := "cfg-stations-map",
        onMountUnmountCallback(
          mount   = _ => initConfigMap(),
          unmount = _ => destroyConfigMap()
        )
      )
    )

  private def initConfigMap(): Unit =
    val lMap = Leaflet.map("config-stations-map", js.Dynamic.literal(zoomControl = true).asInstanceOf[js.Object])

    Leaflet
      .tileLayer(
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png",
        js.Dynamic
          .literal(attribution = "&copy; OpenStreetMap contributors &copy; CARTO", maxZoom = 19)
          .asInstanceOf[js.Object]
      )
      .addTo(lMap)

    KnownStations.all.foreach: station =>
      val marker = Leaflet.marker(
        js.Array(station.latitude, station.longitude),
        js.Dynamic.literal(icon = MapView.greyDotIcon()).asInstanceOf[js.Object]
      )
      marker.bindTooltip(station.name)
      marker.on("click", () => addKnownStationToSelectedArea(station))
      marker.addTo(lMap)

    configMapInstance = lMap

    MapView.waitUntilSized("config-stations-map", triesLeft = 20): () =>
      lMap.invalidateSize()
      val lats = KnownStations.all.map(_.latitude)
      val lons = KnownStations.all.map(_.longitude)
      lMap.fitBounds(
        js.Array(js.Array(lats.min, lons.min), js.Array(lats.max, lons.max)),
        js.Dynamic.literal(padding = js.Array(10, 10)).asInstanceOf[js.Object]
      )
  end initConfigMap

  private def destroyConfigMap(): Unit =
    configMapInstance.foreach(_.remove())
    configMapInstance = js.undefined

  private def addKnownStationToSelectedArea(station: WeatherStation): Unit =
    if !ConfigStore.isDefault(selectedNameVar.now()) then
      val aIdx = selectedAreaIdxVar.now()
      updateArea(aIdx): a =>
        if a.windStations.exists(_.name.equalsIgnoreCase(station.name)) then a
        else a.copy(windStations = a.windStations :+ ConfigStation(station.name, station.latitude, station.longitude))

  private def areaFieldsForm(): HtmlElement =
    div(
      className := "cfg-area-fields",
      div(
        className := "cfg-field",
        label(className := "cfg-field-label", "Titel"),
        input(
          className := "cfg-input",
          typ       := "text",
          disabled <-- readOnlySignal,
          value <-- selectedAreaSignal.map(_.map(_.label).getOrElse("")),
          onChange.mapToValue --> { v => updateArea(selectedAreaIdxVar.now())(_.copy(label = v)) }
        )
      ),
      div(
        className := "cfg-field",
        label(className := "cfg-field-label", "Schwelle (hPa)"),
        input(
          className := "cfg-input",
          typ       := "number",
          stepAttr  := "1",
          disabled <-- readOnlySignal,
          value <-- selectedAreaSignal.map(_.map(_.threshold.toString).getOrElse("0")),
          onChange.mapToValue --> { v =>
            updateArea(selectedAreaIdxVar.now())(_.copy(threshold = v.toIntOption.getOrElse(0)))
          }
        )
      )
    )

  private def windStationsList(): HtmlElement =
    div(
      className := "cfg-station-list",
      children <-- draftVar.signal
        .combineWith(selectedAreaIdxVar.signal)
        .map { case (cfg, aIdx) => cfg.areas.lift(aIdx).map(_.windStations.zipWithIndex).getOrElse(Nil) }
        .split(_._2) { (idx, _, sig) => windStationRow(idx, sig.map(_._1)) }
    )

  private def windStationRow(idx: Int, stationSig: Signal[ConfigStation]): HtmlElement =
    def upd(f: ConfigStation => ConfigStation): Unit =
      val aIdx = selectedAreaIdxVar.now()
      updateArea(aIdx)(a => a.copy(windStations = a.windStations.zipWithIndex.map((s, i) => if i == idx then f(s) else s)))

    div(
      className := "cfg-station-row",
      input(
        className := "cfg-input",
        typ       := "text",
        placeholder := "Name",
        listId    := "known-stations",
        disabled <-- readOnlySignal,
        value <-- stationSig.map(_.name),
        onChange.mapToValue --> { v =>
          KnownStations.find(v) match
            case Some(known) => upd(_.copy(name = v, latitude = known.latitude, longitude = known.longitude))
            case None        => upd(_.copy(name = v))
        }
      ),
      input(
        className := "cfg-input",
        typ       := "number",
        stepAttr  := "0.0001",
        placeholder := "Breitengrad",
        disabled <-- readOnlySignal,
        value <-- stationSig.map(_.latitude.toString),
        onChange.mapToValue --> { v => upd(_.copy(latitude = v.toDoubleOption.getOrElse(0.0))) }
      ),
      input(
        className := "cfg-input",
        typ       := "number",
        stepAttr  := "0.0001",
        placeholder := "Längengrad",
        disabled <-- readOnlySignal,
        value <-- stationSig.map(_.longitude.toString),
        onChange.mapToValue --> { v => upd(_.copy(longitude = v.toDoubleOption.getOrElse(0.0))) }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Nach oben",
        "↑",
        onClick --> { _ =>
          val aIdx = selectedAreaIdxVar.now()
          updateArea(aIdx)(a => a.copy(windStations = moveItem(a.windStations, idx, -1)))
        }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Nach unten",
        "↓",
        onClick --> { _ =>
          val aIdx = selectedAreaIdxVar.now()
          updateArea(aIdx)(a => a.copy(windStations = moveItem(a.windStations, idx, 1)))
        }
      ),
      button(
        className := "cfg-icon-btn",
        disabled <-- readOnlySignal,
        title := "Station entfernen",
        "✕",
        onClick --> { _ =>
          val aIdx = selectedAreaIdxVar.now()
          updateArea(aIdx)(a => a.copy(windStations = a.windStations.patch(idx, Nil, 1)))
        }
      )
    )

  private def addWindStationRow(): HtmlElement =
    div(
      className := "cfg-add-row",
      button(
        className := "cfg-btn",
        disabled <-- readOnlySignal,
        "+ Station hinzufügen",
        onClick --> { _ =>
          val aIdx = selectedAreaIdxVar.now()
          updateArea(aIdx)(a => a.copy(windStations = a.windStations :+ ConfigStation("Neue Station", 0.0, 0.0)))
        }
      )
    )

  private def diffsList(): HtmlElement =
    div(
      className := "cfg-diff-list",
      children <-- draftVar.signal
        .combineWith(selectedAreaIdxVar.signal)
        .map { case (cfg, aIdx) => cfg.areas.lift(aIdx).map(_.diffs.zipWithIndex).getOrElse(Nil) }
        .split(_._2) { (idx, _, sig) => diffRow(idx, sig.map(_._1)) }
    )

  private def diffRow(idx: Int, diffSig: Signal[ConfigDiff]): HtmlElement =
    def upd(f: ConfigDiff => ConfigDiff): Unit =
      val aIdx = selectedAreaIdxVar.now()
      updateArea(aIdx)(a => a.copy(diffs = a.diffs.zipWithIndex.map((d, i) => if i == idx then f(d) else d)))

    def stationFields(labelText: String, stationOf: ConfigDiff => ConfigStation, setStation: (ConfigDiff, ConfigStation) => ConfigDiff): HtmlElement =
      div(
        className := "cfg-diff-station",
        span(className := "cfg-diff-station-label", labelText),
        input(
          className := "cfg-input",
          typ       := "text",
          placeholder := s"$labelText Name",
          listId    := "known-stations",
          disabled <-- readOnlySignal,
          value <-- diffSig.map(d => stationOf(d).name),
          onChange.mapToValue --> { v =>
            KnownStations.find(v) match
              case Some(known) => upd(d => setStation(d, ConfigStation(v, known.latitude, known.longitude)))
              case None        => upd(d => setStation(d, stationOf(d).copy(name = v)))
          }
        ),
        input(
          className := "cfg-input",
          typ       := "number",
          stepAttr  := "0.0001",
          placeholder := "Breitengrad",
          disabled <-- readOnlySignal,
          value <-- diffSig.map(d => stationOf(d).latitude.toString),
          onChange.mapToValue --> { v => upd(d => setStation(d, stationOf(d).copy(latitude = v.toDoubleOption.getOrElse(0.0)))) }
        ),
        input(
          className := "cfg-input",
          typ       := "number",
          stepAttr  := "0.0001",
          placeholder := "Längengrad",
          disabled <-- readOnlySignal,
          value <-- diffSig.map(d => stationOf(d).longitude.toString),
          onChange.mapToValue --> { v => upd(d => setStation(d, stationOf(d).copy(longitude = v.toDoubleOption.getOrElse(0.0)))) }
        )
      )

    div(
      className := "cfg-diff-row",
      stationFields("1", _.station1, (d, s) => d.copy(station1 = s)),
      stationFields("2", _.station2, (d, s) => d.copy(station2 = s)),
      div(
        className := "cfg-diff-bottom",
        div(
          className := "cfg-field cfg-diff-color",
          label(className := "cfg-field-label", "Farbe"),
          input(
            className := "cfg-input",
            typ       := "text",
            placeholder := "red / blue / #cc0000",
            disabled <-- readOnlySignal,
            value <-- diffSig.map(_.color),
            onChange.mapToValue --> { v => upd(_.copy(color = v)) }
          )
        ),
        div(
          className := "cfg-diff-actions",
          button(
            className := "cfg-icon-btn",
            disabled <-- readOnlySignal,
            title := "Nach oben",
            "↑",
            onClick --> { _ =>
              val aIdx = selectedAreaIdxVar.now()
              updateArea(aIdx)(a => a.copy(diffs = moveItem(a.diffs, idx, -1)))
            }
          ),
          button(
            className := "cfg-icon-btn",
            disabled <-- readOnlySignal,
            title := "Nach unten",
            "↓",
            onClick --> { _ =>
              val aIdx = selectedAreaIdxVar.now()
              updateArea(aIdx)(a => a.copy(diffs = moveItem(a.diffs, idx, 1)))
            }
          ),
          button(
            className := "cfg-icon-btn",
            disabled <-- readOnlySignal,
            title := "Differenz entfernen",
            "✕",
            onClick --> { _ =>
              val aIdx = selectedAreaIdxVar.now()
              updateArea(aIdx)(a => a.copy(diffs = a.diffs.patch(idx, Nil, 1)))
            }
          )
        )
      )
    )

  private def addDiffRow(): HtmlElement =
    div(
      className := "cfg-add-row",
      button(
        className := "cfg-btn",
        disabled <-- readOnlySignal,
        "+ Differenz hinzufügen",
        onClick --> { _ =>
          val aIdx = selectedAreaIdxVar.now()
          val blank = ConfigStation("", 0.0, 0.0)
          updateArea(aIdx)(a => a.copy(diffs = a.diffs :+ ConfigDiff(blank, blank, "red")))
        }
      )
    )

  // ── JSON panel ───────────────────────────────────────────────────────────

  private def jsonPanel(): HtmlElement =
    div(
      className := "cfg-json",
      textArea(
        className := "cfg-json-area",
        readOnly <-- readOnlySignal,
        value <-- jsonTextVar,
        onInput.mapToValue --> jsonTextVar
      ),
      button(
        className := "cfg-btn",
        disabled <-- readOnlySignal,
        "JSON übernehmen",
        onClick --> { _ =>
          decode[WeatherConfig](jsonTextVar.now()) match
            case Right(cfg) =>
              draftVar.set(cfg)
              selectedAreaIdxVar.set(0)
              jsonModeVar.set(false)
              setMessage("JSON übernommen — mit Speichern bestätigen.", false)
            case Left(error) =>
              setMessage(s"Ungültiges JSON: ${error.getMessage}", true)
        }
      )
    )

  private def footerRow(): HtmlElement =
    div(
      className := "cfg-footer",
      div(
        className := "cfg-message",
        child <-- statusVar.signal.map:
          case None => emptyNode
          case Some((text, isError)) =>
            span(className := (if isError then "cfg-msg error" else "cfg-msg ok"), text)
      ),
      div(
        className := "cfg-footer-actions",
        button(
          className := "cfg-btn",
          disabled <-- readOnlySignal,
          "Speichern",
          onClick --> { _ => trySave() }
        ),
        button(
          className := "cfg-btn primary",
          "Aktivieren",
          title := "Speichern und die App auf diese Konfiguration umschalten",
          onClick --> { _ => activateSelected() }
        )
      )
    )

end ConfigEditorDialog
