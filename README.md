lucene-plugin
=================

Lucene plugin for indexing and searching files stored in Baratine distributed filesystem

To run the plugin:

1. install maven baratine plugins https://github.com/baratine/maven-collection-baratine
  1. git clone git@github.com:baratine/maven-collection-baratine.git
  2. change to maven-collection-baratine and run mnv install
2. execute mvn -Dmaven.test.skip=true -P release package
3. open lucene-plugin/client/src/web/index.html in latest browser
