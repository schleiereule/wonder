/*
 * $Id: WOMultipartIterator.java,v 1.1.2.2 2012/07/29 10:44:18 helmut Exp $
 * JD-Core Version:    0.6.0
 */

/*     */ package com.webobjects.appserver;
/*     */ 
/*     */ import com.webobjects.appserver._private.WOCaseInsensitiveDictionary;
/*     */ import com.webobjects.appserver._private.WOFileUploadSupport;
/*     */ import com.webobjects.appserver._private.WOHTTPHeaderValue;
/*     */ import com.webobjects.appserver._private.WOHTTPHeadersDictionary;
/*     */ import com.webobjects.appserver._private.WOHttpIO;
/*     */ import com.webobjects.appserver._private.WOInputStreamData;
/*     */ import com.webobjects.appserver._private.WONoCopyPushbackInputStream;
/*     */ import com.webobjects.foundation.NSArray;
/*     */ import com.webobjects.foundation.NSData;
/*     */ import com.webobjects.foundation.NSDictionary;
/*     */ import com.webobjects.foundation.NSLog;
/*     */ import com.webobjects.foundation.NSLog.Logger;
/*     */ import com.webobjects.foundation.NSMutableArray;
/*     */ import com.webobjects.foundation.NSMutableDictionary;
/*     */ import com.webobjects.foundation.NSNumberFormatter;
/*     */ import com.webobjects.foundation.NSRange;
/*     */ import com.webobjects.foundation.NSTimestamp;
/*     */ import com.webobjects.foundation.NSTimestampFormatter;
/*     */ import com.webobjects.foundation._NSStringUtilities;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.text.ParseException;
/*     */ import java.util.LinkedList;
/*     */ 
/*     */ public class WOMultipartIterator
/*     */ {
/*     */   private LinkedList<WOFormData> _formDataList;
/*     */   private LinkedList<WOFormData> _formDataStack;
/*  43 */   int _formDataIndex = 0;
/*     */ 
/*  45 */   private int _nextFormDataIndex = 0;
/*     */ 
/*  47 */   boolean _closed = false;
/*     */ 
/*  49 */   boolean _isFirstFormData = true;
/*     */ 
/*  51 */   boolean _prematureTermination = false;
/*     */   protected String _boundary;
/*     */   byte[] _separator;
/*     */   private WOCaseInsensitiveDictionary _multipartHeaders;
/*     */   protected WORequest _request;
/*     */   WONoCopyPushbackInputStream _bis;
/*  63 */   static byte[] dashDash = WOFileUploadSupport._bytesWithAsciiString("--");
/*     */ 
/*  65 */   static byte[] CRLF = WOFileUploadSupport._bytesWithAsciiString("\r\n");
/*     */ 
/*     */   public WOMultipartIterator(WORequest aRequest)
/*     */   {
/*  73 */     _multipartHeaders = new WOCaseInsensitiveDictionary();
/*     */ 
/*  75 */     _request = aRequest;
/*     */ 
/*  77 */     _formDataList = new LinkedList();
/*  78 */     _formDataStack = new LinkedList();
/*     */ 
/*  82 */     NSArray aContentHeaderArray = aRequest.headersForKey("content-type");
/*     */ 
/*  86 */     int i = 0; for (int count = aContentHeaderArray.count(); i < count; i++) {
/*  87 */       WOCaseInsensitiveDictionary tempHeaders = WOFileUploadSupport._parseOneHeader((String)aContentHeaderArray.objectAtIndex(i));
/*  88 */       _multipartHeaders.addEntriesFromDictionary(tempHeaders);
/*     */     }
/*  90 */     _boundary = ((String)_multipartHeaders.objectForKey("boundary"));
/*     */ 
/*  93 */     if ((aRequest.content() instanceof WOInputStreamData)) {
/*  94 */       WOInputStreamData aData = (WOInputStreamData)aRequest.content();
/*  95 */       InputStream is = aData.inputStream();
/*  96 */       if ((is != null) && ((is instanceof WONoCopyPushbackInputStream)))
/*     */       {
/*  98 */         _bis = ((WONoCopyPushbackInputStream)is);
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 103 */     if (_bis == null) {
				String contentLengthString = aRequest.headerForKey("x-content-length");
				long length = 0;
				try {
					if ((contentLengthString != null) && (contentLengthString.length() > 0))
						length = Long.parseLong(contentLengthString);
					
				} catch (NumberFormatException nfe) {
				}
				_bis = new WONoCopyPushbackInputStream(aRequest.content().stream(), length);
/* 106 */       // old: _bis = new WONoCopyPushbackInputStream(aRequest.content().stream(), aRequest._contentLengthHeader());
/*     */     }
/*     */ 
/* 109 */     _initSeparator();
/*     */   }
/*     */ 
/*     */   protected WOMultipartIterator()
/*     */   {
/*     */   }
/*     */ 
/*     */   public String boundary()
/*     */   {
/* 122 */     return _boundary;
/*     */   }
/*     */ 
/*     */   public NSDictionary multipartHeaders()
/*     */   {
/* 131 */     return _multipartHeaders;
/*     */   }
/*     */ 
/*     */   protected void _initSeparator()
/*     */   {
/* 140 */     if (_boundary == null)
/*     */     {
/*     */       try
/*     */       {
/* 149 */         byte[] firstBites = new byte[4];
/*     */ 
/* 151 */         int firstRead = _bis.read(firstBites);
/* 152 */         if (firstRead == 4)
/*     */         {
/* 155 */           int offset = 0;
/* 156 */           if ((firstBites[0] == CRLF[0]) && (firstBites[1] == CRLF[1]))
/*     */           {
/* 158 */             offset = 2;
/*     */           }
/* 160 */           if ((firstBites[offset] == dashDash[offset]) && (firstBites[(offset + 1)] == dashDash[(offset + 1)]))
/*     */           {
/* 163 */             byte[] nextBytes = new byte[1024];
/* 164 */             int nextRead = _bis.read(nextBytes);
/*     */ 
/* 167 */             NSData nextData = new NSData(nextBytes, new NSRange(0, nextRead), true);
/* 168 */             NSRange crlfRange = WOFileUploadSupport._rangeOfData(nextData, new NSData(CRLF));
/*     */ 
/* 170 */             if (crlfRange.length() > 0) {
/* 171 */               _boundary = _NSStringUtilities.stringForBytes(nextBytes, 0, crlfRange.location(), "US-ASCII");
/*     */ 
/* 173 */               _bis.unread(nextBytes, 0, nextRead);
/* 174 */               NSLog.err.appendln("Missing multipart boundary parameter; using \"" + _boundary + "\"");
/*     */             }
/*     */           }
/*     */         }
/*     */       }
/*     */       catch (Exception e)
/*     */       {
/* 181 */         NSLog.err.appendln("Exception while attempting to find missing boundary string: " + e);
/* 182 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
/* 183 */           NSLog.err.appendln(e);
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 188 */     if (_boundary != null)
/*     */     {
/* 190 */       _separator = WOFileUploadSupport._bytesWithAsciiString("\r\n--" + _boundary);
/*     */     }
/*     */ 
/* 193 */     if (_separator == null)
/* 194 */       _closed = true;
/*     */   }
/*     */ 
/*     */   public boolean didContentTerminatePrematurely()
/*     */   {
/* 206 */     return _prematureTermination;
/*     */   }
/*     */ 
/*     */   public int contentLengthRemaining()
/*     */   {
/* 217 */     return (int)_bis.theoreticallyAvailable();
/*     */   }

			public long contentLengthRemainingLong() {
				return _bis.theoreticallyAvailable();
			}

			public int _estimatedContentLength(int numFileUploads, int numNonFileUploads) {
				return (int) _estimatedContentLengthLong(numFileUploads, numNonFileUploads);
			}
/*     */ 
/*     */   public long _estimatedContentLengthLong(int numFileUploads, int numNonFileUploads)
/*     */   {
/* 224 */     long totalRemaining = _bis.originalReadMax();
/*     */ 
/* 226 */     long delimiterLength = _separator.length + 4;
/*     */ 
/* 228 */     long chaff = numFileUploads * (delimiterLength + 150) + numNonFileUploads * (delimiterLength + 50 + 10) + delimiterLength;
/*     */ 
/* 230 */     return totalRemaining - chaff;
/*     */   }
/*     */ 
/*     */   public WOFormData nextFormData()
/*     */   {
/* 245 */     WOFormData bodyPart = null;
/*     */ 
/* 249 */     bodyPart = _nextFormDataInList();
/*     */ 
/* 251 */     if (bodyPart == null) {
/* 252 */       if (_closed) {
/* 253 */         return null;
/*     */       }
/*     */ 
/* 258 */       _invalidateFormData(_currentFormData());
/*     */ 
/* 262 */       bodyPart = _nextFormData();
/*     */ 
/* 265 */       _addFormData(bodyPart);
/*     */ 
/* 268 */       _nextFormDataIndex += 1;
/*     */     }
/*     */ 
/* 271 */     return bodyPart;
/*     */   }
/*     */ 
/*     */   protected void _invalidateFormData(WOFormData data)
/*     */   {
/* 276 */     if (data != null)
/* 277 */       data._invalidate();
/*     */   }
/*     */ 
/*     */   protected WOFormData _currentFormData()
/*     */   {
/* 283 */     if (_formDataList.size() > 0) {
/* 284 */       WOFormData current = (WOFormData)_formDataList.getLast();
/* 285 */       if (current != null) {
/* 286 */         return current;
/*     */       }
/*     */     }
/* 289 */     return null;
/*     */   }
/*     */ 
/*     */   protected WOFormData _nextFormData()
/*     */   {
/* 295 */     if (_closed) {
/* 296 */       return null;
/*     */     }
/* 298 */     WOFormData bodyPart = null;
/*     */ 
/* 300 */     if (_formDataStack.size() > 0) {
/* 301 */       bodyPart = (WOFormData)_formDataStack.getFirst();
/* 302 */       _formDataStack.removeFirst();
/*     */     }
/*     */     else {
/* 305 */       bodyPart = new WOFormData();
/*     */ 
/* 308 */       if (bodyPart._isTheLast) {
/* 309 */         _closed = true;
/* 310 */         bodyPart = null;
/*     */       }
/*     */       else {
/*     */         try {
/* 314 */           if (bodyPart.isFileUpload())
/*     */           {
/* 316 */             bodyPart._legacyFormValues(_request._formValues());
/*     */           }
/*     */           else
/* 319 */             bodyPart._addToFormValues(_request._formValues());
/*     */         }
/*     */         catch (IOException ioe) {
/* 322 */           NSLog.err.appendln("Failed to create WOFormData: " + ioe);
/* 323 */           if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L)) {
/* 324 */             NSLog.err.appendln(ioe);
/*     */           }
/* 326 */           _closed = true;
/* 327 */           bodyPart = null;
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 332 */     return bodyPart;
/*     */   }
/*     */ 
/*     */   private WOFormData _nextFormDataInList()
/*     */   {
/* 340 */     int size = _formDataList.size();
/* 341 */     if ((size > 0) && (size > _nextFormDataIndex)) {
/* 342 */       return (WOFormData)_formDataList.get(_nextFormDataIndex++);
/*     */     }
/* 344 */     return null;
/*     */   }
/*     */ 
/*     */   protected void _pushFormData(WOFormData newData)
/*     */   {
/* 350 */     if (newData != null)
/* 351 */       _formDataStack.addFirst(newData);
/*     */   }
/*     */ 
/*     */   protected void _addFormData(WOFormData newData)
/*     */   {
/* 357 */     if (newData != null)
/* 358 */       _formDataList.addLast(newData);
/*     */   }
/*     */ 
/*     */   public class WOFormData
/*     */   {
/*     */     NSDictionary _headers;
/*     */     _WOFormDataInputStream _fdstream;
/*     */     NSData _data;
/*     */     NSDictionary _cdHeaders;
/*     */     NSData _cdData;
/* 379 */     int _index = 0;
/*     */ 
/* 381 */     boolean _isTheLast = false;
/*     */ 
/* 383 */     boolean _isFileUpload = false;
/*     */ 
/* 385 */     boolean _streamWasCalled = false;
/*     */ 
/* 387 */     boolean _dataWasCalled = false;
/*     */ 
/* 608 */     String _formValueString = null;
/*     */ 
/*     */     protected WOFormData()
/*     */     {
/* 392 */       _headers = null;
/* 393 */       _cdHeaders = null;
/*     */ 
/* 397 */       _index = WOMultipartIterator.this._formDataIndex;
/* 398 */       WOMultipartIterator.this._formDataIndex += 1;
/*     */ 
/* 400 */       _initHeaders();
/* 401 */       WOMultipartIterator.this._isFirstFormData = false;
/*     */     }
/*     */ 
/*     */     protected WOFormData(String filePath)
/*     */     {
/*     */     }
/*     */ 
/*     */     private void _initHeaders()
/*     */     {
/*     */       try
/*     */       {
/* 413 */         WOHttpIO anHttpIo = new WOHttpIO();
/*     */ 
/* 415 */         if (WOMultipartIterator.this._isFirstFormData)
/*     */         {
/* 417 */           int lineLength = 0; int sanityCheck = 0;
/* 418 */           while ((lineLength == 0) && (sanityCheck < 5)) {
/* 419 */             lineLength = anHttpIo.readLine(WOMultipartIterator.this._bis);
/* 420 */             sanityCheck++;
/*     */           }
/* 422 */           if (sanityCheck == 5) {
/* 423 */             _isTheLast = true;
/* 424 */             return;
/*     */           }
/*     */         }
/*     */         else {
/* 428 */           byte[] firstBites = new byte[2];
/*     */ 
/* 433 */           int read = WOMultipartIterator.this._bis.read(firstBites);
/* 434 */           if ((read < 2) || ((firstBites[0] == WOMultipartIterator.dashDash[0]) && (firstBites[1] == WOMultipartIterator.dashDash[1])))
/*     */           {
/* 436 */             _isTheLast = true;
/* 437 */             return;
/* 438 */           }if ((firstBites[0] != WOMultipartIterator.CRLF[0]) || (firstBites[1] != WOMultipartIterator.CRLF[1]))
/*     */           {
/* 441 */             if (firstBites[0] == WOMultipartIterator.CRLF[1])
/*     */             {
/* 445 */               WOMultipartIterator.this._bis.unread(firstBites, 1, 1);
/*     */             }
/* 447 */             else WOMultipartIterator.this._bis.unread(firstBites);
/*     */           }
/*     */ 
/*     */         }
/*     */ 
/* 452 */         anHttpIo._readHeaders(WOMultipartIterator.this._bis, false, false, true);
/* 453 */         _headers = anHttpIo.headers();
/*     */ 
/* 455 */         WOHTTPHeadersDictionary _headerDict = (WOHTTPHeadersDictionary)_headers;
/* 456 */         WOCaseInsensitiveDictionary valueDict = null;
/*     */ 
/* 461 */         NSArray values = (NSArray)_headerDict._realObjectForKey("content-disposition");
/*     */ 
/* 463 */         if ((values != null) && (values.count() > 0)) {
/* 464 */           if ((values.objectAtIndex(0) instanceof WOHTTPHeaderValue))
/*     */           {
/* 466 */             NSData headerData = ((WOHTTPHeaderValue)values.objectAtIndex(0))._data();
/*     */ 
/* 469 */             valueDict = WOFileUploadSupport._parseContentDispositionHeader(WOMultipartIterator.this._request, null, _headerDict, null, headerData);
/*     */           }
/*     */ 
/* 473 */           values = (NSArray)_headerDict.objectForKey("content-disposition");
/*     */ 
/* 476 */           if (valueDict == null) {
/* 477 */             valueDict = WOFileUploadSupport._parseOneHeader((String)values.objectAtIndex(0));
/*     */           }
/*     */ 
/* 480 */           _cdHeaders = valueDict;
/* 481 */           _headerDict.setObjectForKey(new NSArray(valueDict), "content-disposition");
/*     */ 
/* 484 */           _isFileUpload = (_cdHeaders.objectForKey("filename") != null);
/*     */         }
/*     */       } catch (IOException ioe) {
/* 487 */         NSLog.err.appendln("Failed to create WOFormData " + ioe);
/* 488 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L))
/* 489 */           NSLog.err.appendln(ioe);
/*     */       }
/*     */     }
/*     */ 
/*     */     public boolean isFileUpload()
/*     */     {
/* 500 */       return _isFileUpload;
/*     */     }
/*     */ 
/*     */     public NSDictionary headers()
/*     */     {
/* 512 */       return _headers;
/*     */     }
/*     */ 
/*     */     public NSDictionary contentDispositionHeaders()
/*     */     {
/* 524 */       return _cdHeaders;
/*     */     }
/*     */ 
/*     */     public String name()
/*     */     {
/* 533 */       return (String)_cdHeaders.objectForKey("name");
/*     */     }
/*     */ 
/*     */     public InputStream formDataInputStream()
/*     */     {
/* 545 */       if (_isTheLast)
/* 546 */         return null;
/* 547 */       if (_dataWasCalled) {
/* 548 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(3, 4L)) {
/* 549 */           NSLog.debug.appendln("<WOFormData>: formDataInputStream() called after accessing formData()");
/*     */         }
/* 551 */         return null;
/*     */       }
/* 553 */       _streamWasCalled = true;
/*     */ 
/* 555 */       if (_fdstream == null) {
/* 556 */         _fdstream = new _WOFormDataInputStream();
/*     */       }
/* 558 */       return _fdstream;
/*     */     }
/*     */ 
/*     */     public NSData formData()
/*     */       throws IOException
/*     */     {
/* 565 */       return formData(4096);
/*     */     }
/*     */ 
/*     */     public NSData formData(int bufferSize)
/*     */       throws IOException
/*     */     {
/* 577 */       if (_isTheLast)
/* 578 */         return null;
/* 579 */       if (_streamWasCalled) {
/* 580 */         if (NSLog.debugLoggingAllowedForLevelAndGroups(3, 4L)) {
/* 581 */           NSLog.debug.appendln("<WOFormData>: formData() called after accessing formDataInputStream()");
/*     */         }
/* 583 */         return null;
/*     */       }
/* 585 */       _dataWasCalled = true;
/*     */ 
/* 587 */       if (_data == null) {
/* 588 */         _fdstream = new _WOFormDataInputStream();
/* 589 */         _data = new NSData(_fdstream, bufferSize);
/*     */       }
/* 591 */       return _data;
/*     */     }
/*     */ 
/*     */     public boolean isStreamAvailable()
/*     */     {
/* 604 */       return !_dataWasCalled;
/*     */     }
/*     */ 
/*     */     public String formValue()
/*     */       throws IOException
/*     */     {
/* 620 */       if (_formValueString == null) {
/* 621 */         _formValueString = WOFileUploadSupport._getFormValuesFromData(WOMultipartIterator.this._request, null, formData(), name());
/*     */       }
/* 623 */       return _formValueString;
/*     */     }
/*     */ 
/*     */     protected void _addToFormValues(NSMutableDictionary formValues)
/*     */       throws IOException
/*     */     {
/* 629 */       WOFileUploadSupport._getFormValuesFromData(WOMultipartIterator.this._request, formValues, formData(), name());
/*     */     }
/*     */ 
/*     */     protected void _legacyFormValues(NSMutableDictionary aFormValues)
/*     */       throws IOException
/*     */     {
/* 636 */       String key = (String)_cdHeaders.objectForKey("name");
/*     */ 
/* 640 */       if (key != null) {
/* 641 */         String aFilename = (String)_cdHeaders.objectForKey("filename");
/* 642 */         if (aFilename != null) {
/* 643 */           String newKey = key + "." + "filename";
/*     */ 
/* 645 */           NSMutableArray valueArray = (NSMutableArray)aFormValues.objectForKey(newKey);
/* 646 */           if (valueArray != null)
/* 647 */             valueArray.addObject(aFilename);
/*     */           else {
/* 649 */             aFormValues.setObjectForKey(new NSMutableArray(aFilename), newKey);
/*     */           }
/*     */ 
/*     */         }
/*     */ 
/* 654 */         String contentType = null;
/* 655 */         NSArray contentArray = (NSArray)_headers.objectForKey("content-type");
/* 656 */         if ((contentArray != null) && (contentArray.count() > 0)) {
/* 657 */           contentType = (String)contentArray.objectAtIndex(0);
/*     */         }
/* 659 */         if (contentType != null) {
/* 660 */           String newKey = key + "." + "mimetype";
/*     */ 
/* 662 */           NSMutableArray valueArray = (NSMutableArray)aFormValues.objectForKey(newKey);
/* 663 */           if (valueArray != null)
/* 664 */             valueArray.addObject(contentType);
/*     */           else {
/* 666 */             aFormValues.setObjectForKey(new NSMutableArray(contentType), newKey);
/*     */           }
/*     */         }
/*     */ 
/* 670 */         NSMutableArray valueArray = (NSMutableArray)aFormValues.objectForKey(key);
/* 671 */         InputStream fdStream = formDataInputStream();
/* 672 */         NSData aBodyData = new WOInputStreamData(fdStream, 0);
/*     */ 
/* 674 */         if (valueArray != null)
/* 675 */           valueArray.addObject(aBodyData);
/*     */         else
/* 677 */           aFormValues.setObjectForKey(new NSMutableArray(aBodyData), key);
/*     */       }
/*     */     }
/*     */ 
/*     */     public Number numericFormValue(NSNumberFormatter numericFormatter)
/*     */       throws IOException
/*     */     {
/* 693 */       String numberString = formValue();
/* 694 */       Number number = null;
/*     */ 
/* 696 */       if ((numberString != null) && (numericFormatter != null)) {
/*     */         try {
/* 698 */           number = (Number)numericFormatter.parseObject(numberString);
/*     */         } catch (ParseException e) {
/* 700 */           if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 128L)) {
/* 701 */             NSLog.err.appendln(e);
/*     */           }
/*     */         }
/*     */       }
/* 705 */       return number;
/*     */     }
/*     */ 
/*     */     public NSTimestamp dateFormValue(NSTimestampFormatter dateFormatter)
/*     */       throws IOException
/*     */     {
/* 719 */       String aDateString = formValue();
/* 720 */       NSTimestamp aDate = null;
/*     */ 
/* 722 */       if ((aDateString != null) && (dateFormatter != null)) {
/*     */         try {
/* 724 */           aDate = (NSTimestamp)dateFormatter.parseObject(aDateString);
/*     */         } catch (ParseException e) {
/* 726 */           if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 128L)) {
/* 727 */             NSLog.err.appendln(e);
/*     */           }
/*     */         }
/*     */       }
/* 731 */       return aDate;
/*     */     }
/*     */ 
/*     */     void _invalidate()
/*     */     {
/* 738 */       if ((_isFileUpload) || (_streamWasCalled))
/*     */       {
/* 741 */         if (_fdstream == null)
/* 742 */           _fdstream = new _WOFormDataInputStream();
/*     */         try {
/* 744 */           _fdstream.close();
/*     */         } catch (IOException ioe) {
/* 746 */           NSLog.err.appendln("WOFormData failed to skip past data: " + ioe);
/* 747 */           if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L))
/* 748 */             NSLog.err.appendln(ioe);
/*     */         }
/*     */       }
/*     */       else
/*     */       {
/*     */         try {
/* 754 */           formData();
/*     */         } catch (IOException ioe) {
/* 756 */           NSLog.err.appendln("WOFormData failed to read data: " + ioe);
/* 757 */           if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L))
/* 758 */             NSLog.err.appendln(ioe);
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */     public boolean isStreamValid()
/*     */     {
/* 770 */       if (_fdstream != null) {
/* 771 */         return !_fdstream.isClosed();
/*     */       }
/* 773 */       return false;
/*     */     }
/*     */ 
/*     */     public String toString()
/*     */     {
/* 783 */       if (_isTheLast) {
/* 784 */         return "<WOFormData>: This WOFormData represents the end of the multipart form data";
/*     */       }
/* 786 */       return "WOFormData " + _index + " isStreamValid " + isStreamValid() + " headers: " + _headers;
/*     */     }
/*     */ 
/*     */     protected class _WOFormDataInputStream extends InputStream
/*     */     {
/*     */       private boolean _streamClosed;
/*     */       private byte[] _oneByteArray;
/*     */       private byte[] _drainBuffer;
/* 799 */       private int _drainBufferLength = 4096;
/*     */ 
/*     */       protected _WOFormDataInputStream()
/*     */       {
/* 803 */         _streamClosed = false;
/* 804 */         _oneByteArray = new byte[1];
/*     */       }
/*     */ 
/*     */       public int available()
/*     */       {
/* 812 */         if (_streamClosed)
/* 813 */           return -1;
/* 814 */         return 0;
/*     */       }
/*     */ 
/*     */       public void close()
/*     */         throws IOException
/*     */       {
/* 824 */         while (!_streamClosed)
/* 825 */           skip(9223372036854775807L);
/*     */       }
/*     */ 
/*     */       public int read()
/*     */         throws IOException
/*     */       {
/* 834 */         int read = read(_oneByteArray);
/* 835 */         if (read == -1)
/* 836 */           return read;
/* 837 */         return _oneByteArray[0];
/*     */       }
/*     */ 
/*     */       public int read(byte[] b)
/*     */         throws IOException
/*     */       {
/* 845 */         return read(b, 0, b.length);
/*     */       }
/*     */ 
/*     */       public int read(byte[] b, int off, int len)
/*     */         throws IOException
/*     */       {
/* 853 */         if (b == null)
/* 854 */           throw new IllegalArgumentException("<" + getClass().getName() + ">: buffer passed is null!");
/* 855 */         if (len == 0)
/* 856 */           return 0;
/* 857 */         if ((off < 0) || (len < 0) || (off + len > b.length))
/* 858 */           throw new IndexOutOfBoundsException("<" + getClass().getName() + ">: attempted to read " + len + " bytes into buffer of length " + b.length + " at offset " + off);
/* 859 */         if (_streamClosed) {
/* 860 */           return -1;
/*     */         }
/* 862 */         int bytesRead = 0;
/*     */         try
/*     */         {
/* 865 */           int charsMatched = 0;
/* 866 */           int separatorLength = WOMultipartIterator.this._separator.length;
/*     */ 
/* 868 */           bytesRead = WOMultipartIterator.this._bis.read(b, off, len);
/*     */ 
/* 870 */           for (int i = 0; i < bytesRead; i++) {
/* 871 */             charsMatched = 0;
/* 872 */             while ((i + charsMatched < bytesRead) && (charsMatched < separatorLength) && (b[(off + i + charsMatched)] == WOMultipartIterator.this._separator[charsMatched])) {
/* 873 */               charsMatched++;
/*     */             }
/*     */ 
/* 877 */             if (charsMatched == separatorLength)
/*     */             {
/* 881 */               _streamClosed = true;
/*     */ 
/* 884 */               int pbsize = bytesRead - i - separatorLength;
/*     */ 
/* 886 */               if (pbsize > 0) {
/* 887 */                 byte[] tempB = new byte[pbsize];
/* 888 */                 System.arraycopy(b, i + off + separatorLength, tempB, 0, pbsize);
/* 889 */                 WOMultipartIterator.this._bis.unread(tempB);
/*     */               }
/*     */ 
/* 892 */               return i;
/* 893 */             }if (i + charsMatched != bytesRead)
/*     */             {
/*     */               continue;
/*     */             }
/* 897 */             byte[] tempB = new byte[separatorLength - charsMatched];
/*     */ 
/* 899 */             int readB = 0;
/*     */             do {
/* 901 */               int justRead = WOMultipartIterator.this._bis.read(tempB, readB, tempB.length - readB);
/* 902 */               if (justRead == -1) {
/*     */                 break;
/*     */               }
/* 905 */               readB += justRead;
/* 906 */             }while (readB < tempB.length);
/*     */ 
/* 909 */             int bIndex = 0;
/*     */ 
/* 911 */             while ((bIndex < readB) && (charsMatched < separatorLength)) {
/* 912 */               if (tempB[(bIndex++)] == WOMultipartIterator.this._separator[(charsMatched++)]) {
/*     */                 continue;
/*     */               }
/* 915 */               WOMultipartIterator.this._bis.unread(tempB, 0, readB);
/*     */ 
/* 917 */               return bytesRead;
/*     */             }
/*     */ 
/* 922 */             _streamClosed = true;
/*     */ 
/* 924 */             if (charsMatched == separatorLength)
/*     */             {
/* 927 */               return i;
/* 928 */             }if (bIndex == readB)
/*     */             {
/* 930 */               return bytesRead;
/*     */             }
/*     */           }
/*     */         }
/*     */         catch (IOException ioe) {
/* 935 */           if (WOMultipartIterator.this._bis.wasPrematurelyTerminated()) {
/* 936 */             WOMultipartIterator.this._closed = true;
/* 937 */             WOMultipartIterator.this._prematureTermination = true;
/*     */           }
/* 939 */           throw ioe;
/*     */         }
/* 941 */         return bytesRead;
/*     */       }
/*     */ 
/*     */       public long skip(long n)
/*     */         throws IOException
/*     */       {
/* 949 */         if (_drainBuffer == null) {
/* 950 */           _drainBuffer = new byte[_drainBufferLength];
/*     */         }
/*     */ 
/* 953 */         long left = n;
/* 954 */         int count = 0;
/*     */         try
/*     */         {
/* 957 */           while (left > 0L) {
/* 958 */             count = read(this._drainBuffer, 0, (int)(left > _drainBufferLength ? _drainBufferLength : left));
/* 959 */             if (count == -1) {
/* 960 */               _streamClosed = true;
/* 961 */               return n - left;
/*     */             }
/* 963 */             left -= count;
/*     */           }
/*     */         } catch (IOException ioe) {
/* 966 */           if (WOMultipartIterator.this._bis.wasPrematurelyTerminated()) {
/* 967 */             WOMultipartIterator.this._closed = true;
/* 968 */             WOMultipartIterator.this._prematureTermination = true;
/*     */           }
/* 970 */           throw ioe;
/*     */         }
/* 972 */         return n;
/*     */       }
/*     */ 
/*     */       public boolean isClosed()
/*     */       {
/* 979 */         return _streamClosed;
/*     */       }
/*     */     }
/*     */   }
/*     */ }

