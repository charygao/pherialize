package com.github.pherialize.io;

import java.io.ByteArrayInputStream;

import com.github.pherialize.exceptions.UnserializeException;

public class ByteArraySource implements Source
{
    protected final ByteArrayInputStream stream;
    
    public ByteArraySource(byte[] data)
    {
        this.stream=new ByteArrayInputStream(data);
    }
    
    @Override
    public void close()
    {
        // Closing ByteArrayInputStream has no effect
    }
    
    @Override
    public int read()
    {
        return stream.read();
    }
    
    @Override
    public int read(byte[] buffer, int offset, int length)
    {
        return stream.read(buffer, offset, length);
    }

}
