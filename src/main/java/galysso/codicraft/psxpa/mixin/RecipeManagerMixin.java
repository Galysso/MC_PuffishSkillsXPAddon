package galysso.codicraft.psxpa.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApplyHead(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        Iterator<Map.Entry<ResourceLocation, JsonElement>> iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, JsonElement> entry = iterator.next();
            ResourceLocation resourceLocation = entry.getKey();
            if (entry.getKey().toString().equals("minecraft:tipped_arrow")) {
                System.out.println("Removing recipe: " + entry.getKey().toString());
                iterator.remove();
            }
        }
    }
}

/*
@Mixin(TippedArrowRecipe.class)
public class CraftingSpecialTippedArrow extends CustomRecipe {

    public CraftingSpecialTippedArrow(ResourceLocation p_252125_, CraftingBookCategory p_249010_) {
        super(p_252125_, p_249010_);
    }

    @Override
    public boolean matches(CraftingContainer craftingContainer, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer craftingContainer, RegistryAccess registryAccess) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }
}*/