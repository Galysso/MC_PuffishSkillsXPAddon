package galysso.codicraft.psxpa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.davespotioneering.blockentity.CAdvancedBrewingStandBlockEntity;


import java.util.UUID;

import tfar.davespotioneering.inventory.BasicInventoryBridge;
import tfar.davespotioneering.menu.CAdvancedBrewingStandMenu;

import static galysso.codicraft.psxpa.CodiCraft_PuffishSkillsXPAddon.areDifferentPotions;
import static galysso.codicraft.psxpa.experience.sources.BrewingExperienceSource.grantExperience;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static net.darkhax.gamestages.GameStageHelper.hasStage;

@Mixin(value = CAdvancedBrewingStandBlockEntity.class, remap = false)
public abstract class DavesPotioneering_AdvancedBrewingStandBlockEntityMixin extends BlockEntity implements MenuProvider {
    private UUID ownerUUID;
    private ItemStack[] potionsBeforeBrewing;
    private boolean processedBrewingEntirely;
    private boolean brewTimeWasIncreased;
    private ItemStack failedPotions;

    public DavesPotioneering_AdvancedBrewingStandBlockEntityMixin(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Accessor("handler")
    public abstract BasicInventoryBridge getHandler();

    @Accessor("brewTime")
    public abstract int getBrewTime();

    @Accessor("brewTime")
    public abstract void setBrewTime(int brewTime);

    @Accessor("data")
    public abstract ContainerData getData();

    @Accessor("fuel")
    public abstract int getFuel();

    @Shadow
    protected int fuel;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(BlockEntityType $$0, BlockPos $$1, BlockState $$2, CallbackInfo ci) {
        this.ownerUUID = null;
        this.potionsBeforeBrewing = new ItemStack[3];
        this.processedBrewingEntirely = false;
        this.brewTimeWasIncreased = false;
        CompoundTag failedTag = new CompoundTag();
        failedTag.putString("Potion", "codicraft:failed");
        CompoundTag displayName = new CompoundTag();
        displayName.put("Name", StringTag.valueOf("{\"text\":\"Potion râtée\",\"italic\":false}"));
        failedTag.put("display", displayName);
        this.failedPotions = new ItemStack(Items.POTION);
        this.failedPotions.setTag(failedTag);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        if (this.ownerUUID == null) {
            if (getPersistentData().hasUUID("OwnerUUID")) {
                this.ownerUUID = getPersistentData().getUUID("OwnerUUID");
            } else {
                this.ownerUUID = player.getUUID();
                getPersistentData().putUUID("OwnerUUID", ownerUUID);
                saveAdditional(getPersistentData());
                player.sendSystemMessage(Component.literal("Tu es désormais propriétaire de cet alambic. Personne d'autre ne pourra l'utiliser jusqu'à ce que celui-ci soit retiré puis replacé."));
            }
        }
        if (this.ownerUUID != null && !this.ownerUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Cet alambic appartient déjà à " + this.getLevel().getPlayerByUUID(ownerUUID).getName().getString() + ", tu ne peux pas l'utiliser."));
            return null;
        } else {
            return new CAdvancedBrewingStandMenu(id, playerInventory, getHandler(), getData(), (CAdvancedBrewingStandBlockEntity) (Object) this);
        }
    }

    public void updateBrewingTrackingBefore() {
        if (ownerUUID != null) {
            if (getBrewTime() > 0 && !this.brewTimeWasIncreased) {
                this.brewTimeWasIncreased = true;
                setBrewTime(3000);
                fuel -= 9;
            } else if (getBrewTime() == 1) {
                if (!this.processedBrewingEntirely) {
                    this.processedBrewingEntirely = true;
                    for (int i = 0; i < 3; i++) {
                        potionsBeforeBrewing[i] = getHandler().$getStackInSlot(i).copy();
                    }
                }
            }
        }
    }

    public void updateBrewingTrackingAfter() {
        if (ownerUUID != null && getBrewTime() == 0) {
            if (this.processedBrewingEntirely) {
                int nbPotionsBrewed = 0;
                int nbPotionsFailed = 0;
                NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
                for (int i = 0; i < 3; i++) {
                    if (!(getHandler().$getStackInSlot(i).isEmpty() || potionsBeforeBrewing[i].isEmpty()) && areDifferentPotions(getHandler().$getStackInSlot(i), potionsBeforeBrewing[i])) {
                        CompoundTag tag = getHandler().$getStackInSlot(i).getTag();
                        if (tag != null && tag.contains("Potion") && !hasStage(this.getLevel().getPlayerByUUID(ownerUUID), tag.getString("Potion").split(":")[1])) {
                            nbPotionsFailed += getHandler().$getStackInSlot(i).getCount();
                            this.failedPotions.setCount(getHandler().$getStackInSlot(i).getCount());
                            getHandler().$setStackInSlot(i, this.failedPotions.copy());
                        } else {
                            nbPotionsBrewed += getHandler().$getStackInSlot(i).getCount();
                            itemStacks.set(i, getHandler().$getStackInSlot(i));
                        }
                    }
                }
                String message = "";
                if (nbPotionsBrewed == 1) {
                    message += "1 potion est prête.";
                } else if (nbPotionsBrewed > 1) {
                    message += nbPotionsBrewed + " potions sont prêtes.";
                }
                if (nbPotionsFailed == 1) {
                    message += " 1 potion n'a pas pu être créée car tu n'as pas débloqué la recette.";
                } else if (nbPotionsFailed > 1) {
                    message += " " + nbPotionsFailed + " potions n'ont pas pu être créées car tu n'as pas débloqué leurs recettes.";
                }
                this.getLevel().getPlayerByUUID(ownerUUID).sendSystemMessage(Component.literal(message));
                grantExperience((ServerPlayer) this.getLevel().getPlayerByUUID(this.ownerUUID), itemStacks);
            }
            this.processedBrewingEntirely = false;
            this.brewTimeWasIncreased = false;
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void InjectServerTickAtHead(Level p_155286_, BlockPos p_155287_, BlockState p_155288_, CAdvancedBrewingStandBlockEntity brewingStand, CallbackInfo ci) {
        ((DavesPotioneering_AdvancedBrewingStandBlockEntityMixin) (Object) brewingStand).updateBrewingTrackingBefore();
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void InjectServerTickAtTail(Level p_155286_, BlockPos p_155287_, BlockState p_155288_, CAdvancedBrewingStandBlockEntity brewingStand, CallbackInfo ci) {
        ((DavesPotioneering_AdvancedBrewingStandBlockEntityMixin) (Object) brewingStand).updateBrewingTrackingAfter();
    }
}