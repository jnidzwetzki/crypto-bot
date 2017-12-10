#!/bin/bash

mvn install -DskipTests
mvn -pl crypto-bot-trading exec:java

