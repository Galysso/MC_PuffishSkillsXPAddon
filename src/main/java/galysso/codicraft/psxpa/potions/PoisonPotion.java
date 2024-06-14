package galysso.codicraft.psxpa.potions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;


public class PoisonPotion {
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, "codicraft");
    public static final RegistryObject<Potion> TEST_POTION = POTIONS.register("test_potion", () -> new Potion(new MobEffectInstance(MobEffects.POISON, 9600)));

    public static void registerPotions() {
        System.out.println("poisonPotion constructor");
        POTIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
