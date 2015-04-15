#!/bin/bash

if [ -z $BARATINE_HOME ]; then
  BARATINE_HOME=~/baratine
fi;

if [ ! -f $BARATINE_HOME/lib/baratine.jar ]; then
  echo "BARATINE_HOME '$BARATINE_HOME' does not point to a baratine installation";
  exit 1;
fi;

BARATINE_DATA_DIR=/tmp/baratine
BARATINE_CONF=src/main/bin/conf.cf
BARATINE_ARGS="--data-dir $BARATINE_DATA_DIR --conf $BARATINE_CONF"

$BARATINE_HOME/bin/baratine shutdown $BARATINE_ARGS

rm -rf $BARATINE_DATA_DIR

cd ..

mvn -Dmaven.test.skip=true -P local clean package

cd service

cp  target/lucene-*.bar lucene.bar

$BARATINE_HOME/bin/baratine start $BARATINE_ARGS --deploy lucene.bar

echo "index ..."
curl 'http://localhost:8085/s/lucene/lucene/foo?m=indexText&p0=foo&p1=bar&p2=lamb';

echo "search ..."
curl 'http://localhost:8085/s/lucene/lucene/foo?m=search&p0=foo&p1=lamb&p2=5';


echo "search ..."

$BARATINE_HOME/bin/baratine cat $BARATINE_ARGS /proc/services

