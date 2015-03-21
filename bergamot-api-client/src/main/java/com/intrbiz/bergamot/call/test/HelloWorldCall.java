package com.intrbiz.bergamot.call.test;

import java.io.IOException;

import org.apache.http.client.fluent.Response;

import com.intrbiz.bergamot.BergamotAPICall;
import com.intrbiz.bergamot.BergamotAPIException;
import com.intrbiz.bergamot.BergamotClient;

public class HelloWorldCall extends BergamotAPICall<String>
{    
    public HelloWorldCall(BergamotClient client)
    {
        super(client);
    }
    
    public String execute()
    {
        try
        {
            Response response = execute(get(url("/test/hello/world")));
            return transcoder().decodeFromString(response.returnContent().asString(), String.class);
        }
        catch (IOException e)
        {
            throw new BergamotAPIException("Error calling hello world", e);
        }
    }
}
