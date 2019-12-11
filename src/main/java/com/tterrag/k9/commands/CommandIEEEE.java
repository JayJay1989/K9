package com.tterrag.k9.commands;

import com.tterrag.k9.commands.api.Argument;
import com.tterrag.k9.commands.api.Command;
import com.tterrag.k9.commands.api.CommandBase;
import com.tterrag.k9.commands.api.CommandContext;

import discord4j.core.DiscordClient;

import reactor.core.publisher.Mono;

import java.math.BigInteger;

@Command
public class CommandIEEEE extends CommandBase {

    private static final Argument<String> ARG_QUERY = new SentenceArgument("query", "Decimal to convert from", true);

    public CommandIEEEE() {super("ieeee", false);}

    @Override
    public Mono<?> process(CommandContext ctx) {
        String arg = ctx.getArg(ARG_QUERY);

        return ctx.getClient().getSelf()
                .flatMap(u -> ctx.reply(spec ->
                        spec.setDescription("Convert Decimal To IEEE 754")
                        .setTitle("Converter")
                        .addField("Solution:",converter(arg), false)
                ));
    }

    public String converter(String args)
    {
        String myLong = args.replace("\"", "");
        float l = Float.parseFloat(myLong+"f");
        int bits = Float.floatToIntBits(l);
        int signbit = bits >>> 31;
        boolean isSignPositive = (signbit == 0);
        int exponent = (bits >>> 23 & ((1 << 8) - 1)) - ((1 << 7) - 1);
        int fraction = bits & ((1 << 23) - 1);

        String exponentBits = String.format("%08d", new BigInteger(Long.toBinaryString(127+exponent)));
        String fractionBits = String.format("%023d", new BigInteger(Long.toBinaryString(fraction)));
        BigInteger value = new BigInteger(""+signbit+ "" + exponentBits + "" + fractionBits);
        String[] numbers = String.format("%032d", value).split("(?<=\\G.{4})");
        StringBuilder hex = new StringBuilder();

        for (int i = 0; i < numbers.length; i += 1){
            int decimal = Integer.parseInt(numbers[i],2);
            String hexStr = Integer.toString(decimal,16);
            hex.append(hexStr);
        }

        return(
                "Start: (" + myLong + ")DEC" +
                        "\nSign: " + signbit + (isSignPositive ? " (positief)" : " (negatief)" ) +
                        "\nExponent: " + " 127 + " + exponent + " = " + (127+exponent) + " => " + exponentBits +
                        "\nFraction: " + fractionBits  +
                        "\n0x" + hex.toString().toUpperCase() + " = " + signbit + " " + exponentBits + " " + fractionBits
        );
    }

    @Override
    public String getDescription(CommandContext ctx) {
        return "Convert a decimal to a 32bit Floating Point Binary and Hex";
    }

    @Override
    public void onRegister(DiscordClient client) {
        super.onRegister(client);
    }

}
