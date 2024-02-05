# jetty-memory-leak

A demo application reproducing a memory leak in the Jetty 12 HTTP client that occurs when a server times out.
The memory leak is caused by the internal scheduler queue growing infinitely.

### Jetty 11 (no memory leak)
To run the app that uses Jetty 11, execute:

`./gradlew :jetty11:run`

You should see logs like: 

```
Queue size: 0
Queue size: 0
Queue size: 0
Queue size: 0
Queue size: 0
```

### Jetty 12 (memory leak)

To run the app that uses Jetty 12, execute:

`./gradlew :jetty12:run`

You should see logs like:

```
Queue size: 0
Queue size: 1
Queue size: 2
Queue size: 3
Queue size: 4
```
