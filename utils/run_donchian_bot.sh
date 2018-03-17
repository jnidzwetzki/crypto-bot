#!/bin/bash

if [ -d target/lib/ ]; then
   rm -r target/lib/
fi

mvn install -DskipTests

libs=$(find target/lib -name '*.jar' | xargs echo | tr ' ' ':')
projects=$(find crypto-bot* -name '*.jar' | xargs echo | tr ' ' ':')

classpath="$libs:$projects"

java -cp $classpath com.github.jnidzwetzki.cryptobot.DonchianBot $@
