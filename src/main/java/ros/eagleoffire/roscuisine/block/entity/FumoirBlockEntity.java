package ros.eagleoffire.roscuisine.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import ros.eagleoffire.roscuisine.block.custom.FumoirBlock;
import ros.eagleoffire.roscuisine.recipe.FumoirRecipes;
import ros.eagleoffire.roscuisine.screen.FumoirMenu;

import java.util.Optional;

public class FumoirBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(11);

    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int INPUT_SLOT_3 = 2;
    private static final int INPUT_SLOT_4 = 3;
    private static final int INPUT_SLOT_5 = 4;
    private static final int INPUT_SLOT_6 = 5;
    private static final int INPUT_SLOT_7 = 6;
    private static final int INPUT_SLOT_8 = 7;
    private static final int INPUT_SLOT_9 = 8;
    private static final int OUTPUT_SLOT = 9;
    private static final int FUEL_SLOT = 10;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;
    private int burnTime = 0;
    private int currentBurnTime = 0;

    public FumoirBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FUMOIR_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> FumoirBlockEntity.this.progress;
                    case 1 -> FumoirBlockEntity.this.maxProgress;
                    case 2 -> FumoirBlockEntity.this.burnTime;
                    case 3 -> FumoirBlockEntity.this.currentBurnTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> FumoirBlockEntity.this.progress = pValue;
                    case 1 -> FumoirBlockEntity.this.maxProgress = pValue;
                    case 2 -> FumoirBlockEntity.this.burnTime = pValue;
                    case 3 -> FumoirBlockEntity.this.currentBurnTime = pValue;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER){
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void drops(){
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++){
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.roscuisine.fumoir");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory inventory, Player player) {
        return new FumoirMenu(pContainerId, inventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("fumoir.progress", progress);
        pTag.putInt("fumoir.burnTime", burnTime);
        pTag.putInt("fumoir.currentBurnTime", currentBurnTime);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("fumoir.progress");
        burnTime = pTag.getInt("fumoir.burnTime");
        currentBurnTime = pTag.getInt("fumoir.currentBurnTime");
    }


    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        boolean isBurning = isBurning();

        if (isBurning) {
            burnTime--;
        }

        if (hasRecipe()) {
            if (!isBurning && canBurn()) {
                startBurning();
            }

            if (isBurning) {
                increaseCraftingProgress();
                setChanged(pLevel, pPos, pState);

                if (hasProgressFinished()) {
                    craftItem();
                    resetProgress();
                }
            }
        } else {
            resetProgress();
        }

        if (isBurning != isBurning()) {
            pLevel.setBlock(pPos, pState, 3);
            setChanged(pLevel, pPos, pState);
        }
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    private boolean canBurn() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        return !fuelStack.isEmpty() && FurnaceBlockEntity.isFuel(fuelStack);
    }

    private void startBurning() {
        ItemStack fuelStack = this.itemHandler.extractItem(FUEL_SLOT, 1, false);
        this.currentBurnTime = ForgeHooks.getBurnTime(fuelStack, null);
        this.burnTime = currentBurnTime;
    }


    private void resetProgress() {
        progress = 0;
    }

    private void craftItem() {
        Optional<FumoirRecipes> recipe = getCurrentRecipe();
        ItemStack result = recipe.get().getResultItem(null);
        this.itemHandler.extractItem(INPUT_SLOT_1, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_2, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_3, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_4, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_5, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_6, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_7, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_8, 1, false);
        this.itemHandler.extractItem(INPUT_SLOT_9, 1, false);

        this.itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(result.getItem(),
                this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + result.getCount()));
    }

    private boolean hasRecipe() {
        Optional<FumoirRecipes> recipe = getCurrentRecipe();

        if (recipe.isEmpty()){
            return false;
        }

        ItemStack result = recipe.get().getResultItem(null);

        return canInsertAmountIntoOutputSlot(result.getCount()) && canInsertItemIntoOutputSlot(result.getItem());
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    private Optional<FumoirRecipes> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());
        for(int i = 0; i< itemHandler.getSlots(); i++){
            inventory.setItem(i, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(FumoirRecipes.Type.INSTANCE, inventory, level);
    }

    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }
}
