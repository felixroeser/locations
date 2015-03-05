#!/usr/bin/env ruby

require 'json'
require 'httparty'
require 'zlib'

port = (ARGV[0] || 9000).to_i
host = ARGV[1] || 'localhost'
APIKEY = ARGV[2] || 'api'
puts "Going to use #{host}:#{port}"

%w(gkh saturn).each do |filename|
  # See http://www.pocketnavigation.de/poidownload/pocketnavigation/de/?device-format-id=4&country=DE#selection-step2
  # for the original csv
  Zlib::GzipReader.open("#{filename}.json.gz") do |gz|
    parsed = JSON.parse gz.read

    puts "Posting #{parsed.size} locations to #{host}:#{port}"

    parsed.each do |h|
      h.tap { h[:ownerId] = filename }
      response = HTTParty.post("http://api:#{APIKEY}@#{host}:#{port}/locations",
        { body: h.tap { |h| h[:ownerId] = filename }.to_json,
          headers: { 'Content-Type' => 'application/json', 'Accept' => 'application/vnd.locations.v1+json'}
      })
      puts [response.code, response.body] unless response.code == 202
    end

  end
end
