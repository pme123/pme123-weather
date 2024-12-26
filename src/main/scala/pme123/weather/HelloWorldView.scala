package pme123.weather

import com.raquo.laminar.api.L.{*, given}

object HelloWorldView {

  def apply(): HtmlElement = {
    div(
      renderExample()
    )
  }

  def renderExample(): HtmlElement = {
    // #Exercise for the reader:
    // What will change if we move nameVar to be
    // a member of object HelloWorldView
    // (outside of the `renderExample` method) and why?
    // HINT: htrof dna kcab gnitagivan yrt

    // BEGIN[hello world]
    val nameVar = Var(initial = "world")
    div(
      label("Your name: "),
      input(
        placeholder := "Enter your name here",
        onInput.mapToValue --> nameVar
      ),
      p(
        "Hello, ",
        text <-- nameVar.signal.map(_.toUpperCase)
      )
    )
    // END[hello world]
  }
}
