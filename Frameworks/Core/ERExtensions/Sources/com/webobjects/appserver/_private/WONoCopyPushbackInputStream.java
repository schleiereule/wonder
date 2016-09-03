/*
 * $Id: WONoCopyPushbackInputStream.java,v 1.1.2.2 2012/07/29 10:45:31 helmut Exp $
 * JD-Core Version:    0.6.0
 */

/*     */ package com.webobjects.appserver._private;
/*     */ 
/*     */ import java.io.FilterInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.LinkedList;
/*     */ import java.util.ListIterator;
/*     */ 
/*     */ public class WONoCopyPushbackInputStream extends FilterInputStream
/*     */ {
/*     */   LinkedList<PushbackBuffer> buffers;
/*     */   byte[] oneByteArray;
/*     */   long readMax;
/*     */   long originalReadMax;
/*  36 */   boolean prematureTermination = false;
/*     */ 
/*     */   private void ensureOpen()
/*     */     throws IOException
/*     */   {
/*  42 */     if (in == null)
/*  43 */       throw new IOException("Stream closed");
/*     */   }
/*     */ 
/*     */   public WONoCopyPushbackInputStream(InputStream ins, long maxBytes)
/*     */   {
/*  54 */     super(ins);
/*  55 */     buffers = new LinkedList();
/*  56 */     oneByteArray = new byte[1];
/*     */ 
/*  62 */     readMax = (maxBytes < 0 ? 0 : maxBytes);
/*  63 */     originalReadMax = readMax;
/*     */   }
/*     */ 
/*     */   public int read()
/*     */     throws IOException
/*     */   {
/*  80 */     int read = read(oneByteArray);
/*  81 */     if (read == -1)
/*  82 */       return read;
/*  83 */     return oneByteArray[0];
/*     */   }
/*     */ 
/*     */   public int read(byte[] b)
/*     */     throws IOException
/*     */   {
/*  91 */     return read(b, 0, b.length);
/*     */   }
/*     */ 
/*     */   public int read(byte[] buffer, int offset, int length)
/*     */     throws IOException
/*     */   {
/* 111 */     ensureOpen();
/* 112 */     int off = offset;
/* 113 */     int len = length;
/* 114 */     if ((off < 0) || (off > buffer.length) || (len < 0) || (off + len > buffer.length) || (off + len < 0)) {
/* 115 */       throw new IndexOutOfBoundsException();
/*     */     }
/*     */ 
/* 118 */     if (len == 0) {
/* 119 */       return 0;
/*     */     }
/*     */ 
/* 122 */     int avail = 0;
/* 123 */     PushbackBuffer pb = null;
/* 124 */     ListIterator li = buffers.listIterator(0);
/*     */ 
/* 127 */     while ((li.hasNext()) && (len > 0)) {
/* 128 */       pb = (PushbackBuffer)li.next();
/* 129 */       long pbLen = pb.length;
/*     */ 
/* 131 */       if (len < pbLen) {
/* 132 */         pbLen = len;
/*     */       }
/* 134 */       System.arraycopy(pb.buf, (int)pb.pos, buffer, off, (int)pbLen);
/* 135 */       avail += pbLen;
/*     */ 
/* 137 */       pb.pos += pbLen;
/* 138 */       pb.length -= pbLen;
/*     */ 
/* 140 */       off += pbLen;
/* 141 */       len -= pbLen;
/*     */ 
/* 144 */       if (pb.length == 0) {
/* 145 */         li.remove();
/*     */       }
/*     */     }
/* 148 */     if (len > 0)
/*     */     {
/* 150 */       if (readMax <= 0) {
/* 151 */         if (avail > 0) {
/* 152 */           return avail;
/*     */         }
/* 154 */         return -1;
/*     */       }
/*     */ 
/* 158 */       if (len > readMax) {
/* 159 */         len = (int)readMax;
/*     */       }
/*     */ 
/*     */       try
/*     */       {
/* 164 */         len = super.read(buffer, off, len);
/*     */       } catch (IOException ioe) {
/* 166 */         prematureTermination = true;
/* 167 */         throw ioe;
/*     */       }
/* 169 */       if (len == -1) {
/* 170 */         if (avail == 0) {
/* 171 */           if (readMax > 0) {
/* 172 */             prematureTermination = true;
/*     */ 
/* 174 */             throw new IOException("Connection reset by peer: Amount read didn't match content-length");
/*     */           }
/* 176 */           return -1;
/*     */         }
/* 178 */         return avail;
/*     */       }
/*     */ 
/* 181 */       readMax -= len;
/* 182 */       return avail + len;
/*     */     }
/* 184 */     return avail;
/*     */   }
/*     */ 
/*     */   public void unread(byte[] b, int off, int len)
/*     */     throws IOException
/*     */   {
/* 200 */     ensureOpen();
/* 201 */     if ((off < 0) || (off > b.length) || (len < 0) || (off + len > b.length) || (off + len < 0)) {
/* 202 */       throw new IndexOutOfBoundsException();
/*     */     }
/* 204 */     if (len == 0)
/* 205 */       return;
/* 206 */     PushbackBuffer pb = new PushbackBuffer(b, off, len);
/* 207 */     buffers.addFirst(pb);
/*     */   }
/*     */ 
/*     */   public void unread(byte[] b)
/*     */     throws IOException
/*     */   {
/* 214 */     unread(b, 0, b.length);
/*     */   }
/*     */ 
/*     */   public int available()
/*     */     throws IOException
/*     */   {
/* 228 */     ensureOpen();
/* 229 */     int avail = 0;
/* 230 */     ListIterator li = buffers.listIterator(0);
/* 231 */     while (li.hasNext()) {
/* 232 */       avail += ((PushbackBuffer)li.next()).length;
/*     */     }
/* 234 */     return avail + super.available();
/*     */   }
/*     */ 
/*     */   public long theoreticallyAvailable()
/*     */   {
/* 247 */     long avail = 0;
/* 248 */     ListIterator li = buffers.listIterator(0);
/* 249 */     while (li.hasNext()) {
/* 250 */       avail += ((PushbackBuffer)li.next()).length;
/*     */     }
/* 252 */     return avail + readMax;
/*     */   }
/*     */ 
/*     */   public long skip(long numberOfBytesToSkip)
/*     */     throws IOException
/*     */   {
/* 270 */     ensureOpen();
/* 271 */     long n = numberOfBytesToSkip;
/* 272 */     if (n <= 0L) {
/* 273 */       return 0L;
/*     */     }
/*     */ 
/* 276 */     long pskip = 0L;
/* 277 */     PushbackBuffer pb = null;
/* 278 */     ListIterator li = buffers.listIterator(0);
/*     */ 
/* 281 */     while ((li.hasNext()) && (n > 0L)) {
/* 282 */       pb = (PushbackBuffer)li.next();
/* 283 */       long pbLen = pb.length;
/*     */ 
/* 285 */       if (n < pbLen) {
/* 286 */         pbLen = n;
/*     */       }
/* 288 */       n -= pbLen;
/* 289 */       pskip += pbLen;

				pb.pos += pbLen;
				pb.length -= pbLen;
/*     */ 
/* 295 */       if (pb.length == 0) {
/* 296 */         li.remove();
/*     */       }
/*     */     }
/* 299 */     if (n > 0L) {
/* 300 */       if (n > readMax) {
/* 301 */         n = readMax;
/*     */       }
/* 303 */       long superSkip = 0L;
/*     */       try {
/* 305 */         superSkip = super.skip(n);
/*     */       } catch (IOException ioe) {
/* 307 */         prematureTermination = true;
/* 308 */         throw ioe;
/*     */       }
/* 310 */       if (superSkip == -1L) {
/* 311 */         if (pskip == 0L) {
/* 312 */           if (readMax > 0) {
/* 313 */             prematureTermination = true;
/*     */ 
/* 315 */             throw new IOException("Connection reset by peer: Amount read didn't match content-length");
/*     */           }
/*     */         }
/* 318 */         else return pskip;
/*     */       }
/*     */ 
/* 321 */       readMax = (int)(readMax - superSkip);
/* 322 */       pskip += superSkip;
/*     */     }
/* 324 */     return pskip;
/*     */   }
/*     */ 
/*     */   public boolean markSupported()
/*     */   {
/* 336 */     return false;
/*     */   }
/*     */ 
/*     */   public synchronized void close()
/*     */     throws IOException
/*     */   {
/* 347 */     if (in != null) {
/* 348 */       in.close();
/* 349 */       in = null;
/*     */     }
/* 351 */     buffers = null;
/*     */   }
/*     */ 
/*     */   public void drain()
/*     */     throws IOException
/*     */   {
/* 358 */     buffers = null;
/* 359 */     long toRead = readMax;
/* 360 */     int read = 0;
/* 361 */     byte[] drainBuffer = new byte[2048];
/*     */ 
/* 363 */     while (toRead > 0L) {
/* 364 */       read = in.read(drainBuffer);
/* 365 */       if (read == -1)
/*     */         break;
/* 367 */       toRead -= read;
/*     */     }
/*     */   }
/*     */ 
/*     */   public long readMax()
/*     */   {
/* 375 */     return readMax;
/*     */   }
/*     */ 
/*     */   public long originalReadMax()
/*     */   {
/* 382 */     return originalReadMax;
/*     */   }
/*     */ 
/*     */   public boolean wasPrematurelyTerminated()
/*     */   {
/* 389 */     return prematureTermination;
/*     */   }
/*     */ 
/*     */   class PushbackBuffer
/*     */   {
/*     */     byte[] buf;
/*     */     long pos;
/*     */     long length;
/*     */ 
/*     */     PushbackBuffer(byte[] b, long p, long l)
/*     */     {
/*  24 */       buf = b;
/*  25 */       pos = p;
/*  26 */       length = l;
/*     */     }
/*     */   }
/*     */ }
