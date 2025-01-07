package pme123.weather

import be.doeraene.webcomponents.ui5.CheckBox
import com.raquo.laminar.api.L.*

object StationsCheckboxGroup:
  def apply(selectedOptionsVar: Var[Seq[String]]): HtmlElement =
    val options = selectedOptionsVar.now()
    div(
      className := "stations-checkbox-group",
      options.toSeq.map: option =>
        val isChecked = selectedOptionsVar.signal.map(_.contains(option))
        CheckBox(
          className := "checkbox",
          _.text := option,
          _.checked <-- isChecked,
          onChange.mapTo(option) --> { selectedOption =>
            selectedOptionsVar.update { selected =>
              if selected.contains(selectedOption) then
                selected.filterNot(_ == selectedOption)
              else selected :+ selectedOption
            }
            println(s"selectedOption: $option ${selectedOptionsVar.now().contains(option)}")
          }
        )
    )
  end apply
end StationsCheckboxGroup
