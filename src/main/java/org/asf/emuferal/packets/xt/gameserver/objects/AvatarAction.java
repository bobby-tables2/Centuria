package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class AvatarAction implements IXtPacket<AvatarAction> {

	private String action;

	@Override
	public AvatarAction instantiate() {
		return new AvatarAction();
	}

	@Override
	public String id() {
		return "aa";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		action = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	private ArrayList<Integer> ints = new ArrayList<Integer>();

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Avatar action
		Player plr = (Player) client.container;
		XtWriter pk = new XtWriter();
		pk.writeString("ou");
		pk.writeInt(-1); // Data prefix
		pk.writeString(plr.account.getAccountID());
		pk.writeInt(4);
		pk.writeLong(System.currentTimeMillis() / 1000);
		pk.writeDouble(plr.lastPosX);
		pk.writeDouble(plr.lastPosY);
		pk.writeDouble(plr.lastPosZ);
		pk.writeString("0");
		pk.writeDouble(plr.lastRotX);
		pk.writeDouble(plr.lastRotY);
		pk.writeDouble(plr.lastRotZ);
		pk.writeString("0");
		pk.writeString("0");
		pk.writeString("0");
		pk.writeDouble(plr.lastRotW);
		switch (action) {
		case "8930": { // Sleep
			pk.writeInt(40);
			break;
		}
		case "9108": { // Tired
			pk.writeInt(41);
			break;
		}
		case "9116": { // Sit
			pk.writeInt(60);
			break;
		}
		case "9121": { // Mad
			pk.writeInt(70);
			break;
		}
		case "9122": { // Excite
			pk.writeInt(80);
			break;
		}
		case "9143": { // Sad
			pk.writeInt(180);
			break;
		}
		case "9151": { // Flex
			pk.writeInt(200);
			break;
		}
		case "9190": { // Play
			pk.writeInt(210);
			break;
		}
		}
		pk.writeString(""); // Data suffix

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(pk.encode());
			}
		}

		// TODO
		return true;
	}

}