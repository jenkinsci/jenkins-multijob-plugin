package com.tikal.jenkins.plugins.multijob;

import net.sf.json.JSON;
import net.sf.json.JSONArray;

/**
 * Created with IntelliJ IDEA.
 * User: liorb
 * Date: 9/24/13
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {
    public static JSONArray getCreateJSONArray(JSON json){
        if(!(json instanceof  JSONArray)){
           JSONArray jsonArray = new JSONArray();
           jsonArray.add(json);
           return jsonArray;
        }
        return (JSONArray)json;
    }
}
