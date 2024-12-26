package pme123.weather

import com.raquo.laminar.api.L.*


object StationsCheckboxGroup:
  def apply(selectedOptionsVar: Var[Set[String]]): HtmlElement =
    val options = selectedOptionsVar.now()
    div(
      options.toSeq.map: option =>
        val isChecked = selectedOptionsVar.signal.map(_.contains(option))
        div(
          input(
            typ := "checkbox",
            checked <-- isChecked,
            onChange.mapTo(option) --> { selectedOption =>
              selectedOptionsVar.update { selected =>
                if selected.contains(selectedOption) then selected - selectedOption
                else selected + selectedOption
              }
              println(s"selectedOption: $option ${selectedOptionsVar.now().contains(option)}")
            }
          ),
          label(option)
        )
    )
  end apply
end StationsCheckboxGroup
