package com.github.pherialize.io;

import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

import com.github.pherialize.exceptions.UnserializeException;

/**
 * A source of serialized data. Might be a String or a Stream or such.
 * @author mwyraz
 */
public interface Source
{
    /**
     * Closes the source. This will never thron an exception.
     */
    public void close();
    
    /**
     * {@link InputStream.read}
     * 
     * @throws UnserializeException in case of an error
     */
    public int read();
    
    /**
     * {@link InputStream.read} 
     * @throws UnserializeException in case of an error
     */
    public int read(byte[] buffer, int offset, int length);
    
}
