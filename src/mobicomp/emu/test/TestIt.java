package mobicomp.emu.test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Dave Bryson
 */
public class TestIt
{
    public static void main(String[] args)
    {
        JSONObject obj = new JSONObject();
        try
        {
            obj.put("to","123");
            obj.put("from","456");
            JSONArray $ = new JSONArray("[1,2,3]");
            obj.put("list",$);
            System.out.println(obj.toString());
        }
        catch (JSONException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
