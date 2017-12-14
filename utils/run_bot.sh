#!/bin/bash

mvn install -DskipTests

libs=$(find target/lib -name '*.jar' | xargs echo | tr ' ' ':')
classpath="$libs"

java -cp $classpath org.achfrag.crypto.bot.DonchianBot
