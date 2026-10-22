package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * Face identity through the real blockstate transforms (0.8.0): for every pose of the ten windows
 * and an alias, either value of {@code face_reversed}, every rotation and both mirrors move the
 * attachment FRONT to exactly the world side the transform takes it to, so two different face trims
 * never swap sides when a structure is rotated or mirrored. Saved NBT is untouched by the transform.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowFaceIdentityGameTests {
    private WindowFaceIdentityGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void everyRotationAndMirrorKeepsEachFaceOnItsWorldSide(GameTestHelper helper) {
        List<Block> blocks = new ArrayList<>(ModBlocks.windows());
        blocks.add(LegacyContent.windows().get(0));
        int checked = 0;
        for (Block block : blocks) {
            WindowPaneBlock window = (WindowPaneBlock) block;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (state.getValue(BlockStateProperties.WATERLOGGED) || state.getValue(WindowPaneBlock.TRIMMED) || connected(state)) {
                    continue;
                }
                Direction front = window.attachmentFront(state);
                for (Rotation rotation : Rotation.values()) {
                    BlockState turned = state.rotate(rotation);
                    Direction expected = rotation.rotate(front);
                    if (window.attachmentFront(turned) != expected) {
                        check(false, block + " " + state + " " + rotation + ": front " + window.attachmentFront(turned) + ", expected " + expected);
                    }
                    checked++;
                }
                for (Mirror mirror : new Mirror[] {Mirror.LEFT_RIGHT, Mirror.FRONT_BACK}) {
                    BlockState reflected = state.mirror(mirror);
                    Direction expected = mirror.mirror(front);
                    if (window.attachmentFront(reflected) != expected) {
                        check(false, block + " " + state + " " + mirror + ": front " + window.attachmentFront(reflected) + ", expected " + expected);
                    }
                    checked++;
                }
            }
        }
        check(checked > 1000, "too few transforms checked: " + checked);
        helper.succeed();
    }

    private static boolean connected(BlockState state) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                return true;
            }
        }
        return false;
    }
}
