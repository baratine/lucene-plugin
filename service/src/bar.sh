#!/bin/sh
#

m2=~/.m2
m2lucene=$m2/repository/org/apache/lucene
lucene_ver=5.0.0

lucene_core=$m2lucene/lucene-core/$lucene_ver/lucene-core-$lucene_ver.jar
lucene_analyze=$m2lucene/lucene-analyzers-common/$lucene_ver/lucene-analyzers-common-$lucene_ver.jar
lucene_query=$m2lucene/lucene-queryparser/$lucene_ver/lucene-queryparser-$lucene_ver.jar

m2tika=$m2/repository/org/apache/tika
