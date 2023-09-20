#!/bin/bash
mvn clean install -DskipTests
java -cp target/cloudsimplus-*-with-dependencies.jar org.cloudsimplus.examples.BasicFirstExample
java -cp target/cloudsimplus-*-with-dependencies.jar org.cloudsimplus.examples.CloudFactoryGeneratedWorkload 1 64 256 1.0
