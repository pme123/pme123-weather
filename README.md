# pme123-weather

A sophisticated weather analysis application built with Scala.js that helps predict wind conditions by analyzing pressure differences between weather stations. The app provides interactive visualizations of weather data across multiple regions in Switzerland and surrounding areas.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Features

- **Real-time Weather Data**: Fetches current weather data from OpenMeteo API
- **Interactive Visualizations**: 
  - Pressure difference graphs between weather stations
  - Wind speed and direction analysis
  - Temperature and pressure trends
- **Multiple Regions**: Coverage of various Swiss regions including:
  - Urnersee
  - Mittelland
  - Comersee
  - Gardasee
  - Hyeres
- **Customizable Views**: 
  - Selectable weather stations for comparison
  - Adjustable thresholds for wind prediction
  - Interactive graph controls

## Technical Stack

- **Frontend**: Scala.js with Laminar for reactive UI
- **UI Components**: UI5 Web Components
- **Data Visualization**: Plotly.js
- **API Integration**: OpenMeteo REST APIs
- **Build Tools**: SBT and Vite

## Live Demo

Visit the live application at: https://pme123.github.io/pme123-weather/

Note: At the moment there is a problem with optimizing the Javascript. 
So be aware that loading the page can take some time.

## Development

- Run `sbt ~fastLinkJS` to generate the Javascript from Scala code.
- Run `npm run dev` to start the webserver (Vite).
- Open `http://localhost:5173/` in your browser.

## Production

- Run `./helper.scala` (make sure you have the rights > `chmod +x helper.scala`).
- Commit and push the changes > this will trigger the GitHub Action to deploy the page.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
