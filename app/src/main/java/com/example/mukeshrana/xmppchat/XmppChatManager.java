package com.example.mukeshrana.xmppchat;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XmppChatManager {


    private static boolean connected = false;
    private boolean loggedin = false;
    private static boolean isconnecting = false;
    private boolean chat_created = false;
    private String loginUser;
    private String passwordUser, toJID;
    private CommunicationListener mCommunicationListener;
    public static XmppChatManager instance = null;
    private static XMPPTCPConnection mConnection;
    private Activity context;
    private XMPPConnectionListener connectionListener;

    public static XmppChatManager getInstance(Activity context, String user,
                                              String pass, String toJid) {

        Log.e("xmpp1", "getInstance ->" + instance);
        if (instance == null) {
            instance = new XmppChatManager(context, user, pass, toJid);
        }
        return instance;
    }


    public XmppChatManager(Activity context, String logiUser,
                           String passwordser, String toJID) {

        Log.e("enter 1", "init ");
        this.loginUser = logiUser;
        this.toJID = toJID;
//        mCommunicationListener = (CommunicationListener) context;
        this.passwordUser = passwordser;
        this.context = context;
        Log.e("enter init", "init ");

        initialiseConnection();
    }

    private void initialiseConnection() {
        // Add SSL certificate
        SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
        SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
        SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

        Log.e("before try", "initialiseConnection");
        try {
            Log.e("enter try", "initialiseConnection");
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()

                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setHost("kanonhealth.com")
                    .setXmppDomain("kanonhealth.com")
                    .setPort(5222)
                    .enableDefaultDebugger()
                    .build();

            XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
            XMPPTCPConnection.setUseStreamManagementDefault(true);
            mConnection = new XMPPTCPConnection(config);
            connectionListener = new XMPPConnectionListener();
            mConnection.addConnectionListener(connectionListener);
            mConnection.setUseStreamManagement(true);
        } catch (XmppStringprepException e) {
            Log.e("xmpp catch", e.getCausingString());
            e.printStackTrace();
        }

        // set packet reply timeout time
        mConnection.setReplyTimeout(10000);

        // Add reconnect manager
        ReconnectionManager.getInstanceFor(mConnection).enableAutomaticReconnection();
        ServerPingWithAlarmManager.onCreate(context);
        PingManager.setDefaultPingInterval(600);
        ServerPingWithAlarmManager.getInstanceFor(mConnection).setEnabled(true);
        ReconnectionManager.setEnabledPerDefault(true);
        Log.e("xmpp1", ServerPingWithAlarmManager.getInstanceFor(mConnection).isEnabled() + "");

        // Add ping manager here
        PingManager.getInstanceFor(mConnection).registerPingFailedListener(new PingFailedListener() {
            @Override
            public void pingFailed() {
//                disconnect();
                initialiseConnection();
            }
        });

        // Set message incoming listener
//        incomingListener = new IncomingListener();
//        ChatManager.getInstanceFor(mConnection).addIncomingListener(incomingListener);

        // Connect
        new ConnectionThread().execute("");
//        addStanzaListner();
//        addIncomingListener();
//        stanzaLisetnerNew();
    }


    public void sendOneToOneMessage(ChatModelNew message) {

        try {
            if (!chat_created) {
                oneToOneChat = mChatManager.chatWith(JidCreate.entityBareFrom(message.getToJID()));
                Log.e("xmpp", "in one to one send msg");
                chat_created = true;
            }
            final Message chatMessage = new Message();
            chatMessage.setBody(message.getMessageBody().toJson());
            chatMessage.setType(Message.Type.chat);
            oneToOneChat.send(chatMessage);

            Log.e("xmpp", "message send " + message.getMessageBody().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            initialiseConnection();
        }
    }

    class IncomingListener implements IncomingChatMessageListener {
        @Override
        public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
            ChatModelNew.Body body = new Gson().fromJson(message.getBody(), ChatModelNew.Body.class);
            Log.e("xmpp", "message received " + message.getBody());
            if (null != message.getBody() && !body.getMessageID().equals("initial_chat")) {
                mCommunicationListener.onCommunication((message).getBody());
            }
        }
    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConnection.disconnect();
                instance = null;
                Log.e("xmpp discpnection", mConnection.isConnected() + "");
                mConnection = null;
            }
        }).start();
    }

    public class XMPPConnectionListener implements ConnectionListener {
        @Override
        public void connected(final XMPPConnection mConnection) {

            Log.e("xmpp", "Connected!");
            connected = true;
            if (!mConnection.isAuthenticated()) {
//                login();
//                mConnection.addConnectionListener(connectionListener);
//                addIncomingListener();
            }
        }

        @Override
        public void connectionClosed() {

            Log.e("xmpp", "ConnectionCLosed!");
            connected = false;
            chat_created = false;
            loggedin = false;
//            ChatManager.getInstanceFor(mConnection).removeListener(incomingChatMessageListener);
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {

            Log.e("xmpp", "ConnectionClosedOn Error! " + arg0.getLocalizedMessage());
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void authenticated(XMPPConnection arg0, boolean arg1) {
            Log.e("xmpp", "Authenticated!");
            loggedin = true;

            //      ChatManager.getInstanceFor(mConnection).addChatListener(mChatManagerListener);

            chat_created = false;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    static class ConnectionThread extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... args) {
            final String caller = args[0];
            Log.e("xmpp", String.valueOf(mConnection.isConnected()));
            if (mConnection.isConnected()) {
                Log.e("xmpp", "already connected");
                return false;
            } else {
                Log.e("xmpp", "True");
            }
            isconnecting = true;
            Log.e("xmpp1", "=>connecting....");

            try {
                mConnection.connect();
//                ChatManager.getInstanceFor(mConnection).chatWith(JidCreate.entityBareFrom(caller));
                DeliveryReceiptManager deliveryReceiptManager =
                        DeliveryReceiptManager.getInstanceFor(mConnection);
                deliveryReceiptManager.autoAddDeliveryReceiptRequests();
                deliveryReceiptManager.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
                deliveryReceiptManager.addReceiptReceivedListener(new ReceiptReceivedListener() {

                    @Override
                    public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {

                        // Mark delivered here

                        Message message = new Message();
                        message.setStanzaId(receipt.getStanzaId());
                        //                message.setType(Message.Type.headline);
//                        try {
//                            EntityBareJid entityJid = JidCreate.entityBareFrom(caller);
//                            ChatManager.getInstanceFor(mConnection)
//                                    .chatWith(entityJid)
//                                    .send(message);
//                        } catch (Exception e) {
//                            Log.e("Connect", "Chat not created." + caller);
//                            e.printStackTrace();
//                        }
                    }
                });
                connected = true;
            } catch (Exception e) {
                Log.e("Connect", "Connection not created " + caller);
                Log.e("Connect", e.getMessage());
            }
            return isconnecting = false;
        }
    }

    public interface CommunicationListener {
        void onCommunication(String messageBody);
    }
}
