/*
 * JBoss, a division of Red Hat
 * Copyright 2011, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.api.util;

import org.gatein.api.id.Id;
import org.gatein.api.id.Identifiable;

/**
 * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
 * @version $Revision$
 */
public interface Container<K, T extends Identifiable>
{
   IterableResult<T> getAll();

   <U extends T> IterableResult<U> getAllSatisfying(Query<U> query);

   <U extends T> IterableResult<U> getAllWhere(Filter<U> filter);

   int size();

   boolean contains(K key);

   boolean contains(Id<T> id);

   T createAndAdd(K key);

   T createAndAdd(Id<T> id);

   T get(K key);

   T get(Id<T> id);

   Id<T> getIdForChild(K key);
}
