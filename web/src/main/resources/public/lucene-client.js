/**
 * construct LuceneClient using url of the deployed lucene service: e.g. 'ws://localhost:8080/lucene'
 * @param url
 * @constructor
 */
LuceneClient = function (url)
{
  this.url = url;
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
  var request = new XMLHttpRequest();
  request.open("POST", this.url + "/index-text");
  request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
  var data = "collection="
             + collection
             + "&id="
             + extId
             + "&text="
             + encodeURIComponent(text);
  request.send(data);
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
  var request = new XMLHttpRequest();
  request.open("POST", this.url + "/index-map");
  request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
  var data = "collection=" + collection + "&id=" + extId;
  for (var property in map) {
    if (map.hasOwnProperty(property)) {
      data = data + "&" + property + "=" + encodeURIComponent(map[property]);
    }
  }

  console.log(data);

  request.send(data);
}

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
  request = new XMLHttpRequest();

  data = "collection=" + collection + "&query=" + query + "&limit=" + limit;

  request.onreadystatechange = function ()
  {
    if (this.readyState === XMLHttpRequest.DONE
        && this.status === 200) {
      console.log(this.responseText);

      var results = JSON.parse(this.responseText);

      var ids = new Array();

      console.log(results);
      console.log(results.length);

      for (var i = 0; i < results.length; i++) {
        ids.push(results[i]._externalId);
      }

      callback(ids);
    }
  };

  request.open("GET", this.url + "/search?" + data);

  request.send();
}

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
LuceneClient.prototype.close = function ()
{
  this.client.close();
};
