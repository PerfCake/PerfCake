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

import java.io.File;
import java.io.FileNotFoundException;
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
 * FileQueue is persistent queue, which stores its items to specified file
 * 
 * @author Pavel Drozd <ravliv7@gmail.com>
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

   public FileQueue(final String filename) {
      try {
         boolean fileExists = new File(filename).exists();
         file = new RandomAccessFile(filename, "rw");
         if (fileExists) {
            queueSize = file.readLong();
            pointer = file.readLong();
         } else {
            file.writeLong(queueSize);
            file.writeLong(pointer);
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
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
            ObjectOutputStream s = new ObjectOutputStream(outputStream);
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
      } catch (IOException e) {
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
      } catch (IOException e) {
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
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean addAll(final Collection<? extends T> c) {
      for (T item : c) {
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
      T item = poll();
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
               T result = getItem();
               pointer = channel.position();
               file.seek(0);
               if (pointer == channel.size()) {
                  clear();
               }
               queueSize--;
               file.writeLong(queueSize);
               file.writeLong(pointer);
               return result;
            }
         }
         return null;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public T element() {
      return getItem();
   }

   /**
    * Returns first item in queue
    * 
    * @return
    */
   @SuppressWarnings("unchecked")
   protected T getItem() {
      try {
         synchronized (this) {
            channel.position(pointer);
            return (T) new ObjectInputStream(inputStream).readObject();
         }
      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
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
   public <T> T[] toArray(final T[] a) {
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
