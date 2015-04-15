var Jamp = {};

Jamp.unserialize = function (json)
{
  var array = JSON.parse(json);

  return Jamp.unserializeArray(array);
};

Jamp.unserializeArray = function (array)
{
  var type = array[0];

  switch (type) {
  case "reply":
    if (array.length < 5) {
      throw new Error("incomplete message for JAMP type: " + type);
    }

    var headerMap = array[1];
    var fromAddress = array[2];
    var queryId = array[3];
    var result = array[4];

    var msg = new Jamp.ReplyMessage(headerMap, fromAddress, queryId, result);

    return msg;

  case "error":
    if (array.length < 5) {
      throw new Error("incomplete message for JAMP type: " + type);
    }

    var headerMap = array[1];
    var toAddress = array[2];
    var queryId = array[3];
    var result = array[4];

    if (array.length > 5) {
      var resultArray = new Array();

      for (var i = 4; i < array.length; i++) {
        resultArray.push(array[i]);
      }

      result = resultArray;
    }

    var msg = new Jamp.ErrorMessage(headerMap, toAddress, queryId, result);

    return msg;

  case "query":
    if (array.length < 6) {
      throw new Error("incomplete message for JAMP type: " + type);
    }

    var headerMap = array[1];
    var fromAddress = array[2];
    var queryId = array[3];
    var toAddress = array[4];
    var methodName = array[5];

    var args = null;

    if (array.length > 6) {
      args = new Array();

      for (var i = 6; i < array.length; i++) {
        args.push(array[i]);
      }
    }

    var msg = new Jamp.QueryMessage(headerMap,
                                    fromAddress,
                                    queryId,
                                    toAddress,
                                    methodName,
                                    args);

    return msg;

  case "send":
    if (array.length < 4) {
      throw new Error("incomplete message for JAMP type: " + type);
    }

    var headerMap = array[1];
    var toAddress = array[2];
    var methodName = array[3];

    var parameters = null;

    if (array.length > 4) {
      parameters = new Array();

      for (var i = 4; i < array.length; i++) {
        parameters.push(array[i]);
      }
    }

    var msg = new Jamp.SendMessage(headerMap,
                                   toAddress,
                                   methodName,
                                   parameters);

    return msg;

  default:
    throw new Error("unknown JAMP type: " + type);
  }
};

Jamp.Message = function (headerMap)
{
  if (headerMap == null) {
    headerMap = {};
  }

  this.headerMap = headerMap;

  this.serialize = function ()
  {
    var array = new Array();

    this.serializeImpl(array);

    var json = JSON.stringify(array);

    return json;
  };

  this.serializeImpl;
};

Jamp.SendMessage = function (headerMap, toAddress, methodName, parameters)
{
  Jamp.Message.call(this, headerMap);

  this.toAddress = toAddress;
  this.methodName = methodName;

  this.parameters = parameters;

  this.serializeImpl = function (array)
  {
    array.push("send");
    array.push(this.headerMap);
    array.push(this.toAddress);
    array.push(this.methodName);

    if (this.parameters != null) {
      for (var i = 0; i < parameters.length; i++) {
        array.push(this.parameters[i]);
      }
    }
  };
};

Jamp.QueryMessage = function (headerMap,
                              fromAddress,
                              queryId,
                              toAddress,
                              methodName,
                              args)
{
  Jamp.Message.call(this, headerMap);

  this.fromAddress = fromAddress
  this.queryId = queryId;

  this.toAddress = toAddress;
  this.methodName = methodName;

  this.args = args;

  if (fromAddress == null) {
    this.fromAddress = "me";
  }

  this.serializeImpl = function (array)
  {
    array.push("query");
    array.push(this.headerMap);
    array.push(this.fromAddress);
    array.push(this.queryId);
    array.push(this.toAddress);
    array.push(this.methodName);

    if (this.args != null) {
      for (var i = 0; i < args.length; i++) {
        array.push(this.args[i]);
      }
    }
  };
};

Jamp.SubscribeMessage = function (headerMap,
                                  serviceName,
                                  methodName,
                                  args,
                                  callbackAddress)
{
  Jamp.Message.call(this, headerMap);

  this.toAddress = serviceName;
  this.methodName = methodName;

  this.args = args;
  this.callbackAddress = callbackAddress;

  this.serializeImpl = function (array)
  {
    array.push("send");
    array.push(this.headerMap);
    array.push(this.toAddress);
    array.push(this.methodName);
    array.push(this.callbackAddress);

    if (this.args != null) {
      for (var i = 0; i < args.length; i++) {
        array.push(this.args[i]);
      }
    }
  };
};

Jamp.ReplyMessage = function (headerMap, fromAddress, queryId, result)
{
  Jamp.Message.call(this, headerMap);

  this.fromAddress = fromAddress;
  this.queryId = queryId;

  this.result = result;

  this.serializeImpl = function (array)
  {
    array.push("reply");
    array.push(this.headerMap);
    array.push(this.fromAddress);
    array.push(this.queryId);
    array.push(this.result);
  };
};

Jamp.ErrorMessage = function (headerMap, toAddress, queryId, result)
{
  Jamp.Message.call(this, headerMap);

  this.toAddress = toAddress;
  this.queryId = queryId;

  this.result = result;

  this.serializeImpl = function (array)
  {
    array.push("error");
    array.push(this.headerMap);
    array.push(this.toAddress);
    array.push(this.queryId);
    array.push(this.result);
  };
};
Jamp.Channel = function (url)
{
  this.url = url;

  this.requestMap = {};
  this.callbackMap = {};
  this.listenerMap = {};

  this.callbackCount = 0;
  this.queryCount = 0;

  this.close;
  this.reconnect;
  this.submitRequest;

  this.query = function (serviceName,
                         methodName,
                         args,
                         isBlocking,
                         headerMap,
                         fromAddress)
  {
    var queryId = this.queryCount++;

    var msg = new Jamp.QueryMessage(headerMap,
                                    fromAddress,
                                    queryId,
                                    serviceName,
                                    methodName,
                                    args);

    var request = this.createQueryRequest(queryId, msg);
    this.submitRequest(request, isBlocking);

    return request.promise;
  };

  this.send = function (serviceName,
                        methodName,
                        args,
                        isBlocking,
                        headerMap,
                        fromAddress)
  {
    var queryId = this.queryCount++;

    var msg = new Jamp.SendMessage(headerMap, serviceName, methodName, args);

    var request = this.createSendRequest(queryId, msg);
    this.submitRequest(request, isBlocking);

    return request.promise;
  };

  this.setListener = function (serviceName,
                               methodName,
                               args,
                               headerMap,
                               callbackAddress,
                               listener)
  {
    var queryId = this.queryCount++;

    var msg = new Jamp.SubscribeMessage(headerMap,
                                        serviceName,
                                        methodName,
                                        args,
                                        callbackAddress);

    this.addListener(callbackAddress, listener);

    var request = this.createSendRequest(queryId, msg);
    this.submitRequest(request, false);

    return request.promise;
  };

  this.addListener = function (callbackAddress, listener)
  {
    this.listenerMap[callbackAddress] = listener;
  };

  this.getListener = function (callbackAddress)
  {
    return this.listenerMap[callbackAddress];
  }

  this.registerCallback = function (callback)
  {
    var serviceName = "channel:";
    var methodName = "publishChannel";

    var id = "/_cb_" + this.callbackCount++;
    var args = new Array().push(id);

    var promise = this.query(serviceName, methodName, args);

    promise.then(function completed(value)
                 {
                   var address = value;

                   this.callbackMap[address] = callback;

                   promise.resolved(address);
                 });

    return promise;
  };

  this.removeCallback = function (callback)
  {
    var address = null;

    for (var key in this.callbackMap) {
      var value = this.callbackMap[key];

      if (value === callback) {
        address = key;

        break;
      }
    }

    if (address == null) {
      throw new Error("callback has not been registered: " + callback);
    }

    delete this.callbackMap[address];
  };

  this.createSendRequest = function (queryId, msg)
  {
    var request = new Jamp.SendRequest(queryId, msg);

    this.requestMap[queryId] = request;

    return request;
  };

  this.createQueryRequest = function (queryId, msg)
  {
    var request = new Jamp.QueryRequest(queryId, msg);

    this.requestMap[queryId] = request;

    return request;
  };

  var myThis = this;

  this.onMessageJson = function (json)
  {
    var msg = Jamp.unserialize(json);

    myThis.onMessage(msg);
  }

  this.onMessageArray = function (list)
  {
    for (var i = 0; i < list.length; i++) {
      var msg = Jamp.unserializeArray(list[i]);
      this.onMessage(msg);
    }
  };

  this.onMessage = function (msg)
  {
    if (msg instanceof Jamp.ReplyMessage) {
      var queryId = msg.queryId;
      var request = this.removeRequest(queryId);

      if (request != null) {
        request.completed(this, msg.result);
      }
      else {
        console.log("cannot find request for query id: " + queryId);
      }
    }
    else if (msg instanceof Jamp.ErrorMessage) {
      var queryId = msg.queryId;
      var request = this.removeRequest(queryId);

      if (request != null) {
        request.error(this, msg.result);
      }
      else {
        console.log("cannot find request for query id: " + queryId);
      }
    }
    else if (msg instanceof Jamp.SendMessage) {
      var listener = this.getListener(msg.toAddress);

      listener[msg.methodName].apply(listener, msg.parameters);
    }
    else {
      throw new Error("unexpected jamp message type: " + msg);
    }
  };

  this.checkRequests = function ()
  {
    var expiredRequests = new Array();

    for (var queryId in this.requestMap) {
      var request = this.requestMap[queryId];

      expiredRequests.push(request);
    }

    for (var i = 0; i < expiredRequests.length; i++) {
      var request = expiredRequests[i];

      this.removeRequest(request.queryId);

      request.error(this, "request expired");
    }
  };

  this.removeRequest = function (queryId)
  {
    var request = this.requestMap[queryId];

    delete this.requestMap[queryId];

    return request;
  };
};

Jamp.Request = function (queryId, msg, timeout)
{
  this.queryId = queryId;
  this.msg = msg;

  this.expirationTime = timeout;
  this.promise = new Jamp.Promise();

  if (timeout == null) {
    this.expirationTime = new Date(new Date().getTime() + 1000 * 60 * 5);
  }

  this.isExpired = function (now)
  {
    if (now == null) {
      now = new Date();
    }

    return (now.getTime() - this.expirationTime.getTime()) > 0;
  };

  this.sent = function (channel)
  {
  };

  this.completed = function (channel, value)
  {
    if (!this.promise.isCompleted) {
      this.promise.resolve(value);
    }
  };

  this.error = function (channel, value)
  {
    if (!this.promise.isCompleted) {
      this.promise.reject(value);
    }
  };
};

Jamp.SendRequest = function (queryId, msg, timeout)
{
  Jamp.Request.call(this, queryId, msg, timeout);

  this.sent = function (channel)
  {
    channel.removeRequest(this.queryId);

    if (!this.promise.isCompleted) {
      this.promise.resolve(true);
    }
  };
};

Jamp.QueryRequest = function (queryId, msg, timeout)
{
  Jamp.Request.call(this, queryId, msg, timeout);
};

Jamp.Promise = function ()
{
  this.resolvedValue = null;
  this.rejectedValue = null;

  this.onFulfilledArray = new Array();
  this.onRejectedArray = new Array();

  this.isCompleted = false;

  this.resolve = function (value)
  {
    this.resolvedValue = value;
    this.isCompleted = true;

    for (var i = 0; i < this.onFulfilledArray.length; i++) {
      this.onFulfilledArray[i](value);
    }
  };

  this.reject = function (value)
  {
    this.rejectedValue = value;
    this.isCompleted = true;

    for (var i = 0; i < this.onRejectedArray.length; i++) {
      this.onRejectedArray[i](value);
    }
  };

  this.then = function (onFulfilled, onRejected)
  {
    if (onFulfilled != null) {
      this.onFulfilledArray.push(onFulfilled);
    }

    if (onRejected != null) {
      this.onRejectedArray.push(onRejected);
    }

    return this;
  };
};
Jamp.HttpChannel = function (url, onChannel)
{
  Jamp.Channel.call(this, url);

  this.submitRequest = function (request, isBlocking)
  {
    var httpRequest;

    if (isBlocking) {
      httpRequest = this.initRpcRequest();
    }
    else {
      httpRequest = this.initPushRequest();
    }

    var msg = request.msg;

    var json = msg.serialize();
    json = "[" + json + "]";

    var channel = this;

    httpRequest.onreadystatechange = function ()
    {
      if (httpRequest.readyState != 4) {
        return;
      }

      if (httpRequest.status == "200") {
        request.sent(channel);

        channel.pull(channel);
      }
      else {
        request.error(this,
                      "error submitting query "
                      + httpRequest.status
                      + " "
                      + httpRequest.statusText
                      + " : "
                      + httpRequest.responseText);
      }
    };

    httpRequest.send(json);
  };

  this.initRpcRequest = function ()
  {
    var httpRequest = new XMLHttpRequest();

    httpRequest.open("POST", this.url, true);
    httpRequest.setRequestHeader("Content-Type", "x-application/jamp-rpc");

    return httpRequest;
  };

  this.initPushRequest = function ()
  {
    var httpRequest = new XMLHttpRequest();

    httpRequest.open("POST", this.url, true);
    httpRequest.setRequestHeader("Content-Type", "x-application/jamp-push");

    return httpRequest;
  };

  this.initPullRequest = function ()
  {
    var httpRequest = new XMLHttpRequest();

    httpRequest.open("POST", this.url, true);
    httpRequest.setRequestHeader("Content-Type", "x-application/jamp-pull");

    return httpRequest;
  };

  this.pull = function (channel)
  {
    var httpRequest = channel.initPullRequest();
    httpRequest.send("[]");

    httpRequest.onreadystatechange = function ()
    {
      if (httpRequest.readyState != 4) {
        return;
      }

      if (httpRequest.status == "200") {
        var json = httpRequest.responseText;

        var list = JSON.parse(json);

        channel.onMessageArray(list);
      }
      else {
        console.log(this,
                    "error submitting query "
                    + httpRequest.status
                    + " "
                    + httpRequest.statusText
                    + " : "
                    + httpRequest.responseText);
      }

      channel.pull(channel);
    };

    httpRequest.ontimeout = function ()
    {
      channel.pull(channel);
    };
  };

  return this;
};
Jamp.WebSocketChannel = function (url, onChannel)
{
  Jamp.Channel.call(this, url);

  this.close = function ()
  {
    this.conn.close();
  };

  this.reconnect = function ()
  {
    this.conn.reconnect(this.conn);
  };

  this.removeRequest = function (queryId)
  {
    var request = this.requestMap[queryId];

    delete this.requestMap[queryId];

    return request;
  };

  this.submitRequest = function (request, isBlocking)
  {
    this.conn.addRequest(request);
  };

  this.toString = function ()
  {
    return "Jamp.WebSocketChannel[]";
  };

  this.conn = new Jamp.WebSocketConnection(url, this.onMessageJson, this);
  this.onChannel = onChannel;
  this.conn.init(this.conn);
};

Jamp.WebSocketConnection = function (url,
                                     onMessageHandler,
                                     channel,
                                     reconnectIntervalMs,
                                     isReconnectOnClose,
                                     isReconnectOnError)
{
  this.url = url;
  this.channel = channel;
  this.onMessageHandler = onMessageHandler;

  this.socket;
  this.isClosing = false;
  this.requestQueue = new Array();

  this.reconnectIntervalMs = 5000;
  this.isReconnectOnClose = true;
  this.isReconnectOnError = true;

  if (reconnectIntervalMs != null) {
    this.reconnectIntervalMs = reconnectIntervalMs;
  }

  if (isReconnectOnClose != true) {
    this.isReconnectOnClose = false;
  }

  if (isReconnectOnError != true) {
    this.isReconnectOnError = false;
  }

  this.init = function (conn)
  {
    if (conn.isClosing) {
      return;
    }

    conn.socket = new WebSocket(this.url, ["jamp"]);

    conn.socket.onopen = function ()
    {
      if (conn.channel.onChannel)
        conn.channel.onChannel(conn.channel);

      conn.submitRequestLoop();
    };

    conn.socket.onclose = function ()
    {
      if (conn.isClosing) {
        return;
      }

      conn.reconnect(conn);
    };

    conn.socket.onerror = function ()
    {
      if (conn.isClosing) {
        return;
      }

      conn.reconnect(conn);
    };

    this.socket.onmessage = function (event)
    {
      conn.onMessageHandler(event.data);
    }
  };

  this.addRequest = function (data)
  {
    if (this.isClosing) {
      throw new Error("websocket is closing");
    }

    this.requestQueue.push(data);

    if (this.socket.readyState = WebSocket.OPEN) {
      this.submitRequestLoop();
    }
  };

  this.submitRequestLoop = function ()
  {
    while (this.socket.readyState === WebSocket.OPEN
           && this.requestQueue.length > 0) {
      var request = this.requestQueue[0];
      var msg = request.msg;

      var json = msg.serialize();

      this.socket.send(json);

      request.sent(this.channel);

      this.requestQueue.splice(0, 1);
    }
  };

  this.reconnect = function (conn)
  {
    this.close();

    this.isClosing = false;

    setTimeout(conn.init(conn), this.reconnectIntervalMs);
  };

  this.close = function ()
  {
    this.isClosing = true;

    if (this.socket.readyState == WebSocket.OPEN) {
      this.socket.close();
    }
  };
};
