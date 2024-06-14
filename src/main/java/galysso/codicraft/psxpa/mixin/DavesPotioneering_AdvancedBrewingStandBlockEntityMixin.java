package galysso.codicraft.psxpa.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeProvider;
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

@Mixin(value = CAdvancedBrewingStandBlockEntity.class, remap = false)
public abstract class DavesPotioneering_AdvancedBrewingStandBlockEntityMixin extends BlockEntity implements MenuProvider {
    private UUID ownerUUID;
    private ItemStack[] potionsBeforeBrewing;
    private boolean processedBrewingEntirely;
    private boolean brewTimeWasIncreased;

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
    /*@Accessor("fuel")
    public abstract void setFuel(int fuel);*/

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(BlockEntityType $$0, BlockPos $$1, BlockState $$2, CallbackInfo ci) {
        this.ownerUUID = null;
        this.potionsBeforeBrewing = new ItemStack[3];
        this.processedBrewingEntirely = false;
        this.brewTimeWasIncreased = false;
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

    public void updateBrewingTracking() {
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
            } else if (getBrewTime() == 0) {
                if (this.processedBrewingEntirely) {
                    int nbPotionsBrewed = 0;
                    NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
                    for (int i = 0; i < 3; i++) {
                        if (!(getHandler().$getStackInSlot(i).isEmpty() || potionsBeforeBrewing[i].isEmpty()) && areDifferentPotions(getHandler().$getStackInSlot(i), potionsBeforeBrewing[i])) {
                            nbPotionsBrewed += getHandler().$getStackInSlot(i).getCount();
                            itemStacks.set(i, getHandler().$getStackInSlot(i));
                        }
                    }
                    if (nbPotionsBrewed == 1) {
                        this.getLevel().getPlayerByUUID(ownerUUID).sendSystemMessage(Component.literal("1 potion est prête."));
                    } else {
                        this.getLevel().getPlayerByUUID(ownerUUID).sendSystemMessage(Component.literal(nbPotionsBrewed + " potions sont prêtes."));
                    }
                    grantExperience((ServerPlayer) this.getLevel().getPlayerByUUID(this.ownerUUID), itemStacks);
                }
                this.processedBrewingEntirely = false;
                this.brewTimeWasIncreased = false;
            }
        }
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void InjectServerTickAtTail(Level p_155286_, BlockPos p_155287_, BlockState p_155288_, CAdvancedBrewingStandBlockEntity brewingStand, CallbackInfo ci) {
        ((DavesPotioneering_AdvancedBrewingStandBlockEntityMixin) (Object) brewingStand).updateBrewingTracking();
    }
}