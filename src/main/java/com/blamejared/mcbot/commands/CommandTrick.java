package com.blamejared.mcbot.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blamejared.mcbot.commands.CommandTrick.TrickData;
import com.blamejared.mcbot.commands.api.Argument;
import com.blamejared.mcbot.commands.api.Command;
import com.blamejared.mcbot.commands.api.CommandContext;
import com.blamejared.mcbot.commands.api.CommandException;
import com.blamejared.mcbot.commands.api.CommandPersisted;
import com.blamejared.mcbot.commands.api.CommandRegistrar;
import com.blamejared.mcbot.commands.api.Flag;
import com.blamejared.mcbot.trick.Trick;
import com.blamejared.mcbot.trick.TrickClojure;
import com.blamejared.mcbot.trick.TrickFactories;
import com.blamejared.mcbot.trick.TrickSimple;
import com.blamejared.mcbot.util.SaveHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.Value;

@Command
public class CommandTrick extends CommandPersisted<Map<String, TrickData>> {
    
    @Value
    public static class TrickData {
        String type, input;
        long owner;
    }
    
    public static final String DEFAULT_TYPE = "str";
    
    private static final Pattern ARG_SPLITTER = Pattern.compile("(\"(?<quoted>.+?)(?<![^\\\\]\\\\)\")|(?<unquoted>\\S+)");
    
    private static final Flag FLAG_ADD = new SimpleFlag('a', "add", "Add a new trick.", false);
    private static final Flag FLAG_TYPE = new SimpleFlag('t', "type", "The type of trick, aka the language.", true) {
        
        @Override
        public String description() {
            return super.description() + " Possible values: `" + Arrays.toString(TrickFactories.INSTANCE.getTypes())  + "`. Default: `" + DEFAULT_TYPE + "`";
        }
    };
    private static final Flag FLAG_GLOBAL = new SimpleFlag('g', "global", "If true, the trick will be globally available. Only usable by admins.", false);
    
    private static final Argument<String> ARG_TRICK = new WordArgument("trick", "The trick to invoke", true);
    private static final Argument<String> ARG_PARAMS = new SentenceArgument("params", "The parameters to pass to the trick, or when adding a trick, the content of the trick, script or otherwise.", false) {
        @Override
        public boolean required(Collection<Flag> flags) {
            return flags.contains(FLAG_ADD);
        }
    };
    
    private SaveHelper<Map<String, TrickData>> globalHelper;
    private Map<String, TrickData> globalTricks;
    
    private final Map<Long, Map<String, Trick>> trickCache = new HashMap<>();

    public CommandTrick() {
        super("trick", false, HashMap::new);
    }
    
    @Override
    public void init(File dataFolder, Gson gson) {
        super.init(dataFolder, gson);

        globalHelper = new SaveHelper<Map<String,TrickData>>(dataFolder, gson, new HashMap<>());
        globalTricks = globalHelper.fromJson("global_tricks.json", getDataType());
        
        TrickFactories.INSTANCE.addFactory(DEFAULT_TYPE, TrickSimple::new);
        
        final CommandClojure clj = (CommandClojure) CommandRegistrar.INSTANCE.findCommand("clj");
        TrickFactories.INSTANCE.addFactory("clj", code -> new TrickClojure(clj, code));
    }
    
    @Override
    protected TypeToken<Map<String, TrickData>> getDataType() {
        return new TypeToken<Map<String, TrickData>>(){};
    }

    @Override
    public void process(CommandContext ctx) throws CommandException {
        if (ctx.hasFlag(FLAG_ADD)) {
            String type = ctx.getFlag(FLAG_TYPE);
            TrickData data = new TrickData(type == null ? DEFAULT_TYPE : type, ctx.getArg(ARG_PARAMS), ctx.getAuthor().getLongID());
            final String trick = ctx.getArg(ARG_TRICK);
            if (ctx.hasFlag(FLAG_GLOBAL)) {
                if (!CommandRegistrar.isAdmin(ctx.getAuthor())) {
                    throw new CommandException("You do not have permission to add global tricks.");
                }
                globalTricks.put(trick, data);
                globalHelper.writeJson("global_tricks.json", globalTricks);
                trickCache.getOrDefault(null, new HashMap<>()).remove(trick);
            } else {
                if (ctx.getGuild() == null) {
                    throw new CommandException("Cannot add local tricks in private message.");
                }
                storage.get(ctx).put(trick, data);
                trickCache.getOrDefault(ctx.getGuild().getLongID(), new HashMap<>()).remove(trick);
            }
            ctx.reply("Added new trick!");
        } else {
            TrickData data = ctx.getGuild() == null ? null : storage.get(ctx).get(ctx.getArg(ARG_TRICK));
            boolean global = false;
            if (data == null) {
                data = globalTricks.get(ctx.getArg(ARG_TRICK));
                if (data == null) {
                    throw new CommandException("No such trick!");
                }
                global = true;
            }
            
            Map<String, Trick> tricks = trickCache.computeIfAbsent(global ? null : ctx.getGuild().getLongID(), id -> new HashMap<>());
            
            final TrickData td = data;
            Trick trick = tricks.computeIfAbsent(ctx.getArg(ARG_TRICK), input -> TrickFactories.INSTANCE.create(td.getType(), td.getInput()));

            String args = ctx.getArgOrElse(ARG_PARAMS, "");
            Matcher matcher = ARG_SPLITTER.matcher(args);
            List<String> splitArgs = new ArrayList<>();
            while (matcher.find()) {
                String arg = matcher.group("quoted");
                if (arg == null) {
                    arg = matcher.group("unquoted");
                }
                splitArgs.add(arg);
            }
            
            ctx.replyBuffered(trick.process(splitArgs.toArray()));
        }
    }

    @Override
    public String getDescription() {
        return "Teach K9 a new trick!";
    }
}