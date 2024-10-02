package com.tracelytics.joboe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.tracelytics.ext.uncommons.maths.random.SeedException;
import com.tracelytics.ext.uncommons.maths.random.SeedGenerator;

/**
 * Copied from <code>com.tracelytics.ext.uncommons.maths.random.DevRandomSeedGenerator</code>, but use /dev/urandom instead of 
 * /dev/random
 * @author pluk
 */
public class DevURandomSeedGenerator implements SeedGenerator
{
    private static final File DEV_U_RANDOM = new File("/dev/urandom");

    public byte[] generateSeed(int length) throws SeedException
    {
        FileInputStream file = null;
        try
        {
            file = new FileInputStream(DEV_U_RANDOM);
            byte[] randomSeed = new byte[length];
            int count = 0;
            while (count < length)
            {
                int bytesRead = file.read(randomSeed, count, length - count);
                if (bytesRead == -1)
                {
                    throw new SeedException("EOF encountered reading random data.");
                }
                count += bytesRead;
            }
            return randomSeed;
        }
        catch (IOException ex)
        {
            throw new SeedException("Failed reading from " + DEV_U_RANDOM.getName(), ex);
        }
        catch (SecurityException ex)
        {
            // Might be thrown if resource access is restricted (such as in
            // an applet sandbox).
            throw new SeedException("SecurityManager prevented access to " + DEV_U_RANDOM.getName(), ex);
        }
        finally
        {
            if (file != null)
            {
                try
                {
                    file.close();
                }
                catch (IOException ex)
                {
                    // Ignore.
                }
            }
        }
    }


    @Override
    public String toString()
    {
        return "/dev/urandom";
    }
}
