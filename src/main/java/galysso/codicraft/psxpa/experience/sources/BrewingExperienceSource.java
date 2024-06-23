package galysso.codicraft.psxpa.experience.sources;

import galysso.codicraft.psxpa.CodiCraft_PuffishSkillsXPAddon;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.LegacyCalculation;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;


import net.minecraftforge.common.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class BrewingExperienceSource implements ExperienceSource {
    public static final ResourceLocation ID = new ResourceLocation("codicraft_psxpa", "brewing");
    private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

    private BrewingExperienceSource() {

    }

    public static void register() {
        SkillsAPI.registerExperienceSource(ID, BrewingExperienceSource::parse);
    }

    private static Result<BrewingExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
        return Result.success(new BrewingExperienceSource());
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {

    }

    public static void grantExperience(ServerPlayer player, NonNullList<ItemStack> createdPotions) {
        if (player instanceof ServerPlayer) {
            SkillsAPI.updateExperienceSources(player, experienceSource -> {
                if (experienceSource instanceof BrewingExperienceSource brewingExperienceSource) {
                    return brewingExperienceSource.getValue(player, createdPotions);
                }
                return 0;
            });
        }
    }

    public int getValue(final ServerPlayer player, NonNullList<ItemStack> createdPotions) {
        int xpValue = 0;
        for (ItemStack potion : createdPotions) {
            if (!potion.isEmpty()) {
                CompoundTag tag = createdPotions.get(0).getTag();
                if (tag.contains("xpFactor") && tag.getInt("xpFactor") > 0) {
                    xpValue += tag.getInt("xpFactor") * potion.getCount();
                }
            }
        }
        return xpValue;
    }

    private record Data(ServerPlayer player, LivingEntity entity, ItemStack weapon, DamageSource damageSource, double entityDroppedXp) { }
}
