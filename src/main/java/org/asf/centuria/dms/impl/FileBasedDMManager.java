package org.asf.centuria.dms.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedDMManager extends DMManager {

	private ArrayList<String> activeIDs = new ArrayList<String>();
	private static final int HISTORY_LIMIT = 5500;

	@Override
	public void openDM(String dmID, String[] participants) {
		try {
			if (!new File("dms").exists())
				new File("dms").mkdirs();
			if (!dmExists(dmID)) {
				JsonObject dm = new JsonObject();
				JsonArray participantObjects = new JsonArray();
				for (String p : participants)
					participantObjects.add(p);
				dm.add("participants", participantObjects);
				dm.add("messages", new JsonArray());
				Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			}
		} catch (Exception e) {
		}
	}

	@Override
	public boolean dmExists(String dmID) {
		try {
			return new File("dms/" + UUID.fromString(dmID) + ".json").exists();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public PrivateChatMessage[] getDMHistory(String dmID, String requester) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("messages").getAsJsonArray();
			reader.close();

			ArrayList<PrivateChatMessage> messages = new ArrayList<PrivateChatMessage>();
			for (JsonElement ele : data) {
				JsonObject msg = ele.getAsJsonObject();
				String source;
				if (msg.has("source"))
					source = msg.get("source").getAsString();
				else
					source = msg.get("s").getAsString();

				if (SocialManager.getInstance().socialListExists(requester)
						&& SocialManager.getInstance().getPlayerIsBlocked(requester, source))
					continue;

				PrivateChatMessage message = new PrivateChatMessage();
				message.content = msg.has("content") ? msg.get("content").getAsString() : msg.get("c").getAsString();
				message.source = source;
				if (msg.has("sentAt")) {
					// Parse old format
					SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					message.sentAt = 0;
					try {
						message.sentAt = fmt.parse(msg.get("sentAt").getAsString()).getTime();
					} catch (ParseException e) {
					}
				} else
					message.sentAt = msg.get("a").getAsLong();
				messages.add(message);
			}
			return messages.toArray(t -> new PrivateChatMessage[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void saveDMMessge(String dmID, PrivateChatMessage message) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("messages").getAsJsonArray();
			reader.close();

			// Remove first message if the chat is too long
			if (data.size() >= HISTORY_LIMIT)
				data.remove(0);

			// Add message
			JsonObject msg = new JsonObject();
			msg.addProperty("c", message.content);
			msg.addProperty("s", message.source);
			msg.addProperty("a", message.sentAt);
			data.add(msg);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			if (activeIDs.contains(dmID))
				activeIDs.remove(dmID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getDMParticipants(String dmID) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		ArrayList<String> participants = new ArrayList<String>();
		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Add participants
			for (JsonElement ele : data) {
				participants.add(ele.getAsString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return participants.toArray(t -> new String[t]);
	}

	@Override
	public void deleteDM(String dmID) {
		if (dmExists(dmID))
			new File("dms/" + UUID.fromString(dmID) + ".json").delete();
	}

	@Override
	public void addParticipant(String dmID, String participant) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Add participant
			data.add(participant);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void removeParticipant(String dmID, String participant) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Remove participant
			for (JsonElement ele : data) {
				if (ele.getAsString().equals(participant)) {
					data.remove(ele);
					break;
				}
			}

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void updateDMParticipants(String dmID, String[] participants) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			reader.close();

			// Update participants
			JsonArray participantObjects = new JsonArray();
			for (String p : participants)
				participantObjects.add(p);
			dm.add("participants", participantObjects);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
