# pme123-weather
Playing with weather data to predict wind.

## Result
You see the wind diagrams here:
https://pme123.github.io/pme123-weather/

- At the moment there is a problem with optimizing the Javascript. 
  So be aware that loading the page can take some time.

## Development

- Run `sbt ~fastLinkJS` to generate the Javascript from Scala code.
- Run `npm run dev` to start the webserver (Vite).
- Open `http://localhost:5173/` in your browser.

## Production

- Run `./helper.scala` (make sure you have the rights > `chmod +x helper.scala`).
- Commit and push the changes > this will trigger the GitHub Action to deploy the page.
