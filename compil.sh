#!/bin/bash
mvn clean install -DskipTests
java -cp target/cloudsimplus-8.5.0-with-dependencies.jar org.cloudsimplus.examples.BasicFirstExample
