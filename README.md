## locations

a demo akka based microservice demo with (buzzword alarm) cqrs and event sourcing

### Getting started

1. flush the event/snapshot journal

    $ rm -rf target/example

2. start the application on localhost:9000

    $ sbt run

### API Example


  Do some single api calls

    $ #
    $ # grab a fresh shell or open a new tmux/screen window
    $ #
    $ # post a new location
    $ curl -H "Content-Type: application/json" -d '{"id": "test1", "ownerId":"some_retailer", "address":{"city":"cologne", "state":"nrw", "zipcode":"50676", "country":"de", "long":6.952221, "street":"Leonhard-Tietz-Strae 1", "lat":50.934009}, "databag":{"status":"whatever"}}' http://localhost:9000/v0/locations
    $ curl http://localhost:9000/v0/locations/
    $ # add a key-value pair to the databag
    $ curl -H "Content-Type: application/json" -d '{"foo": "bar"}' http://localhost:9000/v0/locations/test1/databag
    $ # remove a key-value pair from the databag
    $ curl -X DELETE http://localhost:9000/v0/locations/test1/databag/status
    $ # request the location
    $ curl http://localhost:9000/v0/locations/test1
    $ # search for locations around _neumarkt cologne_
    $ curl -H "Content-Type: application/json" -d '{"limit" : 5, "lat": 50.936350, "long": 6.949840, "maxDistance": 2}' http://localhost:9000/v0/locations/search
    $ # delete the location
    $ curl -X DELETE http://localhost:9000/v0/locations/test1

  Load a bigger dataset

    $ cd example
    $ # ruby required from here on
    $ bundle
    $ # post 200+ gkh and saturn locations
    $ ./post.rb
    $ # request all locations - please mind the slash
    $ curl -H "Content-Type: application/json" http://localhost:9000/v0/locations/
    $ # request all locations by owner
    $ curl -H "Content-Type: application/json" -d '{"ownerId": "saturn"}' http://localhost:9000/v0/locations/search
    $ # do a distance based search with limit
    $ curl -H "Content-Type: application/json" -d '{"ownerId": "gkh", "limit" : 5, "lat": 11.5746, "long": 48.13718, "maxDistance": 200}' http://localhost:9000/v0/locations/search

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

## Contributions

* The basic structure goes back to [sandermak](https://github.com/sandermak/akka-eventsourcing)

## License

The license is Apache 2.0
