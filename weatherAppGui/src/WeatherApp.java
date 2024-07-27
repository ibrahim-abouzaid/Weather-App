import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

//retrieve weather data from API - this backend logic will fetch the latest weather data
// from the external api and return it,the GUI will display this data to user
public class WeatherApp {
    //fetch weather data for giving location
    public static JSONObject getWeatherData (String locationName){
        //get location coordinates using the geolocation API
        JSONArray locationData=getLocationData(locationName);
        if(locationData == null){
            System.out.println("enter correct name");
            return null;
        }
        //extract latitude and longitude data
        JSONObject location =(JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //build API request URL with location coordinates
        String urlString ="https://api.open-meteo.com/v1/forecast?"+
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Africa%2FCairo";
         try{
             //call api and get response
             HttpURLConnection conn =fetchApiResponse(urlString);

             //check for response status
             //200 means that the connecction was success
             if(conn.getResponseCode() !=200){
                 System.out.println("Error: Could not connect to API");
                 return  null;
             }
             //store the API results
             StringBuilder resultJson =new StringBuilder();
             Scanner sc=new Scanner(conn.getInputStream());

             //read and store the resulting json data into our String builder
             while (sc.hasNext()){
                 resultJson.append(sc.nextLine());
             }
             //close scanner
             sc.close();

             //close url connection
             conn.disconnect();

             //parse the JSON string into json obj
             JSONParser pareser= new JSONParser();
             JSONObject resultsJsonObj =(JSONObject) pareser.parse(String.valueOf(resultJson));

             //retrieve hourly data
             JSONObject  hourly =(JSONObject) resultsJsonObj.get("hourly");

             //we want to get the current hour's data
             //so we need to get the index of our current hour
             JSONArray time =(JSONArray) hourly.get("time");
             int index= findIndexOfCurrentTime(time);

             //get temperatureData
             JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
             double temperature=(double) temperatureData.get(index);

             //get weather code
             JSONArray weather_Code = (JSONArray) hourly.get("weather_code");
             String weathercondition= convertWeatherCode((long)weather_Code.get(index));

             //get humidity
             JSONArray humidityData = (JSONArray) hourly.get("relative_humidity_2m");
             long humidity=(long) humidityData.get(index);

             //get wind speed
             JSONArray windSpeedData = (JSONArray) hourly.get("wind_speed_10m");
             double windspeed=(double) windSpeedData.get(index);

             //build the weather json data object that we are going to access in our frontend
             JSONObject weatherData=new JSONObject();
             weatherData.put("temperature",temperature);
             weatherData.put("weather_condition",weathercondition);
             weatherData.put("humidity",humidity);
             weatherData.put("windspeed",windspeed);

             return weatherData;




         }catch (Exception e){
             e.printStackTrace();
         }
        return null;
    }
    //retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName){
        //replace any whitespace in location name to + to adhere to API's request format
        locationName=locationName.replaceAll(" ","+");

        //build API url with location parameter
        String urlString ="https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try{
            //call api and get response
            HttpURLConnection conn= fetchApiResponse(urlString);

            //check response status
            //200 means successfull connection
            if(conn.getResponseCode()!= 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }
            else{
                //store the API results
                StringBuilder resultJson =new StringBuilder();
                Scanner sc=new Scanner(conn.getInputStream());

                //read and store the resulting json data into our String builder
                while (sc.hasNext()){
                    resultJson.append(sc.nextLine());
                }
                //close scanner
                sc.close();

                //close url connection
                conn.disconnect();

                //parse the JSON string into json obj
                JSONParser pareser= new JSONParser();
                JSONObject resultsJsonObj =(JSONObject) pareser.parse(String.valueOf(resultJson));

                //get the list of location data the Api generated from the location name
                JSONArray loactionDate=(JSONArray) resultsJsonObj.get("results");
                return loactionDate;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private static  HttpURLConnection fetchApiResponse (String urlString ){
        try {
            //attempt to create connection
            URL url= new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");

            //connect to our API
            conn.connect();
            return conn;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        //could not make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime=getcurrentTime();

        //iterate through the time list and see which one matches our current time
        for (int i=0;i<timeList.size();i++){
            String time=(String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }
        return 0;
    }
    public static String getcurrentTime(){
        //get current data and time
        LocalDateTime curentDateTime= LocalDateTime.now();

        //format data to be 2024-07-27T00:00 (this is how is read in the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //formate and print the current data and time
        String formattedDateTime=curentDateTime.format(formatter);
        return formattedDateTime;
    }
    private static String convertWeatherCode (long weathercode){
        String weatherCondition ="";

        if(weathercode==0L){
            //clear
            weatherCondition="Clear";
        }else if(weathercode >0L && weathercode <=3L){
            //cloudy
            weatherCondition="Cloudy";
        }else if((weathercode >= 51L && weathercode <=67L)
                    || (weathercode >=80L && weathercode <=99L)){
            //rain
            weatherCondition= "Rain";
        }else if(weathercode >= 71L && weathercode <= 77L){
            //snow
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
