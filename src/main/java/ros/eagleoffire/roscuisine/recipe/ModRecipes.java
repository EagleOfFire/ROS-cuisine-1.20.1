package ros.eagleoffire.roscuisine.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ros.eagleoffire.roscuisine.ROSCuisine;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ROSCuisine.MODID);

    public static final RegistryObject<RecipeSerializer<FumoirRecipes>> FUMOIR_SERIALIZER =
            SERIALIZERS.register("fumoir", () -> FumoirRecipes.Serializer.INSTANCE);

    public static void register(IEventBus eventBus){
        SERIALIZERS.register(eventBus);
    }
}
