package com.sublate.core.generator;


import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.sublate.core.Config;
import com.sublate.core.R;
import com.sublate.core.entities.Account;
import com.sublate.core.entities.Contact;
import com.sublate.core.entities.Conversation;
import com.sublate.core.services.XmppConnectionService;
import com.sublate.core.utils.Xmlns;
import com.sublate.core.xml.Element;
import com.sublate.core.xmpp.forms.Data;
import com.sublate.core.xmpp.jid.Jid;
import com.sublate.core.xmpp.pep.Avatar;
import com.sublate.core.xmpp.stanzas.IqPacket;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import eu.siacs.conversations.generator.AbstractGenerator;


public class IqGenerator extends AbstractGenerator {

	public IqGenerator(final XmppConnectionService service) {
		super(service);
	}

	public IqPacket discoResponse(final IqPacket request) {
		final IqPacket packet = new IqPacket(IqPacket.TYPE.RESULT);
		packet.setId(request.getId());
		packet.setTo(request.getFrom());
		final Element query = packet.addChild("query",
				"http://jabber.org/protocol/disco#info");
		query.setAttribute("node", request.query().getAttribute("node"));
		final Element identity = query.addChild("identity");
		identity.setAttribute("category", "client");
		identity.setAttribute("type", getIdentityType());
		identity.setAttribute("name", getIdentityName());
		for (final String feature : getFeatures()) {
			query.addChild("feature").setAttribute("var", feature);
		}
		return packet;
	}

	public IqPacket versionResponse(final IqPacket request) {
		final IqPacket packet = request.generateResponse(IqPacket.TYPE.RESULT);
		Element query = packet.query("jabber:iq:version");
		query.addChild("name").setContent(mXmppConnectionService.getString(R.string.app_name1));
		query.addChild("version").setContent(getIdentityVersion());
		if ("chromium".equals(android.os.Build.BRAND)) {
			query.addChild("os").setContent("Chrome OS");
		} else{
			query.addChild("os").setContent("Android");
		}
		return packet;
	}

	public IqPacket commandResponse(final IqPacket request) {
		final IqPacket packet = request.generateResponse(IqPacket.TYPE.RESULT);
		return packet;
	}

	public IqPacket entityTimeResponse(IqPacket request) {
		final IqPacket packet = request.generateResponse(IqPacket.TYPE.RESULT);
		Element time = packet.addChild("time","urn:xmpp:time");
		final long now = System.currentTimeMillis();
		time.addChild("utc").setContent(getTimestamp(now));
		TimeZone ourTimezone = TimeZone.getDefault();
		long offsetSeconds = ourTimezone.getOffset(now) / 1000;
		long offsetMinutes = offsetSeconds % (60 * 60);
		long offsetHours = offsetSeconds / (60 * 60);
		time.addChild("tzo").setContent(String.format("%02d",offsetHours)+":"+String.format("%02d",offsetMinutes));
		return packet;
	}

	protected IqPacket publish(final String node, final Element item) {
		final IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		final Element pubsub = packet.addChild("pubsub",
				"http://jabber.org/protocol/pubsub");
		final Element publish = pubsub.addChild("publish");
		publish.setAttribute("node", node);
		publish.addChild(item);
		return packet;
	}

	protected IqPacket retrieve(String node, Element item) {
		final IqPacket packet = new IqPacket(IqPacket.TYPE.GET);
		final Element pubsub = packet.addChild("pubsub",
				"http://jabber.org/protocol/pubsub");
		final Element items = pubsub.addChild("items");
		items.setAttribute("node", node);
		if (item != null) {
			items.addChild(item);
		}
		return packet;
	}

	public IqPacket publishNick(String nick) {
		final Element item = new Element("item");
		item.addChild("nick","http://jabber.org/protocol/nick").setContent(nick);
		return publish("http://jabber.org/protocol/nick", item);
	}

	public IqPacket publishAvatar(Avatar avatar) {
		final Element item = new Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final Element data = item.addChild("data", "urn:xmpp:avatar:data");
		data.setContent(avatar.image);
		return publish("urn:xmpp:avatar:data", item);
	}

	public IqPacket publishAvatarMetadata(final Avatar avatar) {
		final Element item = new Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final Element metadata = item
			.addChild("metadata", "urn:xmpp:avatar:metadata");
		final Element info = metadata.addChild("info");
		info.setAttribute("bytes", avatar.size);
		info.setAttribute("id", avatar.sha1sum);
		info.setAttribute("height", avatar.height);
		info.setAttribute("width", avatar.height);
		info.setAttribute("type", avatar.type);
		return publish("urn:xmpp:avatar:metadata", item);
	}


	public IqPacket retrievePepAvatar(final Avatar avatar) {
		final Element item = new Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final IqPacket packet = retrieve("urn:xmpp:avatar:data", item);
		packet.setTo(avatar.owner);
		return packet;
	}

	public IqPacket retrieveVcardAvatar(final Avatar avatar) {
		final IqPacket packet = new IqPacket(IqPacket.TYPE.GET);
		packet.setTo(avatar.owner);
		packet.addChild("vCard", "vcard-temp");
		return packet;
	}

	public IqPacket retrieveAvatarMetaData(final Jid to) {
		final IqPacket packet = retrieve("urn:xmpp:avatar:metadata", null);
		if (to != null) {
			packet.setTo(to);
		}
		return packet;
	}
/*
	public IqPacket retrieveDeviceIds(final Jid to) {
		final IqPacket packet = retrieve(AxolotlService.PEP_DEVICE_LIST, null);
		if(to != null) {
			packet.setTo(to);
		}
		return packet;
	}

	public IqPacket retrieveBundlesForDevice(final Jid to, final int deviceid) {
		final IqPacket packet = retrieve(AxolotlService.PEP_BUNDLES+":"+deviceid, null);
		packet.setTo(to);
		return packet;
	}

	public IqPacket retrieveVerificationForDevice(final Jid to, final int deviceid) {
		final IqPacket packet = retrieve(AxolotlService.PEP_VERIFICATION+":"+deviceid, null);
		packet.setTo(to);
		return packet;
	}

	public IqPacket publishDeviceIds(final Set<Integer> ids) {
		final Element item = new Element("item");
		final Element list = item.addChild("list", AxolotlService.PEP_PREFIX);
		for(Integer id:ids) {
			final Element device = new Element("device");
			device.setAttribute("id", id);
			list.addChild(device);
		}
		return publish(AxolotlService.PEP_DEVICE_LIST, item);
	}

	public IqPacket publishBundles(final SignedPreKeyRecord signedPreKeyRecord, final IdentityKey identityKey,
	                               final Set<PreKeyRecord> preKeyRecords, final int deviceId) {
		final Element item = new Element("item");
		final Element bundle = item.addChild("bundle", AxolotlService.PEP_PREFIX);
		final Element signedPreKeyPublic = bundle.addChild("signedPreKeyPublic");
		signedPreKeyPublic.setAttribute("signedPreKeyId", signedPreKeyRecord.getId());
		ECPublicKey publicKey = signedPreKeyRecord.getKeyPair().getPublicKey();
		signedPreKeyPublic.setContent(Base64.encodeToString(publicKey.serialize(),Base64.DEFAULT));
		final Element signedPreKeySignature = bundle.addChild("signedPreKeySignature");
		signedPreKeySignature.setContent(Base64.encodeToString(signedPreKeyRecord.getSignature(),Base64.DEFAULT));
		final Element identityKeyElement = bundle.addChild("identityKey");
		identityKeyElement.setContent(Base64.encodeToString(identityKey.serialize(), Base64.DEFAULT));

		final Element prekeys = bundle.addChild("prekeys", AxolotlService.PEP_PREFIX);
		for(PreKeyRecord preKeyRecord:preKeyRecords) {
			final Element prekey = prekeys.addChild("preKeyPublic");
			prekey.setAttribute("preKeyId", preKeyRecord.getId());
			prekey.setContent(Base64.encodeToString(preKeyRecord.getKeyPair().getPublicKey().serialize(), Base64.DEFAULT));
		}

		return publish(AxolotlService.PEP_BUNDLES+":"+deviceId, item);
	}
	*/




	public IqPacket generateGetBlockList() {
		final IqPacket iq = new IqPacket(IqPacket.TYPE.GET);
		iq.addChild("blocklist", Xmlns.BLOCKING);

		return iq;
	}

	public IqPacket generateSetBlockRequest(final Jid jid, boolean reportSpam) {
		final IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
		final Element block = iq.addChild("block", Xmlns.BLOCKING);
		final Element item = block.addChild("item").setAttribute("jid", jid.toBareJid().toString());
		if (reportSpam) {
			item.addChild("report", "urn:xmpp:reporting:0").addChild("spam");
		}
		Log.d(Config.LOGTAG,iq.toString());
		return iq;
	}

	public IqPacket generateSetUnblockRequest(final Jid jid) {
		final IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
		final Element block = iq.addChild("unblock", Xmlns.BLOCKING);
		block.addChild("item").setAttribute("jid", jid.toBareJid().toString());
		return iq;
	}

	public IqPacket generateSetPassword(final Account account, final String newPassword) {
		final IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		packet.setTo(account.getServer());
		final Element query = packet.addChild("query", Xmlns.REGISTER);
		final Jid jid = account.getJid();
		query.addChild("username").setContent(jid.getLocalpart());
		query.addChild("password").setContent(newPassword);
		return packet;
	}

	public IqPacket changeAffiliation(Conversation conference, Jid jid, String affiliation) {
		List<Jid> jids = new ArrayList<>();
		jids.add(jid);
		return changeAffiliation(conference,jids,affiliation);
	}

	public IqPacket changeAffiliation(Conversation conference, List<Jid> jids, String affiliation) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		packet.setTo(conference.getJid().toBareJid());
		packet.setFrom(conference.getAccount().getJid());
		Element query = packet.query("http://jabber.org/protocol/muc#admin");
		for(Jid jid : jids) {
			Element item = query.addChild("item");
			item.setAttribute("jid", jid.toString());
			item.setAttribute("affiliation", affiliation);
		}
		return packet;
	}

	public IqPacket changeRole(Conversation conference, String nick, String role) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		packet.setTo(conference.getJid().toBareJid());
		packet.setFrom(conference.getAccount().getJid());
		Element item = packet.query("http://jabber.org/protocol/muc#admin").addChild("item");
		item.setAttribute("nick", nick);
		item.setAttribute("role", role);
		return packet;
	}



	public IqPacket generateCreateAccountWithCaptcha(Account account, String id, Data data) {
		final IqPacket register = new IqPacket(IqPacket.TYPE.SET);
		register.setFrom(account.getJid().toBareJid());
		register.setTo(account.getServer());
		register.setId(id);
		Element query = register.query("jabber:iq:register");
		if (data != null) {
			query.addChild(data);
		}
		return register;
	}

	public IqPacket pushTokenToAppServer(Jid appServer, String token, String deviceId) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		packet.setTo(appServer);
		Element command = packet.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("node","register-push-gcm");
		command.setAttribute("action","execute");
		Data data = new Data();
		data.put("token", token);
		data.put("device-id", deviceId);
		data.submit();
		command.addChild(data);
		return packet;
	}

	public IqPacket enablePush(Jid jid, String node, String secret) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		Element enable = packet.addChild("enable","urn:xmpp:push:0");
		enable.setAttribute("jid",jid.toString());
		enable.setAttribute("node", node);
		Data data = new Data();
		data.setFormType("http://jabber.org/protocol/pubsub#publish-options");
		data.put("secret",secret);
		data.submit();
		enable.addChild(data);
		return packet;
	}

	public IqPacket queryAffiliation(Conversation conversation, String affiliation) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.GET);
		packet.setTo(conversation.getJid().toBareJid());
		packet.query("http://jabber.org/protocol/muc#admin").addChild("item").setAttribute("affiliation",affiliation);
		return packet;
	}

	public static Bundle defaultRoomConfiguration() {
		Bundle options = new Bundle();
		options.putString("muc#roomconfig_persistentroom", "1");
		options.putString("muc#roomconfig_membersonly", "1");
		options.putString("muc#roomconfig_publicroom", "0");
		options.putString("muc#roomconfig_whois", "anyone");
		return options;
	}

	public IqPacket generateCommand(Contact contact, String command, int type) {
		IqPacket packet = new IqPacket(IqPacket.TYPE.SET);
		packet.setTo(contact.getJid().toFullinJid());
		packet.setFrom(contact.getAccount().getJid().toFullinJid());
		Element x = new Element("query");
		x.setAttribute("xmlns", "http://sublate.com/command");
		Element action = new Element("action");
		action.setContent(command);
		x.addChild(action);
		packet.addChild(x);
		return packet;
	}

}
