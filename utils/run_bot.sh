#!/bin/bash

mvn install -DskipTests

libs=$(find target/lib -name '*.jar' | xargs echo | tr ' ' ':')
projects=$(find crypto-bot* -name '*.jar' | xargs echo | tr ' ' ':')

classpath="$libs:$projects"

java -cp $classpath org.achfrag.crypto.bot.DonchianBot
