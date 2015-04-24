/**
 * construct LuceneClient using url of the deployed lucene service: e.g. 'ws://localhost:8085/s/lucene'
 * @param url
 * @constructor
 */
LuceneClient = function (url)
{
  this.client = new Jamp.BaratineClient(url);
};

/**
 * add text to the index in a specified collection
 *
 * e.g.
 *
 * indexText('my-collection', 'my-id', 'mary had a little lamb');
 *
 * search('my-collection', 'mary', 255); //fetches {'my-id'}
 *
 * @param collection - name of the collection
 * @param extId - id, which will be returned for searches that match the text
 * @param text - text to add to the index (not stored)
 */
LuceneClient.prototype.indexText = function (collection, extId, text)
{
  this.client.send("/lucene", "indexText", [collection, extId, text]);
};

/**
 * add map to the index in a specified colleciotn
 *
 * e.g.
 *
 * var map = {};
 * map[text] = 'mary had a little lamb'
 *
 * indexMap('my-collection', 'my-id', map);
 *
 * search('my-collection', 'text:mary', 255); // fetches {'my-id'}
 *
 * @param collection - name of the collection
 * @param extId - id, which will be returned for searches that match the map
 * @param map
 */
LuceneClient.prototype.indexMap = function (collection, extId, map)
{
  this.client.send("/lucene", "indexMap", [collection, extId, map]);
};

/**
 * search lucene index inside the collection using specified query
 *
 * e.g.
 *
 * search('my-collection', 'mary', 255, function(ids) {
 *   console.log(ids.join());
 * });
 *
 * @param collection - name of the collection
 * @param query - query to send to lucene
 * @param limit - number of ids returned
 * @param callback - method to call when results are ready to be processed
 */
LuceneClient.prototype.search = function (collection, query, limit, callback)
{
  var onSearch = function (results)
  {
    var ids = new Array();
    for (var i = 0; i < results.length; i++) {
      var result = results[i];

      ids.push(result._externalId);
    }

    if (callback) {
      callback(ids);
    }
  };

  onSearch.onfail = function (error)
  {
    console.log(error);
  };

  this.client.query("/lucene",
                    "search",
    [collection, query, limit],
                    onSearch);
};

/**
 * clears collection
 * @param collection - collection to clear
 * @param callback - callback on success
 */
LuceneClient.prototype.clear = function (collection, callback)
{
  this.client.query("/lucene", "clear", [collection], callback);
};

/**
 * close underlying connections
 */
LuceneClient.prototype.close = function() {
  this.client.close();
};
