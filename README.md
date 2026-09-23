# RuneRadar

RuneRadar is a RuneLite plugin that helps players find and track Grand Exchange flipping opportunities.

## Features

- Grand Exchange flipping recommendations
- Multiple trading modes:
    - Fast
    - Balanced
    - Slow
    - High Profit
    - All
- Strong and More Opportunities filters
- Cash stack based recommendation scaling
- Net profit calculations after Grand Exchange tax
- ROI, break-even and quantity details
- Active Flip tracking
- Automatic detection of Grand Exchange buys and sells
- Partial buy and sell support
- Multiple simultaneous flips
- Persistent Active Flips between RuneLite restarts
- Automatic completed-flip tracking
- Profit Tracker with session, daily and all-time profit

## How RuneRadar works

RuneRadar retrieves market-analysis results from the RuneRadar hosted API and displays them inside RuneLite.

RuneRadar does not perform clicks, trades, inputs or other automated gameplay actions. All Grand Exchange actions are performed manually by the player.

## Third-party server communication

RuneRadar communicates with the RuneRadar API hosted outside RuneLite.

The plugin sends information required to generate Grand Exchange recommendations, such as recommendation settings and the configured cash stack.

RuneRadar does not send account passwords or automate gameplay.

Hosted API:

https://runeradar-production.up.railway.app

## Development

RuneRadar requires Java 11.

To run the development client:

.\gradlew run

## Disclaimer

Grand Exchange prices can change quickly. RuneRadar recommendations are estimates based on available market data and do not guarantee profit.