package cy.jdkdigital.productivebees;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ProductiveBeesConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;
    public static final General GENERAL = new General(BUILDER);
    public static final Bees BEES = new Bees(BUILDER);

    static {
        CONFIG = BUILDER.build();
    }

    public static class General {
        public final ForgeConfigSpec.ConfigValue<Boolean> enableItemConverting;
        public final ForgeConfigSpec.ConfigValue<Integer> itemTickRate;

        public General(ForgeConfigSpec.Builder builder) {
            builder.push("General");

            enableItemConverting = builder
                    .comment("Use items to change the type of a bee.", "If false, productive bees can only be obtained through breeding.")
                    .define("enableItemConverting", true);

            itemTickRate = builder
                    .comment("How often should a bee attempt to generate items while in the hive.")
                    .define("itemTickRate", 600);

            builder.pop();
        }
    }

    public static class Bees {
        public final ForgeConfigSpec.ConfigValue<Config> itemProductionRules;

        public Bees(ForgeConfigSpec.Builder builder) {
            builder.push("Bees");

            Config productionRates = Config.inMemory();

            productionRates.add("minecraft:bee",  0.25D);
            productionRates.add("productivebees:creeper_bee", 0.10D);
            productionRates.add("productivebees:diamond_bee", 0.10D);
            productionRates.add("productivebees:emerald_bee", 0.05D);
            productionRates.add("productivebees:ender_bee", 0.05D);
            productionRates.add("productivebees:glowing_bee", 0.25D);
            productionRates.add("productivebees:gold_bee", 0.10D);
            productionRates.add("productivebees:iron_bee", 0.10D);
            productionRates.add("productivebees:lapis_bee", 0.20D);
            productionRates.add("productivebees:magmatic_bee", 0.20D);
            productionRates.add("productivebees:quartz_bee", 0.25D);
            productionRates.add("productivebees:redstone_bee", 0.25D);
            productionRates.add("productivebees:skeletal_bee", 0.10D);
            productionRates.add("productivebees:zombie_bee", 0.10D);
            productionRates.add("productivebees:wither_bee", 0.01D);

            itemProductionRules = builder
                    .comment("Bee production rates.")
                    .define("itemProductionRates", productionRates);

            builder.pop();
        }
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }
}