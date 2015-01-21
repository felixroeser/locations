## locations

a demo akka based microservice demo with (buzzword alarm) cqrs and event sourcing

TBA

### Example

    $ # start _locations_ on localhost:9000
    $ sbt run
    $ cd example
    $ # ruby required from here on
    $ bundle
    $ # ./post.rb 200+ gkh and saturn locations
    $ ./post
    $ # request all locations
    $ curl -H "Content-Type: application/json" http://localhost:9000/v0/locations/
    $ # request all locations by owner
    $ curl -H "Content-Type: application/json" -d '{"ownerId": "saturn"}' http://localhost:9000/v0/locations/search
    $ # do a distance based search with limit
    $ curl -H "Content-Type: application/json" -d '{"ownerId": "gkh", "limit" : 5, "lat": 11.5746, "long": 48.13718, "maxDistance": 200}' http://localhost:9000/v0/locations/search

## Contributions

* The basic structure goes back to [sandermak](https://github.com/sandermak/akka-eventsourcing)

## License

The license is Apache 2.0
