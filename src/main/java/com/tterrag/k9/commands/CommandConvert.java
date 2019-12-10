package com.tterrag.k9.commands;

import com.tterrag.k9.commands.api.Argument;
import com.tterrag.k9.commands.api.Command;
import com.tterrag.k9.commands.api.CommandBase;
import com.tterrag.k9.commands.api.CommandContext;

import discord4j.core.DiscordClient;
import reactor.core.publisher.Mono;

public class CommandConvert extends CommandBase {
    private static final Argument<String> ARG_QUERY = new SentenceArgument("query", "Decimal to convert from", true);
    private static final Argument<String> ARG_QUERY2 = new SentenceArgument("query", "Decimal to convert from", true);

    protected CommandConvert() {
        super("", false);
    }

    @Override
    public Mono<?> process(CommandContext ctx) {
        return null;
    }

    @Override
    public String getDescription(CommandContext ctx) {
        return null;
    }

    @Override
    public void onRegister(DiscordClient client) {
        super.onRegister(client);
    }
}
