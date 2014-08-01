/*
 * $Id$
 * Copyright (C) 2009 Klaus Reimer <k@ailis.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.github.pherialize;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.github.pherialize.exceptions.UnserializeException;
import com.github.pherialize.io.ByteArraySource;
import com.github.pherialize.io.Source;


/**
 * Unserializes a PHP serialize format string into a Java object.
 *
 * @author Klaus Reimer (k@ailis.de)
 * @version $Revision$
 */

public class Unserializer
{
    /** The source where we read from */
    private final Source source;
    
    /** Charset of the source. Used to construct strings **/
    private final Charset sourceCharset;

    /** The object history for resolving references */
    private final List<Object> history;



    /**
     * Constructor
     *
     * @param source
     *            The source of data to unserialize
     */

    public Unserializer(final Source source, final Charset sourceCharset)
    {
        super();
        this.source = source;
        this.sourceCharset = sourceCharset;
        this.history = new ArrayList<Object>();
    }

    /**
     * Constructor
     *
     * @param data
     *            The data to unserialize
     */

    public Unserializer(final String data, final Charset sourceCharset)
    {
        this(new ByteArraySource(data.getBytes(sourceCharset)),sourceCharset);
    }

    /**
     * Constructor
     *
     * @param data
     *            The data to unserialize
     */

    public Unserializer(final String data)
    {
        this(data,Charset.defaultCharset());
    }

    /**
     * Reads a one-byte character from the source.
     * @return
     */
    protected char readNextControlCharacter()
    {
        int result=source.read();
        if (result<0) throw new UnserializeException("Unexepected end of data.");
        return (char) result;
    }
    
    /**
     * Reads a character and compares it to the given character
     * @throws UnserializeException if the caraters does not match
     */
    protected void readExpected(char expected)
    {
        char c=readNextControlCharacter();
        if (c!=expected) throw new UnserializeException("Unexepected character. Expected '"+expected+"' but got '"+c+"'");
    }
    
    /**
     * Reads digits until "endOfIntegerCharacter" (usually a collon) occurs
     * @throws UnserializeException on unexpected data
     */
    protected int readInt(char endOfIntegerCharacter)
    {
        // The code here is similar to Integer.parseInt but uses the "endOfIntegerCharacter" as terminator
        
        int result=0;
        
        boolean negative=false;
        boolean first=true;
        
        for (;;)
        {
            char c=readNextControlCharacter();
            
            if (first)
            {
                first=false;
                if (c=='-')
                {
                    negative=true;
                    continue;
                }
            }
            
            
            int digit=Character.digit(c, 10);
            if (digit>=0 && digit<=9)
            {
                result=result*10+digit;
            }
            else if (c==endOfIntegerCharacter)
            {
                break;
            }
            else
            {
                throw new UnserializeException("Unexepected character. Expected 0...9 or '"+endOfIntegerCharacter+"' but got '"+c+"'");
            }
            first=false;
        }
        
        if (negative) result=-result;
        
        return result;
    }
    
    /**
     * Reads from the source until the requests number of bytes is read
     * @param numberOfBytes
     * @return
     */
    protected byte[] readExactly(int numberOfBytes)
    {
        byte[] result=new byte[numberOfBytes];
        
        /**
         * Some code from common-io IOUtils.read
         */
        int remaining = numberOfBytes;
        while (remaining > 0) {
            int location = numberOfBytes - remaining;
            int count = source.read(result, location, remaining);
            if (count<0) // EOF
            {
                break;
            }
            remaining -= count;
        }
        
        if (remaining!=0) throw new UnserializeException("Unexepected end of data."); 
        
        return result;
    }
    
    
    /**
     * Unserializes the next object in the data stream.
     *
     * @return The unserializes object
     */

    public Mixed unserializeObject()
    {
        char type;
        Mixed result;

        type = readNextControlCharacter();
        switch (type)
        {
            case 's':
                result = unserializeString();
                break;

            case 'i':
                result = unserializeInteger();
                break;

            case 'd':
                result = unserializeDouble();
                break;

            case 'b':
                result = unserializeBoolean();
                break;

            case 'N':
                result = unserializeNull();
                break;

            case 'a':
                return unserializeArray();

            case 'R':
                result = unserializeReference();
                break;

            default:
                throw new UnserializeException(
                    "Unable to unserialize unknown type " + type);
        }

        this.history.add(result);
        return result;
    }


    /**
     * Unserializes the next object in the data stream into a String.
     *
     * @return The unserialized String
     */

    private Mixed unserializeString()
    {
        readExpected(':');

        int stringLengthInBytes=readInt(':');
        
        readExpected('"');
        
        String result=new String(readExactly(stringLengthInBytes),sourceCharset);
        
        readExpected('"');
        readExpected(';');
        
        return new Mixed(result);
//        pos = this.data.indexOf(':', this.pos + 2);
//        length = Integer.parseInt(this.data.substring(this.pos + 2, pos));
//        this.pos = pos + length + 4;
//        String unencoded = this.data.substring(pos + 2, pos + 2 + length);
//        return new Mixed(encode(unencoded, charset));
    }


    /**
     * Unserializes the next object in the data stream into an Integer.
     *
     * @return The unserialized Integer
     */

    private Mixed unserializeInteger()
    {
        readExpected(':');

        int result=readInt(';');
        return new Mixed(result);
    }


    /**
     * Unserializes the next object in the data stream into an Double.
     *
     * @return The unserialized Double
     */

    private Mixed unserializeDouble()
    {
        readExpected(':');

        StringBuilder sb=new StringBuilder();
        boolean first=true;
        boolean allowFractionalDigits=true;
        
        for (;;)
        {
            char c=readNextControlCharacter();
            
            if ((c>='0' && c<='9') || (first && c=='-'))
            {
                sb.append(c);
            }
            else if (allowFractionalDigits && c=='.')
            {
                sb.append(c);
                allowFractionalDigits=false;
            }
            else if (c==';')
            {
                break;
            }
            else
            {
                String allowed="0...9";
                
                if (first)
                {
                    allowed+=" or '-'";
                }
                if (allowFractionalDigits)
                {
                    allowed+=" or '.'";
                }
                allowed+=" or ';'";
                
                throw new UnserializeException("Unexepected character. Expected "+allowed+" but got '"+c+"'");
            }
        }
        Double result=Double.parseDouble(sb.toString());
        return new Mixed(result);
    }


    /**
     * Unserializes the next object in the data stream as a reference.
     *
     * @return The unserialized reference
     */

    private Mixed unserializeReference()
    {
        readExpected(':');
        int index = readInt(';');
        return (Mixed) this.history.get(index - 1);
    }


    /**
     * Unserializes the next object in the data stream into a Boolean.
     *
     * @return The unserialized Boolean
     */

    private Mixed unserializeBoolean()
    {
        readExpected(':');

        int result=readInt(';');
        
        if (result<0 || result>1) throw new UnserializeException("Unexpected boolean value. Expected 0 or 1 but got "+result);
            
        return new Mixed(result==1);
    }


    /**
     * Unserializes the next object in the data stream into a Null
     *
     * @return The unserialized Null
     */

    private Mixed unserializeNull()
    {
        readExpected(';');        
        return null;
    }


    /**
     * Unserializes the next object in the data stream into an array. This
     * method returns an ArrayList if the unserialized array has numerical
     * keys starting with 0 or a HashMap otherwise.
     *
     * @return The unserialized array
     */

    private Mixed unserializeArray()
    {
        readExpected(':');
        
        Mixed result;
        MixedArray array;
        int max;
        int i;
        Object key, value;

        max = readInt(':');
        readExpected('{');
                
        array = new MixedArray(max);
        result = new Mixed(array);
        this.history.add(result);
        for (i = 0; i < max; i++)
        {
            key = unserializeObject();
            this.history.remove(this.history.size() - 1);
            value = unserializeObject();
            array.put(key, value);
        }
        readExpected('}');
        return result;
    }


    static String decode(String encoded, Charset charset)
    {
        try {
            return new String(encoded.getBytes(charset), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return encoded;
        }
    }


    static String encode(String decoded, Charset charset)
    {
        try {
            return new String(decoded.getBytes("ISO-8859-1"), charset);
        } catch (UnsupportedEncodingException e) {
            return decoded;
        }
    }
}
