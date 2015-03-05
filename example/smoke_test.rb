#!/usr/bin/env ruby

# Not a really test replacement:
# * there will be leftovers in your database if something fails!

require 'json'
require 'httparty'
require 'zlib'
require 'securerandom'
require 'timeout'
require 'colorize'
require 'rspec/expectations'

PORT = (ARGV[0] || 9000).to_i
HOST = ARGV[1] || 'localhost'
APIKEY = ARGV[2] || 'api'
puts "Going to use #{HOST}:#{PORT}".colorize(:yellow)

class LocationsApi
  include HTTParty
  base_uri "http://api:#{APIKEY}@#{HOST}:#{PORT}"
  headers 'Accept' => 'application/vnd.locations.v1+json'

  def initialize
  end

  def all
    response = self.class.get("/locations")
    response.code == 200 ? JSON.parse(response.body) : nil
  end

  def show(id)
    response = self.class.get("/locations/#{id}")
    response.code == 200 ? JSON.parse(response.body) : nil
  end

  def create(payload)
    response = self.class.post("/locations",
      { body: payload.to_json,
        headers: { 'Content-Type' => 'application/json'}
    })
    response.code == 202 ? true : nil
  end

  def search(query)
    response = self.class.post("/locations/search", {
      body: query.to_json,
      headers: { 'Content-Type' => 'application/json'}
    })
    response.code == 200 ? JSON.parse(response.body) : nil
  end

  def upsert_to_databag(id, h)
    response = self.class.post("/locations/#{id}/databag",
      body: h.to_json,
      headers: { 'Content-Type' => 'application/json'}
    )
    response.code == 202 ? true : nil
  end

  def delete_from_databag(id, k)
    response = self.class.delete("/locations/#{id}/databag/#{k}")
    response.code == 202 ? true : nil
  end

  def delete(id)
    response = self.class.delete("/locations/#{id}")
    response.code == 202 ? true : nil
  end

end

class SmokeTest
  include RSpec::Matchers

  def initialize
    @api = LocationsApi.new
  end

  def run

    action "requesting locations" do
      locations = @api.all
      expect(locations).to be_kind_of(Array)
    end

    id = SecureRandom.uuid
    owner_id = SecureRandom.uuid
    action "creating new location with id #{id}" do
      payload = {
        id: id,
        ownerId: owner_id,
        address: {
          city: 'cologne',
          state: 'nrw',
          zipcode: '50676',
          country: 'de',
          lat: 50.934009,
          long: 6.952221,
          street: "Leonhard-Tietz-Strasse 1",
        },
        databag: {
          status: "whatever",
        }
      }

      success = @api.create(payload)
      expect(success).to eq true
    end

    action "request location #{id}" do
      location = nil
      Timeout::timeout(5) do
        while !location do
          sleep 0.1
          location = @api.show(id)
        end
      end

      expect(location).to_not be_nil
      expect(location['id']).to eq id
      expect(location['databag']['status']).to eq 'whatever'
    end

    action "requesting locations should contain #{id}" do
      locations = @api.all
      expect(locations).to be_kind_of(Array)
      expect(locations.map { |l| l['id']} ).to include(id)
    end

    action "requesting locations owned by #{owner_id} should contain #{id}" do
      q = {ownerId: owner_id}
      locations = @api.search(q)
      expect(locations.map { |l| l['id']} ).to eq [id]
    end

    action "a 1km geo search should return id" do
      q = {ownerId: owner_id, lat: 50.936362, long: 6.947658, maxDistance: 1 }
      locations = @api.search(q)
      expect(locations.map { |l| l['id']} ).to eq [id]
    end

    action "but a 0.1km geo search should not return id" do
      q = {ownerId: owner_id, lat: 50.936362, long: 6.947658, maxDistance: 0.1 }
      locations = @api.search(q)
      expect(locations.map { |l| l['id']} ).to eq []
    end

    action "modifying the databag" do
      payload = {foo: 'bar'}
      success = @api.upsert_to_databag(id, payload)
      expect(success).to eq true

      success = @api.delete_from_databag(id, :status)
      expect(success).to eq true

      Timeout::timeout(10) do
        loop do
          sleep 0.1
          location = @api.show(id)
          break if location['databag']['foo'] == 'bar' && location['databag']['status'].nil?
        end
      end
    end

    action "deleting location #{id}" do
      success = @api.delete(id)
      expect(success).to eq true

      action "request location should 404" do
        Timeout::timeout(10) do
          loop do
            sleep 0.1
            break if @api.show(id).nil?
          end
        end
      end
    end

  end

  def action(text, &block)
    puts "* #{text}".colorize(:green)
    block.call if block
    puts "  => done".colorize(:green)
  end
end

SmokeTest.new.run
