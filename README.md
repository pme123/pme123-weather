# pme123-weather
Playing with weather data to predict wind.

## Result
You see the wind diagrams here:
https://pme123.github.io/pme123-weather/

- At the moment there is a problem with optimizing the Javascript. 
  So be aware that loading the page can take some time.

## Development

- Run `sbt ~fastOptJS` to start the development server.
- Open `index-dev.html` in your browser.

## Production

- Run `./helper.scala` (make sure you have the rights > `chmod +x helper.scala`).
- Commit and push the changes > this will trigger the GitHub Action to deploy the page.
