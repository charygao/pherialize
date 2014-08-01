package com.github.pherialize.factory;

import com.github.pherialize.MixedArray;

public interface ObjectFactory
{
    public Object createObject(String className, MixedArray properties);
}
