package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** English names for every block and the creative tab. */
public class RoundwoodLanguageProvider extends LanguageProvider {
    public RoundwoodLanguageProvider(PackOutput output) {
        super(output, Roundwood.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + Roundwood.MOD_ID, "Roundwood");
        for (Finish finish : FinishCatalog.ALL) {
            add(ModBlocks.door(finish), finish.displayName() + " Round Door");
            add(ModBlocks.window(finish), finish.displayName() + " Round Window");
        }
    }
}
