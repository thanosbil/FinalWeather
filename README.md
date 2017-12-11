# FinalWeather
User’s location is determined using Google’s API FusedLocationProviderClient.
Then, using the location data, a remote call to OpenWeatherMap API fetches the
weather information in JSON format. This process takes place in a background
thread using an AsyncTaskLoader. Finally, the weather information is parsed and
displayed in the UI.
