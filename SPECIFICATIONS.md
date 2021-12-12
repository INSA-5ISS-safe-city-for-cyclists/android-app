# Specifications

## Map display

[x] Display a map
[x] Display bicycle paths
[x] Show danger reports
[x] Show danger reports in clusters when zooming out
[x] Display user gps position
[x] Allow viewport to follow user position on button click

## Routing

[x] Allow creating a bike route between 2 points
[x] Detect if route goes near a danger
[] Avoid dangerous zones if possible
[x] Fit route in viewport when calculated
[x] Show route summary when calculated (time, distance, number of dangers, start button)

## Waypoints

[x] Show a pin on selected coordinates
[x] Show flag icon for destination
[x] Allow searching for a place from a search bar
[x] Select a place from search results
[x] Simple click to select a poi, long click to select coordinates anywhere
[x] Use current position as start if available when choosing destination
[x] On click show a modal to ask "route from" or "route to"

## Navigation mode

[] Enter navigation mode if route from gps position
[] Follow gps direction in navigation
[] Hide line after gps
[] Hide search bar on navigation
[] Show turn instructions when navigating
[] Press back or a button to exit navigation
[] Show confirmation when exiting navigation
[] Dynamic rerouting when going out of the route

## ESP connection

[x] Connect to the ESP in BLE
[x] Receive object detection messages in BLE from ESP
[] If gps active, store reports in database
[] If gps not active, discard reports
[x] Update map when updating the database
[] Warning pop-up when connecting BLE but GPS inactive

## Local reports

[x] Fetch classification criteria from server on start
[x] Use fetched classification to display relevant reports on the map
[x] Show local reports in a different color from others
[x] Show button to upload reports
[x] Show recap modal with the list of reports to upload
[x] Clicking on a report in the list will lower the modal and select the report on the map
[x] Allow removing selected local report
[] Remove old non-dangerous or uploaded reports (2 weeks) on start


