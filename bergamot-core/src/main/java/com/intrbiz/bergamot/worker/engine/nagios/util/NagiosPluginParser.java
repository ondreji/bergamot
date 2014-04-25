package com.intrbiz.bergamot.worker.engine.nagios.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.intrbiz.bergamot.model.Status;
import com.intrbiz.bergamot.model.message.result.Result;

public class NagiosPluginParser
{
    public static final int NAGIOS_OK = 0;

    public static final int NAGIOS_WARNING = 1;

    public static final int NAGIOS_CRITICAL = 2;

    public static final int NAGIOS_UNKNOWN = 3;

    /**
     * Convert a Nagios exit code to a Bergamot result
     */
    public static void parseNagiosExitCode(int code, Result result)
    {
        switch (code)
        {
            case NAGIOS_OK:
                result.setOk(true);
                result.setStatus(Status.OK);
                break;
            case NAGIOS_WARNING:
                result.setOk(false);
                result.setStatus(Status.WARNING);
                break;
            case NAGIOS_CRITICAL:
                result.setOk(false);
                result.setStatus(Status.CRITICAL);
                break;
            case NAGIOS_UNKNOWN:
            default:
                result.setOk(false);
                result.setStatus(Status.UNKNOWN);
                break;
        }
    }

    /**
     * Extract the check output
     */
    public static void parseNagiosOutput(String output, Result result) throws IOException
    {
        parseNagiosOutput(new StringReader(output), result);
    }

    /**
     * Extract the check output
     */
    public static void parseNagiosOutput(InputStream stream, Result result) throws IOException
    {
        parseNagiosOutput(new InputStreamReader(stream), result);
    }

    /**
     * Extract the check output
     */
    public static void parseNagiosOutput(Reader stream, Result result) throws IOException
    {
        // currently only look at the first line
        try (BufferedReader br = new BufferedReader(stream))
        {
            String outputLine = br.readLine();
            if (outputLine != null)
            {
                int pipe = outputLine.indexOf("|");
                if (pipe > 0 && pipe < outputLine.length())
                {
                    result.setOutput(outputLine.substring(0, pipe).trim());
                    result.addParameter("nagios_perf_data", outputLine.substring(pipe + 1).trim());
                }
                else
                {
                    result.setOutput(outputLine.trim());
                }
            }
        }
    }
}
