# Experiment.

@Performance @Tag2
Feature: Experiment
  As a load testing client of the FHIR API
  In order to test FHIR
  I want to perform a particular query once

Scenario: Simply make an HTTP request
  Given that we have a headless browser available
  When a headless client requests
  Then a successful response is returned
