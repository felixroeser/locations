## locations

a demo akka based microservice demo with (buzzword alarm) cqrs and event sourcing

## Event storage

Pick your poison

* Build in:
  * [LevelDB](https://github.com/google/leveldb) for development - events will be stored in target/example
* External
  * [In memory](https://github.com/michaelpisula/akka-journal-inmemory) - for tests  
  * [Cassandra](https://github.com/krasserm/akka-persistence-cassandra/)
    * TBA - working on it  
  * [MongoDB](https://github.com/ironfish/akka-persistence-mongo)
    * Provide either MONGO_URL, MONGOHQ_URL or MONGOLAB_URI via the env
    * Format mongodb://HOST:PORT/DATABASE
    * Default mongodb://localhost:27017/schauspieler
    * Events will be stored in the _events_ collection and snapshots in _snapshots_
    * ```MONGO_URL=mongodb://localhost:27017/schauspieler sbt run```

### Getting started

1. flush the event/snapshot journal

    $ rm -rf target/example

2. start the application on localhost:9000 or provide PORT

    $ sbt run
    $ PORT=9001 sbt run

3. build it - how Heroku will build it

    $ sbt clean compile
    $ cd target/universal/stage
    $ ./bin/locations

### API Example

  Do some single api calls

    $ #
    $ # grab a fresh shell or open a new tmux/screen window
    $ #
    $ # post a new location
    $ curl -H "Content-Type: application/vnd.locations.v1+json" -d '{"id": "test1", "ownerId":"some_retailer", "address":{"city":"cologne", "state":"nrw", "zipcode":"50676", "country":"de", "long":6.952221, "street":"Leonhard-Tietz-Strae 1", "lat":50.934009}, "databag":{"status":"whatever"}}' http://localhost:9000/v0/locations
    $ curl http://localhost:9000/v0/locations/
    $ # add a key-value pair to the databag
    $ curl -H "Content-Type: application/vnd.locations.v1+json" -d '{"foo": "bar"}' http://localhost:9000/v0/locations/test1/databag
    $ # remove a key-value pair from the databag
    $ curl -X DELETE http://localhost:9000/v0/locations/test1/databag/status
    $ # request the location
    $ curl http://localhost:9000/v0/locations/test1
    $ # search for locations around _neumarkt cologne_
    $ curl -H "Content-Type: application/vnd.locations.v1+json" -d '{"limit" : 5, "lat": 50.936350, "long": 6.949840, "maxDistance": 2}' http://localhost:9000/v0/locations/search
    $ # delete the location
    $ curl -X DELETE http://localhost:9000/v0/locations/test1

  Load a bigger dataset

    $ cd example
    $ # ruby required from here on
    $ bundle
    $ # post 200+ gkh and saturn locations
    $ ./post.rb
    $ # request all locations - please mind the slash
    $ curl -H "Content-Type: application/vnd.locations.v1+json" http://localhost:9000/v0/locations/
    $ # request all locations by owner
    $ curl -H "Content-Type: application/vnd.locations.v1+json" -d '{"ownerId": "saturn"}' http://localhost:9000/v0/locations/search
    $ # do a distance based search with limit
    $ curl -H "Content-Type: application/vnd.locations.v1+json" -d '{"ownerId": "gkh", "limit" : 5, "lat": 11.5746, "long": 48.13718, "maxDistance": 200}' http://localhost:9000/v0/locations/search

    You can also run ./smoke_test.rb

### Tests

*Missing* But there is a small smoke test you can run against a running _locations_
instance: ```cd example && ./smoke_test.rb 9000 localhost``` This will create one new
location, retrieve it and delete it.

### Misc

#### haproxy

Run command and query on separate hosts

    frontend locations
    bind *:9050
    acl is_command method POST
    acl is_command method DELETE
    use_backend locations_ci if is_command
    default_backend locations_qi

    backend locations_qi
    balance roundrobin
    option httpchk GET /ping HTTP/1.0
    server localhost_9000 localhost:9000

    backend locations_ci
    balance roundrobin
    option httpchk GET /ping HTTP/1.0
    server localhost_9000 localhost:9000

    listen haproxyapp_admin:9100 127.0.0.1:9100
    mode http
    stats uri /

#### Heroku

Yes it works! But mind that api access is without any protection at all! Use with
caution = totally random domain name.

## Contributions

* The basic structure goes back to [sandermak](https://github.com/sandermak/akka-eventsourcing)

## License

The license is Apache 2.0
