package org.asf.emuferal.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InspirationConverter {
	
	public static void main(String[] args) throws IOException {
		// This tool generates a userVars.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject inspirations = new JsonObject();
		String lastData = null;
		String id = "";
		
		JsonObject userVarDefinition = new JsonObject();
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				
				//start a new uservar definition
				userVarDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (NPC ID)
				id = lineParts[0].substring(1);
				
				//1 - DefName (Change this to userVarName, nicer)
				userVarDefinition.addProperty("inspirationName", lineParts[1].replace("\"", ""));
				
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if(!skip)
				{
					System.out.println("Parsing inspiration ID: " + id);
					
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject jsonObject = JsonParser.parseString(lastData).getAsJsonObject();
					userVarDefinition.add("data", jsonObject);
					
					//add the uservar to the uservar list
					inspirations.add(id, userVarDefinition);				
				}
				
				//start a fresh userVar definition
				userVarDefinition = new JsonObject();	
				lastData = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("inspirations", inspirations);
		
		//blegh hard coded path 
        String path = "inspirations.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}