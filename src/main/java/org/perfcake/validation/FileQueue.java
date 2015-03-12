/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.validation;

import org.perfcake.PerfCakeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Stores items persistently to specified file.
 *
 * @author <a href="mailto:ravliv7@gmail.com">Pavel Drozd</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FileQueue<T extends Serializable> implements Queue<T> {

   public static final int EMPTY_POINTER = -1;

   public static final int LONG_SIZE = 8;

   public static final int HEADER_SIZE = LONG_SIZE * 2;

   private final InputStream inputStream;

   private final OutputStream outputStream;

   private RandomAccessFile file;

   private final FileChannel channel;

   private long pointer = EMPTY_POINTER;

   private long queueSize = 0;

   /**
    * Creates a new {@link org.perfcake.validation.FileQueue} that stores items in a file on a specified path.
    *
    * @param filename
    *       Path to the file to store queue items.
    * @throws org.perfcake.PerfCakeException
    *       When it was not possible to create or open the file.
    */
   public FileQueue(final String filename) throws PerfCakeException {
      this(new File(filename));
   }

   /**
    * Creates a new {@link org.perfcake.validation.FileQueue} that stores items in a specified file.
    *
    * @param queueFile
    *       File to store queue items.
    * @throws org.perfcake.PerfCakeException
    *       When it was not possible to create or open the file.
    */
   public FileQueue(final File queueFile) throws PerfCakeException {
      try {
         final boolean fileExists = queueFile.exists();
         file = new RandomAccessFile(queueFile.getAbsolutePath(), "rw");
         if (fileExists && file.length() > 0) {
            queueSize = file.readLong();
            pointer = file.readLong();
         } else {
            file.writeLong(queueSize);
            file.writeLong(pointer);
         }
      } catch (final RuntimeException | IOException e) {
         throw new PerfCakeException("Cannot create and open a file queue: ", e);
      }

      channel = file.getChannel();
      inputStream = Channels.newInputStream(channel);
      outputStream = Channels.newOutputStream(channel);
   }

   @Override
   public boolean add(final T item) {
      try {
         synchronized (this) {
            channel.position(channel.size());
            // write object to file
            final ObjectOutputStream s = new ObjectOutputStream(outputStream);
            s.writeObject(item);
            s.flush();

            queueSize++;
            file.seek(0);
            file.writeLong(queueSize);
            if (pointer == EMPTY_POINTER) {
               pointer = HEADER_SIZE;
               file.writeLong(pointer);
            }
            return true;
         }
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public synchronized int size() {
      return (int) queueSize;
   }

   @Override
   public T peek() {
      synchronized (this) {
         if (pointer != EMPTY_POINTER) {
            return getItem();
         }
      }
      return null;
   }

   @Override
   public synchronized void clear() {
      try {
         pointer = EMPTY_POINTER;
         queueSize = 0;
         file.setLength(HEADER_SIZE);
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean isEmpty() {
      try {
         synchronized (this) {
            file.seek(LONG_SIZE);
            pointer = file.readLong();
            return pointer == EMPTY_POINTER;
         }
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean addAll(final Collection<? extends T> c) {
      for (final T item : c) {
         add(item);
      }
      return true;
   }

   @Override
   public boolean offer(final T e) {
      return add(e);
   }

   @Override
   public T remove() {
      final T item = poll();
      if (item != null) {
         return item;
      }
      throw new NoSuchElementException();
   }

   @Override
   public T poll() {
      try {
         synchronized (this) {
            if (pointer != EMPTY_POINTER) {
               final T result = getItem();
               pointer = channel.position();
               file.seek(0);
               if (pointer == channel.size()) {
                  clear();
               } else {
                  queueSize--;
               }
               file.writeLong(queueSize);
               file.writeLong(pointer);
               return result;
            }
         }
         return null;
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public T element() {
      return getItem();
   }

   /**
    * Gets the first item in queue.
    *
    * @return The first item in queue.
    */
   @SuppressWarnings("unchecked")
   protected T getItem() {
      try {
         synchronized (this) {
            channel.position(pointer);
            return (T) new ObjectInputStream(inputStream).readObject();
         }
      } catch (final ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean contains(final Object o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Iterator<T> iterator() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object[] toArray() {
      throw new UnsupportedOperationException();
   }

   @Override
   public <U> U[] toArray(final U[] a) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean remove(final Object o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean containsAll(final Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean removeAll(final Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean retainAll(final Collection<?> c) {
      throw new UnsupportedOperationException();
   }
}
