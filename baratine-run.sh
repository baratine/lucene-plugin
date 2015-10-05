#!/bin/sh

mvn -P release clean package

cd service

mvn -P release baratine:run

