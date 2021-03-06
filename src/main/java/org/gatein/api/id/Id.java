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

package org.gatein.api.id;

import org.gatein.api.util.ParameterValidation;

import java.util.Arrays;

/**
 * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
 * @version $Revision$
 */
public abstract class Id<T extends Identifiable> implements Comparable<Id>
{
   protected final Context originalContext;
   private final Class<T> identifiableType;

   private Id(Context context, Class<T> identifiableType)
   {
      this.originalContext = context;
      this.identifiableType = identifiableType;
   }

   public String toString(Context context)
   {
      return context.toString(this);
   }

   public Class<T> getIdentifiableType()
   {
      return identifiableType;
   }

   @Override
   public String toString()
   {
      return originalContext.toString(this);
   }

   public static Id create(Context context, String rootComponent, String... additionalComponent)
   {
      return create(context, Identifiable.class, rootComponent, additionalComponent);
   }

   public static <T extends Identifiable> Id<T> create(Context context, Class<T> type, String rootComponent, String... additionalComponents)
   {
      ParameterValidation.throwIllegalArgExceptionIfNull(context, "Context");
      ParameterValidation.throwIllegalArgExceptionIfNullOrEmpty(rootComponent, "root component", null);

      return internalCreate(context, type, true, rootComponent, additionalComponents);
   }

   private static <T extends Identifiable> Id<T> internalCreate(Context context, Class<T> type, final boolean revalidate, String rootComponent, String... additionalComponents)
   {
      if (ParameterValidation.existsAndIsNotEmpty(additionalComponents))
      {
         int length = additionalComponents.length;
         int indexOfFirstNull = -1;
         int current = 0;
         for (String additionalComponent : additionalComponents)
         {
            if (ParameterValidation.isNullOrEmpty(additionalComponent))
            {
               indexOfFirstNull = current;
               break;
            }
            current++;
         }

         length = (indexOfFirstNull != -1 ? indexOfFirstNull : length);
         String[] components = new String[length + 1];
         System.arraycopy(additionalComponents, 0, components, 1, length);
         components[0] = rootComponent;

         return internalCreate(context, type, revalidate, components);
      }
      else
      {
         SimpleId<T> id = new SimpleId<T>(context, type, rootComponent);

         if (revalidate)
         {
            context.validate(id, rootComponent);
         }

         return id;
      }
   }

   private static <T extends Identifiable> Id<T> internalCreate(Context context, Class<T> type, final boolean revalidate, String... components)
   {
      if (ParameterValidation.existsAndIsNotEmpty(components))
      {
         Id<T> id;
         if (components.length == 1)
         {
            id = new SimpleId<T>(context, type, components[0]);
         }
         else
         {
            id = new ComplexId<T>(context, type, components);
         }

         if (revalidate)
         {
            context.validate(id, components);
         }

         return id;
      }
      else
      {
         throw new IllegalArgumentException("A valid root component is required to create an Id.");
      }
   }

   public static Id parse(Context context, String idAsString)
   {
      ParameterValidation.throwIllegalArgExceptionIfNull(context, "Context to interpret String as an Id");
      ParameterValidation.throwIllegalArgExceptionIfNullOrEmpty(idAsString, "String to interpret as Id", null);

      String[] components = context.extractComponents(idAsString);
      return internalCreate(context, Identifiable.class, false, components);
   }

   public static Id getIdForChild(Id parent, String childId)
   {
      ParameterValidation.throwIllegalArgExceptionIfNull(parent, "Parent resource");
      ParameterValidation.throwIllegalArgExceptionIfNullOrEmpty(childId, "child identifier", null);

      String[] components = parent.getComponents();
      int childIndex = components.length;

      Context context = parent.getOriginalContext();
      context.validate(childId, childIndex);

      String[] newComponents = new String[childIndex + 1];
      System.arraycopy(components, 0, newComponents, 0, childIndex);
      newComponents[childIndex] = childId;

      return internalCreate(context, Identifiable.class, false, newComponents);
   }

   public Id getIdforChild(String childId)
   {
      return getIdForChild(this, childId);
   }

   public String getComponent(String component)
   {
      int index = originalContext.getIndexFor(component);
      return getComponent(index, component, originalContext);
   }

   protected abstract String getComponent(int index, String component, Context context);

   protected abstract String[] getComponents();

   public Context getOriginalContext()
   {
      return originalContext;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (!(o instanceof Id))
      {
         return false;
      }

      Id id = (Id)o;

      return Arrays.equals(getComponents(), id.getComponents());
   }

   @Override
   public int hashCode()
   {
      return Arrays.hashCode(getComponents());
   }

   public int compareTo(Id o)
   {
      if (this.equals(o))
      {
         return 0;
      }
      else
      {
         return toString().compareTo(o.toString());
      }
   }

   public abstract int getComponentNumber();

   public abstract String getRootComponent();

   abstract void associateCurrentValueWith(int currentComponent, String componentName);

   abstract String getComponentNameFor(int currentComponent);

   public Id getParent()
   {
      int componentNumber = getComponentNumber();
      if (componentNumber > 1)
      {
         int parent = componentNumber - 1;
         return internalCreate(originalContext, originalContext.getComponent(getComponentNameFor(parent)).getIdentifiedComponentClass(), true, Arrays.copyOf(getComponents(), parent));
      }
      else
      {
         return null;
      }
   }

   private static class SimpleId<T extends Identifiable> extends Id<T>
   {
      private final String root;
      private String componentName;

      private SimpleId(Context context, Class<T> identifiableType, String rootComponent)
      {
         super(context, identifiableType);
         this.root = rootComponent;
      }

      @Override
      protected String getComponent(int index, String component, Context context)
      {
         if (index != 0)
         {
            if (context.isComponentRequired(component))
            {
               throw new IllegalArgumentException("Unknown component: " + component);
            }
            else
            {
               return null;
            }
         }
         else
         {
            return root;
         }
      }

      @Override
      public String[] getComponents()
      {
         return new String[]{root};
      }

      @Override
      public int getComponentNumber()
      {
         return 1;
      }

      @Override
      public String getRootComponent()
      {
         return root;
      }

      @Override
      void associateCurrentValueWith(int currentComponent, String componentName)
      {
         if (currentComponent != 0)
         {
            throw new IllegalStateException("Shouldn't be possible");
         }
         this.componentName = componentName;
      }

      @Override
      String getComponentNameFor(int componentIndex)
      {
         if (componentIndex != 0)
         {
            throw new IllegalArgumentException("Invalid component index: " + componentIndex);
         }
         else
         {
            return componentName;
         }
      }
   }

   private static class ComplexId<T extends Identifiable> extends Id<T>
   {
      private final String[] components;
      private final String[] associatedComponentName;

      public ComplexId(Context context, Class<T> identifiableType, String[] components)
      {
         super(context, identifiableType);
         this.components = components;
         associatedComponentName = new String[components.length];
      }

      @Override
      protected String getComponent(int index, String component, Context context)
      {
         if (index < 0 || index >= components.length)
         {
            if (context.isComponentRequired(component))
            {
               throw new IllegalArgumentException("Unknown component: " + component);
            }
            else
            {
               return null;
            }
         }
         else
         {
            return components[index];
         }
      }

      @Override
      public String[] getComponents()
      {
         return components;
      }

      @Override
      public int getComponentNumber()
      {
         return components.length;
      }

      @Override
      public String getRootComponent()
      {
         return components[0];
      }

      @Override
      void associateCurrentValueWith(int currentComponent, String componentName)
      {
         associatedComponentName[currentComponent] = componentName;
      }

      @Override
      String getComponentNameFor(int currentComponent)
      {
         return associatedComponentName[currentComponent];
      }
   }
}
