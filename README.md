# running-app-detection

Master's Thesis Project.

The final objetive is to be able to detect which app is currently running in the foreground (Android), based on tap pattern analysis.

The first step, will be to analyze in which way we can get data from a background application.
Currently, I believe I could use a Service which will constantly log sensors (accelerometer, graviton an so on).
I will start by developing a simple app that will allow me to see the relationship between different sensors and screen touch (no gestures or swipes).
