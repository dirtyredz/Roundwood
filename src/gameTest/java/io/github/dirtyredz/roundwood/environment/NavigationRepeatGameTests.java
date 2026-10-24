package io.github.dirtyredz.roundwood.environment;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.DoorNavigationGameTests;
import io.github.dirtyredz.roundwood.block.trim.TrimNavigationGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** Opt-in independent copies of the passage tests, never success-on-retry. */
@GameTestHolder(Roundwood.MOD_ID)
public final class NavigationRepeatGameTests {
    private record Walk(String name, Consumer<GameTestHelper> test) {}

    @GameTestGenerator
    public static Collection<TestFunction> repeats() {
        List<Walk> walks = List.of(
                new Walk("south", DoorNavigationGameTests::villagerWalksNorthToSouthThroughTheDoor),
                new Walk("north", DoorNavigationGameTests::villagerWalksSouthToNorthThroughTheDoor),
                new Walk("right", DoorNavigationGameTests::villagerWalksThroughARightHingedDoor),
                new Walk("camouflage", DoorNavigationGameTests::villagerWalksThroughACamouflagedDoor),
                new Walk("fitted_south", TrimNavigationGameTests::villagerWalksNorthToSouthThroughADoorWithAFittedFrame),
                new Walk("fitted_north", TrimNavigationGameTests::villagerWalksSouthToNorthThroughADoorWithAFittedFrame),
                new Walk("frame", TrimNavigationGameTests::villagerWalksThroughAFreestandingBrickFrame));
        List<TestFunction> tests = new ArrayList<>();
        // The annotated originals supply copy zero.
        for (int copy = 1; copy < Integer.getInteger("roundwood.navigationRepeats", 1); copy++) {
            for (Walk walk : walks) {
                tests.add(new TestFunction("navigation_repeats", "navigation_" + walk.name() + "_" + copy,
                        "roundwood:empty", 600, 0, true, walk.test()));
            }
        }
        return tests;
    }
}
