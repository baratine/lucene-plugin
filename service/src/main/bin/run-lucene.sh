#!/bin/bash

if [ -z $BARATINE_HOME ]; then
  BARATINE_HOME=~/baratine
fi;

if [ ! -f $BARATINE_HOME/lib/baratine.jar ]; then
  echo "BARATINE_HOME '$BARATINE_HOME' does not point to a baratine installation";
  exit 1;
fi;

echo "baratine home is set to $BARATINE_HOME";

BARATINE_DATA_DIR=/tmp/baratine
BARATINE_CONF=src/main/bin/conf.cf
BARATINE_ARGS="--data-dir $BARATINE_DATA_DIR --conf $BARATINE_CONF"

$BARATINE_HOME/bin/baratine shutdown $BARATINE_ARGS

rm -rf $BARATINE_DATA_DIR

cd ..

mvn -Dmaven.test.skip=true -Dbaratine.run.skip=true -P local clean package

cd service

cp  target/lucene-*.bar lucene-plugin.bar

exit;

$BARATINE_HOME/bin/baratine start $BARATINE_ARGS --deploy lucene-plugin.bar

echo "index ..."
$BARATINE_HOME/bin/baratine jamp-query $BARATINE_ARGS --pod lucene /service indexText foo bar mary

echo "search ..."
$BARATINE_HOME/bin/baratine jamp-query $BARATINE_ARGS --pod lucene /service search foo lamb 5

$BARATINE_HOME/bin/baratine cat $BARATINE_ARGS /proc/services

