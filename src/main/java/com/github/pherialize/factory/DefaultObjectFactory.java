package com.github.pherialize.factory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map.Entry;

import com.github.pherialize.Mixed;
import com.github.pherialize.MixedArray;
import com.github.pherialize.exceptions.UnserializeException;

public class DefaultObjectFactory implements ObjectFactory 
{
    protected final String javaPackagePrefix;
    public DefaultObjectFactory(String javaPackagePrefix)
    {
        if (javaPackagePrefix!=null)
        {
            if (javaPackagePrefix.length()==0) javaPackagePrefix=null;
            else if (!javaPackagePrefix.endsWith(".")) javaPackagePrefix+=".";
        }
        
        this.javaPackagePrefix=javaPackagePrefix;
    }
    
    protected String getJavaClassName(String phpClassName)
    {
        if (javaPackagePrefix!=null) return javaPackagePrefix+phpClassName;
        return phpClassName;
    }
    
    protected Object createInstance(String javaClassName) throws Exception
    {
        return Class.forName(javaClassName).newInstance();        
    }
    
    
    protected void setProperty(Object instance, String key, Object value) throws Exception
    {
        Field field=findField(instance.getClass(), key);
        
        if (Modifier.isStatic(field.getModifiers())) throw new UnserializeException("Found field '"+field+"' but it is static.");
        if (Modifier.isFinal(field.getModifiers())) throw new UnserializeException("Found field '"+field+"' but it is final.");
        
        field.setAccessible(true);
        
        field.set(instance,value);
    }
    
    protected Field findField(Class type, String fieldName) throws Exception
    {
        NoSuchFieldException firstException=null;
        while (type!=null)
        {
            try
            {
                return type.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException ex)
            {
                if (firstException==null) firstException=ex;
                type=type.getSuperclass();
            }
        }
        throw firstException;
    }
    
    @Override
    public Object createObject(String className, MixedArray properties)
    {
        try
        {
            Object instance=createInstance(getJavaClassName(className));
            
            for (Entry<Object, Object> props: properties.entrySet())
            {
                String key=((Mixed)props.getKey()).toString();
                Object value=((Mixed)props.getValue()).toObject();
                
                setProperty(instance,key,value);
            }
            
            
            return instance;
        }
        catch (Exception ex)
        {
            throw new UnserializeException("Unable to create object",ex);
        }
    }
}
