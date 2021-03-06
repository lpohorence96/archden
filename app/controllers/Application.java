package controllers;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.cache.Cache;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import constants.Constants;
import data.QueryAppender;

public class Application extends Controller {

	public static void index() {
		render();
	}
	
	public static void archden() {
		render();
	}

	public static void plotAllLocations() {
		Logger.info("Plotting all locations");
		Gson gson = new Gson();
		Type mapType = new TypeToken<Map<String, Map<String, String>>>() {
		}.getType();
		String json = (String) Cache.get("alllocations");
		
		if (json != null) {
			Logger.info("RESP: "+ json);
			Map<String, Map<String, String>> retMap = gson.fromJson(json,
					mapType);
			JsonElement je = gson.toJsonTree(retMap);
			renderJSON(je);
			return;
		}

		String ws = Constants.WSURL + "/cql/archden/locations?select=select *";
		HttpResponse res = WS.url(ws)
				.authenticate(Constants.TOKEN, Constants.ACCOUNTID).get();

		json = res.getString("UTF-8");
		Logger.info("RESP: "+ json);
		Cache.add("alllocations", json);
		Map<String, Map<String, String>> retMap = gson.fromJson(json, mapType);
		JsonElement je = gson.toJsonTree(retMap);
		renderJSON(je);
	}

	public static void plotNamedLocation(String name) {

		Logger.info("Plotting location: " + name);
		name = name.trim();

		String ws = Constants.WSURL
				+ "/cql/archden/locations?select=select * WHERE 'name' = '"
				+ name + "'";
		Logger.info("Request: " + ws);
		HttpResponse res = WS.url(ws)
				.authenticate(Constants.TOKEN, Constants.ACCOUNTID).get();

		renderJSON(res.getJson());

	}

	public static void plotByMassTimes(String dayofweek, String timeofday,
			String name, boolean confession) {
		String operator = "EQ";
		String operator2 = "LT";
		String timeofday2 = null;
		QueryAppender qa = new QueryAppender();
		Gson gson = new Gson();
		try {
			if (timeofday.equalsIgnoreCase("morning")) {
				timeofday = "1100";
				operator = "LT";
			} else if (timeofday.equalsIgnoreCase("noon")) {
				timeofday = "1100";
				operator = "GT";
				timeofday2 = "1259";
			} else if (timeofday.equalsIgnoreCase("evening")) {
				timeofday = "1200";
				operator = "GT";
			} else {
				timeofday = "100";
				operator = "GT";
			}
			
			if (confession) {
				if(dayofweek.equalsIgnoreCase("any")){
					dayofweek = "saturday";
				}
				dayofweek = dayofweek + "confessions";
			}

			Map<String, Map<String, String>> retMap = qa.queryByTime(dayofweek,
					timeofday, operator, null, timeofday2, operator2);
			
			JsonElement json = null;
			
			if(name != null  && !name.isEmpty()){
				Map<String, Map<String, String>> searchResults = new HashMap<String, Map<String, String>>();
				Collection<Map<String, String>> results = retMap.values();
				name = name.toLowerCase();
				for (Map<String, String> result : results) {
					
					if (result.get("name").equalsIgnoreCase(name) || 
							result.get("full name").equalsIgnoreCase(name)) {
						//Logger.info("Contains name");
						searchResults.put(result.get("KEY"), result);
					}

				}
				json = gson.toJsonTree(searchResults);
			}else{
				json = gson.toJsonTree(retMap);
			}
			

			renderJSON(json);
		} catch (Exception e) {
			Logger.error(e, "Unable to plot by mass time");
			response.status = 200;
			renderJSON("No Results");
		}

	}

	public static void searchData(String topic) {
		Gson gson = new Gson();
		Map<String, Map<String, String>> searchResults = new HashMap<String, Map<String, String>>();
		Type mapType = new TypeToken<Map<String, Map<String, String>>>() {
		}.getType();
		String json = (String) Cache.get("alllocations");
		if (json != null) {
			Map<String, Map<String, String>> retMap = gson.fromJson(json,
					mapType);

			Collection<Map<String, String>> results = retMap.values();
			int i = 1;
			topic = topic.toLowerCase();
			for (Map<String, String> result : results) {

				//Logger.info("Topic: " + topic);
				if (result.containsKey(topic)) {
					//Logger.info("Contains topic");
					searchResults.put(result.get("KEY"), result);
				}

			}

			JsonElement je = gson.toJsonTree(searchResults);
			renderJSON(je);


		}

	}
}