package com.github.pherialize.io;

import java.io.IOException;
import java.io.InputStream;

import com.github.pherialize.exceptions.UnserializeException;

public class InputStreamSource implements Source
{
    protected final InputStream in;
    
    public InputStreamSource(InputStream in)
    {
        this.in=in;
    }
    
    @Override
    public int read()
    {
        try
        {
            return in.read();
        }
        catch (IOException ex)
        {
            throw new UnserializeException("Exception when reading from InputStream",ex);
        }
    }
    @Override
    public int read(byte[] buffer, int offset, int length)
    {
        try
        {
            return in.read(buffer, offset, length);
        }
        catch (IOException ex)
        {
            throw new UnserializeException("Exception when reading from InputStream",ex);
        }
    }
    
    @Override
    public void close()
    {
        try
        {
            in.close();
        }
        catch (Exception ex)
        {
            // ignored
        }
    }

}
