package com.tterrag.k9.irc;


import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tterrag.k9.commands.api.CommandContext;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.MessageChannel;

public enum IRC {
    
    INSTANCE;
    
    private PircBotX bot;
    
    private final Multimap<String, MessageChannel> relays = HashMultimap.create();
    private final Map<Channel, String> sendableChannels = new HashMap<>();
    
    public void connect(String username, String password) {
        Configuration<PircBotX> esper = new Configuration.Builder<>().setAutoReconnect(true).setLogin(username).setNickservPassword(password).setServer("irc.esper.net", 6667).addListener(new Listener()).setName(username).buildConfiguration();
        bot = new PircBotX(esper);
        try {
            bot.startBot();
        } catch(IOException | IrcException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addChannel(String channel, MessageChannel relay, boolean readonly) {
       if (bot != null) { 
           bot.sendIRC().joinChannel(channel);
           relays.put(channel, relay);
           if (!readonly) {
               sendableChannels.put(relay, channel);
           }
       }
    }
    
    public void removeChannel(String channel, Channel relay) {
        Collection<MessageChannel> chans = relays.get(channel);
        if (chans.remove(relay) && chans.isEmpty()) {
            relays.removeAll(channel);
            sendableChannels.remove(relay);
            bot.sendRaw().rawLine("PART " + channel);
        }
    }
    
    public void onMessageRecieved(MessageCreateEvent event) {
        if (bot == null) return;
        Channel chan = event.getChannel();
        for (Object e : sendableChannels.entrySet()) {
            if (e.getKey().equals(chan)) {
                bot.sendIRC().message(e.getValue(), 
                        "<" + event.getMessage().getAuthor().getDisplayName(event.getGuild()) + "> " + event.getMessage().getFormattedContent());
            }
        }
    }
    
    private class Listener extends ListenerAdapter<PircBotX> {
        
        @Override
        public void onMessage(MessageEvent<PircBotX> event) throws Exception {
            if (event.getUser().getNick().startsWith("Not-")) return; // Ignore notification bots
            Collection<Channel> chans = relays.get(event.getChannel().getName());
            for (Channel channel : chans) {
                RequestBuffer.request(() -> channel.sendMessage(CommandContext.sanitize(channel.getGuild(), "<" + event.getUser().getNick() + "> " + event.getMessage())));
            }
        }
    }
}
