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
[] Fit route in viewport when calculated
[] Show route summary when calculated (time, distance, number of dangers, start button)

## Waypoints

[x] Show a pin on selected coordinates
[x] Show flag icon for destination
[] Allow searching for a place from a search bar
[] Select a place from search results
[] Simple click to select a poi, long click to select coordinates anywhere
[] Use current position as start if available when choosing destination
[] On click show a modal to ask "route from" or "route to"

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

[] Connect to the ESP in BLE
[] Receive object detection messages in BLE from ESP
[] If gps active, store reports in database
[] If gps not active, discard reports
[] Update map when updating the database
[] Warning pop-up when connecting BLE but GPS inactive

## Local reports

[] Fetch classification criteria from server on start
[] Use fetched classification to display relevant reports on the map
[] Show local reports in a different color from others
[] Show button to upload reports
[] Show recap modal with the list of reports to upload
[] Clicking on a report in the list will lower the modal and select the report on the map
[] Allow removing selected local report
[] Clear local database on upload


