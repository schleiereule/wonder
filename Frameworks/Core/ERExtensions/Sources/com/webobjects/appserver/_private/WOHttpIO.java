/*
 * $Id: WOHttpIO.java,v 1.1.2.2 2012/07/29 10:49:40 helmut Exp $
 * JD-Core Version:    0.6.0
 */

/*      */ package com.webobjects.appserver._private;
/*      */ 
/*      */ import com.webobjects.appserver.WOApplication;
/*      */ import com.webobjects.appserver.WOMessage;
/*      */ import com.webobjects.appserver.WORequest;
/*      */ import com.webobjects.appserver.WOResponse;
/*      */ import com.webobjects.foundation.NSArray;
/*      */ import com.webobjects.foundation.NSData;
/*      */ import com.webobjects.foundation.NSDictionary;
/*      */ import com.webobjects.foundation.NSLog;
/*      */ import com.webobjects.foundation.NSLog.Logger;
/*      */ import com.webobjects.foundation.NSMutableArray;
/*      */ import com.webobjects.foundation.NSMutableData;
/*      */ import com.webobjects.foundation.NSMutableDictionary;
/*      */ import com.webobjects.foundation.NSMutableRange;
/*      */ import com.webobjects.foundation.NSRange;
/*      */ import com.webobjects.foundation._NSStringUtilities;
/*      */ import java.io.BufferedInputStream;
/*      */ import java.io.IOException;
/*      */ import java.io.InputStream;
/*      */ import java.io.OutputStream;
/*      */ import java.io.PushbackInputStream;
/*      */ import java.net.Socket;
/*      */ import java.net.SocketException;
/*      */ 
/*      */ public final class WOHttpIO
/*      */ {
/*      */   private static final int USE_KEEP_ALIVE_DEFAULT = 2;
/*      */   private int _keepAlive;
/*      */   private static final int _TheInputBufferSize = 2048;
/*      */   private static final int _TheOutputBufferSize = 2048;
/*      */   private static final int _HighWaterBufferSize;
/*   46 */   public static String URIResponseString = " Apple WebObjects\r\n";
/*      */ 
/*   48 */   private final WOHTTPHeaderValue KeepAliveValue = new WOHTTPHeaderValue("keep-alive");
/*      */ 
/*   50 */   private final WOHTTPHeaderValue CloseValue = new WOHTTPHeaderValue("close");
/*      */ 
/*   56 */   private final WOLowercaseCharArray ConnectionKey = new WOLowercaseCharArray("connection");
/*      */ 
/*   58 */   private final WOLowercaseCharArray ContentLengthKey = new WOLowercaseCharArray("content-length");
             private final WOLowercaseCharArray XContentLengthKey = new WOLowercaseCharArray("x-content-length");

/*      */ 
/*   60 */   private final WOLowercaseCharArray TransferEncodingKey = new WOLowercaseCharArray("transfer-encoding");
/*      */   private byte[] _buffer;
/*      */   private int _bufferLength;
/*      */   private int _bufferIndex;
/*      */   private int _lineStartIndex;
/*      */   StringBuffer _headersBuffer;
/*   76 */   public boolean _socketClosed = false;
/*      */   private final WOApplication _application;
/*   80 */   private static boolean _expectContentLengthHeader = true;
/*      */ 
/*   82 */   private static int _contentTimeout = 5000;
/*      */   private final WOHTTPHeadersDictionary _headers;
/*   86 */   public static boolean _alwaysAppendContentLength = true;
/*      */ 
/*      */   public static void expectContentLengthHeader(boolean expectContentLengthHeader, int contentTimeout)
/*      */   {
/*   98 */     _expectContentLengthHeader = expectContentLengthHeader;
/*   99 */     _contentTimeout = contentTimeout;
/*      */   }
/*      */ 
/*      */   private int _readBlob(InputStream inputStream, int length)
/*      */     throws IOException
/*      */   {
/*  105 */     byte[] leftOverBuffer = _buffer;
/*  106 */     int leftOverLength = _bufferLength - _bufferIndex;
/*  107 */     int leftOverStart = _bufferIndex;
/*      */ 
/*  111 */     _ensureBufferIsLargeEnoughToRead(length - leftOverLength);
/*  112 */     if (_buffer != leftOverBuffer)
/*      */     {
/*  115 */       System.arraycopy(leftOverBuffer, leftOverStart, _buffer, 0, leftOverLength);
/*  116 */       _bufferLength = leftOverLength;
/*      */     }
/*      */ 
/*  119 */     int read = leftOverLength;
/*  120 */     int newlyRead = 1;
/*      */ 
/*  122 */     while ((read < length) && (newlyRead > 0)) {
/*  123 */       newlyRead = inputStream.read(_buffer, _bufferIndex + read, length - read);
/*  124 */       read += newlyRead;
/*      */     }
/*      */ 
/*  127 */     return read > length ? length : read;
/*      */   }
/*      */ 
/*      */   private int refillInputBuffer(InputStream inputStream)
/*      */     throws IOException
/*      */   {
/*  133 */     int moreLength = 0;
/*  134 */     boolean resetLineStartIndex = true;
/*      */ 
/*  136 */     if (_bufferIndex >= 1)
/*      */     {
/*  139 */       if (_bufferLength < _buffer.length)
/*      */       {
/*  142 */         moreLength = inputStream.read(_buffer, _bufferLength, _buffer.length - _bufferLength);
/*  143 */         resetLineStartIndex = false;
/*      */       }
/*      */       else
/*      */       {
/*  147 */         byte[] leftOverBuffer = _buffer;
/*      */ 
/*  150 */         int leftOverLength = _bufferLength - _lineStartIndex;
/*  151 */         int leftOverLineStartIndex = _lineStartIndex;
/*      */ 
/*  153 */         _ensureBufferIsLargeEnoughToRead(_buffer.length);
/*      */ 
/*  155 */         System.arraycopy(leftOverBuffer, leftOverLineStartIndex, _buffer, 0, leftOverLength);
/*  156 */         _bufferLength = leftOverLength;
/*      */ 
/*  158 */         moreLength = inputStream.read(_buffer, leftOverLength, _buffer.length - leftOverLength);
/*  159 */         _bufferIndex = leftOverLength;
/*      */       }
/*      */     }
/*      */     else {
/*  163 */       _bufferLength = 0;
/*  164 */       _bufferIndex = 0;
/*  165 */       moreLength = inputStream.read(_buffer, 0, _buffer.length);
/*      */     }
/*      */ 
/*  168 */     if (moreLength < 1) {
/*  169 */       return 0;
/*      */     }
/*      */ 
/*  172 */     _bufferLength += moreLength;
/*  173 */     if (resetLineStartIndex)
/*  174 */       _lineStartIndex = 0;
/*  175 */     return _bufferLength;
/*      */   }
/*      */ 
/*      */   public int readLine(InputStream inputStream) throws IOException {
/*  179 */     boolean foundNewline = false;
/*  180 */     boolean foundCR = false;
/*  181 */     boolean foundEnd = false;
/*      */ 
/*  183 */     _lineStartIndex = _bufferIndex;
/*      */     do
/*      */     {
/*  192 */       for (; _bufferIndex < _bufferLength; _bufferIndex += 1) {
/*  193 */         if (foundNewline) {
/*  194 */           if (_buffer[_bufferIndex] == 9) {
/*  195 */             _buffer[_bufferIndex] = 32;
/*  196 */             foundNewline = foundCR = false;
/*  197 */           } else if (_buffer[_bufferIndex] == 32) {
/*  198 */             foundNewline = foundCR = false;
/*      */           } else {
/*  200 */             foundEnd = true;
/*      */           }
/*      */         }
/*  203 */         else if (_buffer[_bufferIndex] == 13) {
/*  204 */           _buffer[_bufferIndex] = 32;
/*  205 */           foundCR = true;
/*  206 */         } else if (_buffer[_bufferIndex] == 10) {
/*  207 */           _buffer[_bufferIndex] = 32;
/*  208 */           foundNewline = true;
/*  209 */           if (_bufferIndex - _lineStartIndex < 2) {
/*  210 */             foundEnd = true;
/*  211 */             _bufferIndex += 1;
/*      */           }
/*      */         }
/*      */ 
/*  215 */         if (foundEnd)
/*      */         {
/*      */           break;
/*      */         }
/*      */       }
/*  220 */       if ((_bufferIndex < _bufferLength) || (foundEnd))
/*      */         continue;
/*  222 */       if (refillInputBuffer(inputStream) == 0) {
/*  223 */         if (foundNewline)
/*      */         {
/*      */           break;
/*      */         }
/*      */ 
/*  228 */         return 0;
/*      */       }
/*      */ 
/*  232 */     } while (!foundEnd);
/*      */ 
/*  235 */     int endSearchLocation = _bufferIndex;
/*      */ 
/*  237 */     if (_bufferIndex > _bufferLength)
/*      */     {
/*  244 */       _bufferIndex = _bufferLength;
/*      */     }
/*      */ 
/*  249 */     if (foundNewline) {
/*  250 */       endSearchLocation--;
/*      */ 
/*  252 */       if (foundCR) {
/*  253 */         endSearchLocation--;
/*      */       }
/*      */     }
/*      */ 
/*  257 */     return endSearchLocation - _lineStartIndex;
/*      */   }
/*      */ 
/*      */   public WOHttpIO() {
/*  261 */     _buffer = new byte[_TheInputBufferSize];
/*  262 */     _headersBuffer = new StringBuffer(_TheOutputBufferSize);
/*  263 */     _headers = new WOHTTPHeadersDictionary();
/*  264 */     _application = WOApplication.application();
/*      */   }
/*      */ 
/*      */   public void resetBuffer() {
/*  268 */     _bufferLength = 0;
/*  269 */     _bufferIndex = 0;
/*  270 */     _lineStartIndex = 0;
/*      */   }
/*      */ 
/*      */   private void _ensureBufferIsLargeEnoughToRead(int length) {
/*  274 */     int newSize = _buffer.length;
/*  275 */     if (length + _bufferLength > newSize) {
/*  276 */       while (length + _bufferLength > newSize)
/*  277 */         newSize <<= 1;
/*  278 */       _buffer = new byte[newSize];
/*      */ 
/*  280 */       resetBuffer();
/*      */     }
/*      */   }
/*      */ 
/*      */   private void _shrinkBufferToHighWaterMark() {
/*  285 */     if (_buffer.length > _HighWaterBufferSize)
/*      */     {
/*  288 */       _buffer = new byte[_TheInputBufferSize];
/*  289 */       resetBuffer();
/*      */     }
/*      */   }
/*      */ 
/*      */   public WORequest readRequestFromSocket(Socket connectionSocket) throws IOException {
/*  294 */     InputStream sis = connectionSocket.getInputStream();
/*  295 */     int p = 0;
/*  296 */     int q = 0;
/*  297 */     int offset = 0;
               int lineLength;
/*      */ 
/*  299 */     WORequest aRequest = null;
/*  300 */     String aMethodString = null;
/*  301 */     String aURIString = null;
/*  302 */     String aHttpVersionString = null;
/*      */ 
/*  304 */     resetBuffer();
/*      */ 
/*  307 */     _headers.dispose();
/*      */ 
/*  310 */     lineLength = readLine(sis); // method in first line
/*  311 */     if (lineLength == 0) {
/*  312 */       return null;
/*      */     }
/*      */ 
/*  315 */     offset = _lineStartIndex;
/*  316 */     int lineLengthMinusOne = lineLength - 1;
/*      */ 
/*  321 */     while ((_buffer[(p + offset)] != 32) && (p < lineLengthMinusOne))
/*  322 */       p++;
/*  323 */     if (p < lineLengthMinusOne)
/*      */     {
/*  326 */       q = lineLengthMinusOne;
/*  327 */       while ((_buffer[(q + offset)] != 32) && (q > p)) {
/*  328 */         q--;
/*      */       }
/*  330 */       int _stringLength = lineLengthMinusOne - q;
/*  331 */       if (_stringLength > 0) {
/*  332 */         aHttpVersionString = _NSStringUtilities.stringForBytes(_buffer, q + offset + 1, _stringLength, WORequest.defaultHeaderEncoding());
/*      */       }
/*      */ 
/*  335 */       _stringLength = q - p - 1;
/*  336 */       if (_stringLength > 0) {
/*  337 */         aURIString = _NSStringUtilities.stringForBytes(_buffer, p + offset + 1, _stringLength, WORequest.defaultHeaderEncoding());
/*      */       }
/*      */ 
/*  340 */       _stringLength = p;
/*  341 */       if (_stringLength > 0) {
/*  342 */         aMethodString = _NSStringUtilities.stringForBytes(_buffer, offset, _stringLength, WORequest.defaultHeaderEncoding());
/*      */       }
/*      */ 
/*      */     }
/*      */ 
/*  349 */     _keepAlive = USE_KEEP_ALIVE_DEFAULT;
/*      */ 
/*  352 */     InputStream pbsis = _readHeaders(sis, true, true, false);
/*      */ 
/*  355 */     NSData contentData = null;
/*  356 */     long contentLengthLong = 0;
/*  357 */     NSArray headers = (NSArray)_headers.objectForKey(XContentLengthKey);
/*  358 */     if ((headers != null) && (headers.count() == 1) && (pbsis != null)) {
/*      */       try {
/*  360 */         contentLengthLong = Long.parseLong(headers.lastObject().toString());
/*      */       } catch (NumberFormatException e) {
/*  362 */         if (WOApplication._isDebuggingEnabled()) {
/*  363 */           NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + headers.lastObject() + "'.");
/*      */         }
/*      */ 
/*      */       }
/*      */ 
/*  368 */       if (contentLengthLong > 0)
/*  369 */         contentData = new WOInputStreamData(pbsis, (int)contentLengthLong);
/*      */     } else {
/*  373 */       NSData fakeContentData = _content(sis, connectionSocket, false);
/*      */ 
/*  375 */       if (fakeContentData != null) {
/*  376 */         contentData = new WOInputStreamData(fakeContentData);
/*      */       }
/*      */     }
/*      */ 
/*  380 */     aRequest = _application.createRequest(aMethodString, aURIString, aHttpVersionString, _headers != null ? _headers.headerDictionary() : null, contentData, null);
/*  381 */     if (aRequest != null) {
/*  382 */       aRequest._setOriginatingAddress(connectionSocket.getInetAddress());
/*  383 */       aRequest._setOriginatingPort(connectionSocket.getPort());
/*  384 */       aRequest._setAcceptingAddress(connectionSocket.getLocalAddress());
/*  385 */       aRequest._setAcceptingPort(connectionSocket.getLocalPort());
/*      */     }
/*      */ 
/*  388 */     _shrinkBufferToHighWaterMark();
/*      */ 
/*  390 */     return aRequest;
/*      */   }
/*      */ 
/*      */   private void appendMessageHeaders(WOMessage message) {
/*  394 */     NSDictionary headers = message.headers();
/*  395 */     if (headers != null)
/*      */     {
/*  397 */       if (!(headers instanceof NSMutableDictionary)) {
/*  398 */         headers = headers.mutableClone();
/*      */       }
/*  400 */       ((NSMutableDictionary)headers).removeObjectForKey(ContentLengthKey);
/*      */ 
/*  402 */       NSArray headerKeys = headers.allKeys();
/*      */       NSArray values;
/*      */       Object aKey;
/*  405 */       int kc = headerKeys.count();
/*  406 */       for (int i = 0; i < kc; i++) {
/*  407 */         aKey = headerKeys.objectAtIndex(i);
/*  408 */         values = message.headersForKey(aKey);
/*  409 */         int vc = values.count();
/*  410 */         if ((aKey instanceof WOLowercaseCharArray)) {
/*  411 */           char[] aKeyCharArray = ((WOLowercaseCharArray)aKey).toCharArray();
/*  412 */           for (int j = 0; j < vc; j++) {
/*  413 */             _headersBuffer.append(aKeyCharArray);
/*  414 */             _headersBuffer.append(": ");
/*  415 */             _headersBuffer.append(values.objectAtIndex(j));
/*  416 */             _headersBuffer.append("\r\n");
/*      */           }
/*      */         } else {
/*  419 */           for (int j = 0; j < vc; j++) {
/*  420 */             _headersBuffer.append(aKey);
/*  421 */             _headersBuffer.append(": ");
/*  422 */             _headersBuffer.append(values.objectAtIndex(j));
/*  423 */             _headersBuffer.append("\r\n");
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   public boolean sendResponse(WOResponse aResponse, Socket connectionSocket, WORequest aRequest) throws IOException {
/*  431 */     String httpVersion = aResponse.httpVersion();
/*      */ 
/*  433 */     _headersBuffer.setLength(0);
/*  434 */     _headersBuffer.append(httpVersion);
/*  435 */     _headersBuffer.append(' ');
/*  436 */     _headersBuffer.append(aResponse.status());
/*  437 */     _headersBuffer.append(URIResponseString);
/*      */ 
/*  439 */     return sendMessage(aResponse, connectionSocket, httpVersion, aRequest);
/*      */   }
/*      */ 
/*      */   public void sendRequest(WORequest aRequest, Socket connectionSocket) throws IOException {
/*  443 */     String httpVersion = aRequest.httpVersion();
/*  444 */     _headersBuffer.setLength(0);
/*      */ 
/*  446 */     _headersBuffer.append(aRequest.method());
/*  447 */     _headersBuffer.append(' ');
/*  448 */     _headersBuffer.append(aRequest.uri());
/*  449 */     _headersBuffer.append(' ');
/*  450 */     _headersBuffer.append(httpVersion);
/*  451 */     _headersBuffer.append("\r\n");
/*      */ 
/*  453 */     sendMessage(aRequest, connectionSocket, httpVersion, null);
/*      */   }
/*      */ 
/*      */   protected boolean sendMessage(WOMessage aMessage, Socket connectionSocket, String httpVersion, WORequest aRequest) throws IOException
/*      */   {
/*  458 */     long length = 0L;
/*  459 */     NSData someContent = null;
/*      */ 
/*  462 */     appendMessageHeaders(aMessage);
/*      */     boolean keepSocketAlive;
/*      */     //boolean keepSocketAlive;
/*  464 */     if (isHTTP11(httpVersion))
/*      */     {
/*      */       //boolean keepSocketAlive;
/*  465 */       if (_keepAlive == 0) {
/*  466 */         _headersBuffer.append("connection: close\r\n");
/*  467 */         keepSocketAlive = false;
/*      */       } else {
/*  469 */         keepSocketAlive = true;
/*      */       }
/*      */     }
/*      */     else
/*      */     {
/*      */       //boolean keepSocketAlive;
/*  472 */       if (_keepAlive == 1) {
/*  473 */         _headersBuffer.append("connection: keep-alive\r\n");
/*  474 */         keepSocketAlive = true;
/*      */       } else {
/*  476 */         keepSocketAlive = false;
/*      */       }
/*      */ 
/*      */     }
/*      */ 
/*  482 */     if (aRequest != null) {
/*  483 */       NSData contentData = aRequest.content();
/*  484 */       if ((contentData != null) && ((contentData instanceof WOInputStreamData))) {
/*  485 */         WOInputStreamData isData = (WOInputStreamData)contentData;
/*      */ 
/*  487 */         InputStream is = isData._stream();
/*  488 */         if ((is != null) && ((is instanceof WONoCopyPushbackInputStream))) {
/*  489 */           WONoCopyPushbackInputStream sis = (WONoCopyPushbackInputStream)is;
/*  490 */           if (sis.wasPrematurelyTerminated())
/*      */           {
/*  492 */             return false;
/*      */           }
/*  494 */           String contentLengthString = aRequest.headerForKey("content-length");
/*  495 */           long contentLength = contentLengthString != null ? Long.parseLong(contentLengthString) : 0L;
/*  496 */           if (contentLength > 0L) {
/*  497 */             int _originalReadTimeout = -1;
/*      */             try {
/*  499 */               _originalReadTimeout = setSocketTimeout(connectionSocket, _contentTimeout);
/*      */ 
/*  501 */               sis.drain();
/*      */ 
/*  503 */               if (NSLog.debugLoggingAllowedForLevelAndGroups(3, 4L)) {
/*  504 */                 NSLog.out.appendln("<WOHttpIO>: Drained socket");
/*      */               }
/*      */ 
/*  507 */               if (_originalReadTimeout != -1)
/*  508 */                 _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
/*      */             }
/*      */             catch (SocketException socketException) {
/*  511 */               if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L)) {
/*  512 */                 NSLog.err.appendln("<WOHttpIO>: Unable to set socket timeout:" + socketException.getMessage());
/*  513 */                 NSLog._conditionallyLogPrivateException(socketException);
/*      */               }
/*      */             } catch (IOException iioe) {
/*  516 */               if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
/*  517 */                 NSLog.err.appendln("<WOHttpIO>: Finished reading before content length of " + contentLength + " : " + iioe.getMessage());
/*  518 */                 NSLog._conditionallyLogPrivateException(iioe);
/*      */               }
/*      */             }
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/*  526 */     InputStream is = null;
/*  527 */     int bufferSize = 0;
/*      */ 
/*  529 */     if ((aMessage instanceof WOResponse))
/*      */     {
/*  532 */       WOResponse theResponse = (WOResponse)aMessage;
/*  533 */       is = theResponse.contentInputStream();
/*  534 */       if (is != null) {
/*  535 */         bufferSize = theResponse.contentInputStreamBufferSize();
/*  536 */         length = theResponse.contentInputStreamLength();
/*      */       }
/*      */     }
/*      */ 
/*  540 */     if (is == null) {
/*  541 */       someContent = aMessage.content();
/*  542 */       length = someContent.length();
/*      */     }
/*      */ 
/*  546 */     if ((_alwaysAppendContentLength) || (length > 0L)) {
/*  547 */       _headersBuffer.append("content-length: ");
/*  548 */       _headersBuffer.append(length);
/*      */     }
/*      */ 
/*  551 */     _headersBuffer.append("\r\n\r\n");
/*      */ 
/*  555 */     OutputStream outputStream = connectionSocket.getOutputStream();
/*      */ 
/*  559 */     byte[] headerBytes = _NSStringUtilities.bytesForIsolatinString(new String(_headersBuffer));
/*  560 */     outputStream.write(headerBytes, 0, headerBytes.length);
/*      */ 
/*  562 */     String method = aRequest != null ? aRequest.method() : "";
/*  563 */     boolean isHead = method.equals("HEAD");
/*      */ 
/*  565 */     if ((length > 0L) && (!isHead)) {
/*  566 */       if (is == null) {
/*  567 */         NSMutableRange range = new NSMutableRange();
/*  568 */         byte[] contentBytesNoCopy = someContent != null ? someContent.bytesNoCopy(range) : new byte[0];
/*  569 */         outputStream.write(contentBytesNoCopy, range.location(), range.length());
/*      */       }
/*      */       else {
/*      */         try {
/*  573 */           byte[] buffer = new byte[bufferSize];
/*  574 */           while (length > 0L) {
/*  575 */             int read = is.read(buffer, 0, length > bufferSize ? bufferSize : (int)length);
/*  576 */             if (read == -1)
/*      */               break;
/*  578 */             length -= read;
/*  579 */             outputStream.write(buffer, 0, read);
/*      */           }
/*      */         } finally {
/*      */           try {
/*  583 */             is.close();
/*      */           } catch (Exception e) {
/*  585 */             NSLog.err.appendln("<WOHttpIO>: Failed to close content InputStream: " + e);
/*  586 */             if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
/*  587 */               NSLog.err.appendln(e);
/*      */             }
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*  593 */     outputStream.flush();
/*      */ 
/*  595 */     return keepSocketAlive;
/*      */   }
/*      */ 
/*      */   public WOResponse readResponseFromSocket(Socket connectionSocket) throws IOException
/*      */   {
/*  600 */     InputStream sis = connectionSocket.getInputStream();
/*  601 */     int p = 0;
/*  602 */     int q = 0;
/*  603 */     int offset = 0;
/*      */     int lineLength;
/*  605 */     WOResponse response = null;
/*  606 */     String statusCode = null;
/*  607 */     String httpVersion = null;
/*      */ 
/*  610 */     resetBuffer();
/*      */ 
/*  615 */     lineLength = readLine(sis); // status line
/*  616 */     if (lineLength == 0) {
/*  617 */       return null;
/*      */     }
/*      */ 
/*  620 */     _NSStringUtilities.stringForBytes(_buffer, offset, lineLength, WORequest.defaultHeaderEncoding());
/*  621 */     offset = _lineStartIndex;
/*  622 */     int lineLengthMinusOne = lineLength - 1;
/*      */ 
/*  624 */     while ((_buffer[(p + offset)] != 32) && (p < lineLengthMinusOne))
/*  625 */       p++;
/*  626 */     if (p < lineLengthMinusOne) {
/*  627 */       q = p + 1;
/*  628 */       while ((_buffer[(q + offset)] != 32) && (q < lineLengthMinusOne))
/*  629 */         q++;
/*  630 */       if (q < lineLengthMinusOne) {
/*  631 */         _NSStringUtilities.stringForBytes(_buffer, q + offset + 1, lineLengthMinusOne - q, WORequest.defaultHeaderEncoding());
/*      */       }
/*  633 */       statusCode = _NSStringUtilities.stringForBytes(_buffer, p + offset + 1, q - p - 1, WORequest.defaultHeaderEncoding());
/*  634 */       httpVersion = _NSStringUtilities.stringForBytes(_buffer, offset, p, WORequest.defaultHeaderEncoding());
/*      */     }
/*      */ 
/*  637 */     if (_application != null)
/*  638 */       response = _application.createResponseInContext(null);
/*      */     else {
/*  640 */       response = new WOResponse();
/*      */     }
/*  642 */     response.setHTTPVersion(httpVersion);
/*  643 */     response.setStatus(Integer.parseInt(statusCode));
/*      */ 
/*  646 */     _readHeaders(sis, false, false, false);
/*  647 */     response._setHeaders(_headers);
/*      */ 
/*  650 */     boolean closeConnection = false;
/*  651 */     NSArray connectionStatus = (NSArray)_headers.valueForKey("Connection");
/*  652 */     if (connectionStatus != null) {
/*  653 */       int count = connectionStatus.count();
/*  654 */       for (int i = 0; i < count; i++) {
/*  655 */         String headerValue = (String)connectionStatus.objectAtIndex(i);
/*  656 */         if (headerValue.equalsIgnoreCase("close")) {
/*  657 */           closeConnection = true;
/*  658 */           break;
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/*  663 */     NSData contentData = _content(sis, connectionSocket, closeConnection);
/*  664 */     response.setContent(contentData);
/*      */ 
/*  666 */     _shrinkBufferToHighWaterMark();
/*      */ 
/*  670 */     if ((closeConnection) || ((isHTTP11(httpVersion)) && (_keepAlive == 0)) || ((!isHTTP11(httpVersion)) && (_keepAlive != 1))) {
/*  671 */       connectionSocket.close();
/*  672 */       _socketClosed = true;
/*      */     }
/*      */ 
/*  675 */     return response;
/*      */   }
/*      */ 
/*      */   private static final boolean isHTTP11(String httpVersion) {
/*  679 */     return (httpVersion != null) && ("HTTP/1.1".equals(httpVersion));
/*      */   }
/*      */ 
/*      */   public NSDictionary headers()
/*      */   {
/*  684 */     return _headers;
/*      */   }
/*      */ 
/*      */   public InputStream _readHeaders(InputStream sis, boolean checkKeepAlive, boolean isRequest, boolean isMultipartHeaders)
/*      */     throws IOException
/*      */   {
/*  690 */     int offset = 0;
/*      */     while (true) {
/*  700 */       int lineLength = readLine(sis);
/*  701 */       if (lineLength == 0) {
/*      */         break;
/*      */       }
/*  704 */       offset = _lineStartIndex;
/*      */ 
/*  706 */       int startValue = 0;
/*  707 */       int separator = 0;
/*  708 */       for (int i = 0; i < lineLength; i++) {
/*  709 */         if (_buffer[(offset + i)] == 58) { // ':'
/*  710 */           separator = i;
/*  711 */           i++;
/*  712 */           while ((i < lineLength) && (_buffer[(offset + i)] == 32)) {
/*  713 */             i++;
/*      */           }
/*  715 */           if (i < lineLength) {
/*  716 */             startValue = i;
/*  717 */             break;
/*      */           }
/*      */         }
/*      */       }
/*  721 */       if (startValue != 0) {
/*      */ 
/*  731 */           int key_offset = offset;
/*  732 */           int key_length = separator;
/*  733 */           int value_offset = offset + startValue;
/*  734 */           int value_length = lineLength - startValue;

					 WOHTTPHeaderValue headerValue = null;
					 
					 /*
					  * check header key for content-length and set x-content-length for the supplied length
					  * In case our content is larger than max Integer(2G) limit the ContentLength header to max Integer
					  * This allows WO to stay compatible with NSRange, NSData, etc. and still allows
					  * to capture the data via a content stream and the saved x-content-length size information.
					  */
					 String key = new String(_buffer, key_offset, key_length).toLowerCase();
					 if (key.toLowerCase().equals(ContentLengthKey.toString())) {
						 	String val = new String(_buffer, value_offset, value_length);

							try {
								long length = Long.parseLong(val);
								byte xlen[] = new String("x-" + key + val).getBytes();

								headerValue = _headers.setBufferForKey(xlen, key.length()+2, val.length(), 0, key.length()+2);

								if  (length > Integer.MAX_VALUE) {
									val =  "" + Integer.MAX_VALUE;
									byte[] maxIntBytes = new String(key + val).getBytes();
									headerValue = _headers.setBufferForKey(maxIntBytes, key.length(), val.length(), 0, key.length());
							    } else {
							        headerValue = _headers.setBufferForKey(_buffer, value_offset, value_length, key_offset, key_length);
							    }
							} catch (NumberFormatException e) {
								if (WOApplication._isDebuggingEnabled())
									NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + val + "'.");
							}
					 } else {						 
/*  736 */              headerValue = _headers.setBufferForKey(_buffer, value_offset, value_length, key_offset, key_length);
					 }

/*      */ 
/*      */ 
/*  738 */           WOLowercaseCharArray headerKey = _headers.lastInsertedKey();
/*      */ 
/*  740 */           if (checkKeepAlive) {
/*  741 */               if (_keepAlive == USE_KEEP_ALIVE_DEFAULT) {
/*  742 */                   if (ConnectionKey.equals(headerKey)) {
/*  743 */                       if (headerValue.equalsIgnoreCase(KeepAliveValue)) {
/*  744 */                           _keepAlive = 1;
/*  745 */                       } else if (headerValue.equalsIgnoreCase(CloseValue)) {
/*  746 */                           _keepAlive = 0;
/*      */                       }
/*      */                   }
/*      */               }
/*      */           }
/*      */        }
/*      */     }
/*  755 */     WONoCopyPushbackInputStream pbsis = null;
/*  756 */     int pushbackLength = _bufferLength - _bufferIndex;
/*      */ 
/*  758 */     if (isRequest)
/*      */     {
/*  761 */       long contentLengthLong = 0;
/*  762 */       NSArray headers = (NSArray)_headers.objectForKey(XContentLengthKey);
/*  763 */       if ((headers != null) && (headers.count() == 1)) {
/*      */         try {
/*  765 */           contentLengthLong = Long.parseLong(headers.lastObject().toString());
/*      */         } catch (NumberFormatException e) {
/*  767 */           if (WOApplication._isDebuggingEnabled()) {
/*  768 */             NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + headers.lastObject() + "'.");
/*      */           }
/*      */ 
/*      */         }
/*      */ 
/*  775 */         if (pushbackLength > contentLengthLong) {
/*  776 */           contentLengthLong = pushbackLength;
/*  777 */           _headers.setObjectForKey(new NSMutableArray("" + pushbackLength), XContentLengthKey);
/*      */         }
/*      */ 
/*  781 */         pbsis = new WONoCopyPushbackInputStream(new BufferedInputStream(sis), contentLengthLong - pushbackLength);
/*      */       }
/*      */ 
/*      */     }
/*  786 */     else if (isMultipartHeaders)
/*      */     {
/*  789 */       if ((sis instanceof WONoCopyPushbackInputStream)) {
/*  790 */         pbsis = (WONoCopyPushbackInputStream)sis;
/*      */       }
/*      */ 
/*      */     }
/*      */ 
/*  797 */     if ((pbsis != null) && 
/*  798 */       (pushbackLength > 0))
/*      */     {
/*  800 */       pbsis.unread(_buffer, _bufferIndex, pushbackLength);
/*      */     }
/*      */ 
/*  804 */     return pbsis;
/*      */   }
/*      */ 
/*      */   private NSData _forceReadContent(InputStream sis, Socket connectionSocket)
/*      */   {
/*  812 */     int bytesRead = 0;
/*  813 */     NSMutableData _contentData = null;
/*  814 */     BufferedInputStream bis = new BufferedInputStream(sis);
/*  815 */     byte[] buffer = new byte[_TheInputBufferSize];
/*      */ 
/*  818 */     int _originalReadTimeout = setSocketTimeout(connectionSocket, _contentTimeout);
/*      */ 
/*  823 */     if (_bufferLength > _bufferIndex) {
/*  824 */       _contentData = new NSMutableData(_bufferLength - _bufferIndex);
/*  825 */       _contentData.appendBytes(_buffer, new NSRange(_bufferIndex, _bufferLength - _bufferIndex));
/*      */     } else {
/*  827 */       _contentData = new NSMutableData();
/*      */     }
/*      */     while (true) {
/*      */       try
/*      */       {
/*  832 */         bytesRead = bis.read(buffer, 0, _TheInputBufferSize);
/*  833 */         if (bytesRead >= 0) {
/*  834 */           _contentData.appendBytes(buffer, new NSRange(0, bytesRead));
/*      */         }
/*      */         else
/*      */         {
/*  846 */           if (_originalReadTimeout == -1) break;
/*  847 */           _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout); break;
/*      */         }
/*  846 */         if (_originalReadTimeout != -1)
/*  847 */           _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
/*      */       }
/*      */       catch (IOException ioException)
/*      */       {
/*  840 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
/*  841 */           NSLog.err.appendln("<WOHttpIO>: IOException occurred during read():" + ioException.getMessage());
/*  842 */           NSLog._conditionallyLogPrivateException(ioException);
/*      */         }
/*  844 */         Object localObject1 = null;
/*      */         return (NSData) localObject1;
/*      */       }
/*      */       finally
/*      */       {
/*  846 */         if (_originalReadTimeout != -1)
/*  847 */           _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
/*      */       }
/*      */     }
/*  850 */     return _contentData;
/*      */   }
/*      */ 
/*      */   private NSData _content(InputStream sis, Socket connectionSocket, boolean connectionClosed) throws IOException {
/*  854 */     byte[] content = null;
/*      */ 
/*  856 */     long length = 0;
/*  857 */     int offset = 0;
/*  858 */     NSData contentData = null;
/*      */ 
/*  861 */     NSMutableArray contentLength = (NSMutableArray)_headers.objectForKey(XContentLengthKey);
/*  862 */     if ((contentLength != null) && (contentLength.count() == 1)) {
/*      */       try {
/*  864 */         length = Integer.parseInt((String)contentLength.lastObject());
/*      */       } catch (NumberFormatException e) {
/*  866 */         if (WOApplication._isDebuggingEnabled()) {
/*  867 */           NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + (String)contentLength.lastObject() + "'.");
/*      */         }
/*      */       }
/*  870 */       if (length != 0)
/*      */       {
/*  872 */         length = _readBlob(sis, (int)length);
/*      */ 
/*  874 */         offset = _bufferIndex;
/*  875 */         if (length > 0)
/*      */         {
/*  877 */           content = _buffer;
/*      */         }
/*      */         else
/*      */         {
/*  881 */           offset = 0;
/*  882 */           length = 0;
/*      */         }
/*      */       }
/*      */ 
/*      */       try
/*      */       {
/*  888 */         if (content != null)
/*  889 */           contentData = new NSData(content, new NSRange(offset, (int)length), true);
/*      */       }
/*      */       catch (Exception exception) {
/*  892 */         NSLog.err.appendln("<" + getClass().getName() + "> Error: Request creation failed!\n" + exception.toString());
/*  893 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 8196L))
/*  894 */           NSLog.debug.appendln(exception);
/*      */       }
/*      */     }
/*      */     else {
/*  898 */       boolean readChunks = false;
/*  899 */       NSMutableArray encodingKeys = (NSMutableArray)_headers.objectForKey(TransferEncodingKey);
/*  900 */       if ((encodingKeys != null) && (encodingKeys.count() == 1)) {
/*  901 */         String encoding = (String)encodingKeys.lastObject();
/*  902 */         if ("chunked".equals(encoding)) {
/*  903 */           readChunks = true;
/*      */         }
/*      */       }
/*  906 */       if (readChunks) {
/*  907 */         contentData = _readChunks(sis, connectionSocket);
/*      */       }
/*  909 */       else if ((connectionClosed) || (!_expectContentLengthHeader)) {
/*  910 */         contentData = _forceReadContent(sis, connectionSocket);
/*      */       }
/*      */     }
/*      */ 
/*  914 */     return contentData;
/*      */   }
/*      */ 
/*      */   private NSData _readChunks(InputStream is, Socket socket)
/*      */     throws IOException
/*      */   {
/*  933 */     int _originalReadTimeout = setSocketTimeout(socket, _contentTimeout);
/*      */     try {
/*  935 */       int bytesInBuffer = _bufferLength - _bufferIndex;
/*      */ 
/*  938 */       InputStream inputStream = null;
/*  939 */       if (bytesInBuffer > 0) {
/*  940 */         inputStream = new PushbackInputStream(is, bytesInBuffer);
/*  941 */         ((PushbackInputStream)inputStream).unread(_buffer, _bufferIndex, bytesInBuffer);
/*      */       } else {
/*  943 */         inputStream = is;
/*      */       }
/*  945 */       resetBuffer();
/*  946 */       byte[] buffer = new byte[_TheInputBufferSize];
/*  947 */       NSMutableData result = new NSMutableData();
/*      */       while (true) {
/*  949 */         int contentBytesToRead = readChunkSizeLine(inputStream);
/*      */         int bytesRead;
/*  951 */         if (contentBytesToRead > 0) {
/*  952 */           contentBytesToRead += 2;
/*  953 */           if (contentBytesToRead > buffer.length) {
/*  954 */             buffer = new byte[contentBytesToRead];
/*      */           }
/*  956 */           bytesRead = inputStream.read(buffer, 0, contentBytesToRead);
/*  957 */           if (bytesRead > contentBytesToRead) {
/*  958 */             bytesRead = contentBytesToRead;
/*      */           }
/*  960 */           if (bytesRead > 0)
/*  961 */             result.appendBytes(buffer, new NSRange(0, bytesRead - 2));
/*      */         }
/*      */         else {
/*  964 */           //bytesRead = result;
/*      */           return result;
/*      */         }
/*      */       }
/*      */     }
/*      */     finally
/*      */     {
/*  968 */       if (_originalReadTimeout != -1)
/*  969 */         _originalReadTimeout = setSocketTimeout(socket, _originalReadTimeout); 
/*  969 */     }
/*      */   }
/*      */ 
/*      */   private int readChunkSizeLine(InputStream is)
/*      */     throws IOException
/*      */   {
/*  981 */     int contentBytesToRead = 0;
/*  982 */     boolean skip = false;
/*  983 */     StringBuffer sb = new StringBuffer();
/*      */     while (true) {
/*  985 */       int b = is.read();
/*  986 */       sb.append((char)b);
/*  987 */       if (b == 59) {
/*  988 */         skip = true;
/*  989 */       } else if (b == 13) {
/*  990 */         is.read();
/*  991 */         break;
/*      */       }
/*  993 */       if (!skip) {
/*  994 */         int intVal = b >= 65 ? (b >= 97 ? b - 97 : b - 65) + 10 : b - 48;
/*  995 */         contentBytesToRead *= 16;
/*  996 */         contentBytesToRead += intVal;
/*      */       }
/*      */     }
/*  999 */     return contentBytesToRead;
/*      */   }
/*      */ 
/*      */   protected int setSocketTimeout(Socket socket, int timeout) {
/* 1003 */     int old = timeout;
/*      */     try {
/* 1005 */       old = socket.getSoTimeout();
/* 1006 */       if (timeout != -1)
/* 1007 */         socket.setSoTimeout(timeout);
/*      */     }
/*      */     catch (SocketException ex) {
/* 1010 */       if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 8196L)) {
/* 1011 */         NSLog.err.appendln("<WOHttpIO>: Unable to set socket timeout:" + ex.getMessage());
/*      */       }
/*      */     }
/* 1014 */     return old;
/*      */   }
/*      */ 
/*      */   public String toString()
/*      */   {
/* 1019 */     return "<" + getClass().getName() + " keepAlive='" + _keepAlive + "' buffer=" + _buffer + " >";
/*      */   }
/*      */ 
/*      */   static
/*      */   {
/*   89 */     int value = Integer.getInteger("WOMaxIOBufferSize", 8196).intValue();
/*   90 */     if (value != 0)
/*   91 */       _HighWaterBufferSize = value < _TheInputBufferSize ? _TheInputBufferSize : value;
/*      */     else
/*   93 */       _HighWaterBufferSize = 8196;
/*      */   }
/*      */ }
