package maxhyper.dtbwg.growthlogic;

import com.ferreusveritas.dynamictrees.growthlogic.ConiferLogic;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKitConfiguration;
import com.ferreusveritas.dynamictrees.growthlogic.context.DirectionManipulationContext;
import com.ferreusveritas.dynamictrees.growthlogic.context.DirectionSelectionContext;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class SmallRedwoodLogic extends ConiferLogic {

    public SmallRedwoodLogic(ResourceLocation registryName) { super(registryName); }

    @Override
    protected GrowthLogicKitConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration()
                .with(HEIGHT_VARIATION, 6);
    }

    // *-*-*-*-*-*-*-*- %2 < 1
    // **--**--**--**-- %4 < 2
    // ****----****---- %8 < 4
    // ********-------- %16< 8


    @Override
    public int[] populateDirectionProbabilityMap(GrowthLogicKitConfiguration configuration, DirectionManipulationContext context) {
        final Species species = context.species();
        final GrowSignal signal = context.signal();
        final int[] probMap = context.probMap();
        Direction originDir = signal.dir.getOpposite();
        int treeHash = CoordUtils.coordHashCode(signal.rootPos, 2);

        //Alter probability map for direction change
        probMap[0] = 0;//Down is always disallowed for spruce
        probMap[1] = signal.isInTrunk() ? species.getUpProbability() : 0;
        int canopyHeight = species.getLowestBranchHeight() + 4;
        if (signal.delta.getY() == species.getLowestBranchHeight()) {
            int sideHash = treeHash % 16;
            probMap[2] = sideHash % 2 < 1 ? 1 : 0;
            probMap[3] = sideHash % 4 < 2 ? 1 : 0;
            probMap[4] = sideHash % 8 < 4 ? 1 : 0;
            probMap[5] = sideHash < 8 ? 1 : 0;
        } else if (signal.delta.getY() < canopyHeight){
            probMap[2] = probMap[3] = probMap[4] = probMap[5] = 0;
        } else {
            probMap[2] = probMap[3] = probMap[4] = probMap[5] = //Only allow turns when we aren't in the trunk(or the branch is not a twig and step is odd)
                    !signal.isInTrunk() || (signal.isInTrunk() && signal.numSteps % 2 == 1 && context.radius() > 1) ? 2 : 0;
        }

        probMap[originDir.ordinal()] = 0;//Disable the direction we came from
        probMap[signal.dir.ordinal()] += signal.isInTrunk() ? 0 : signal.numTurns == 1 ? 2 : 1;//Favor current travel direction

        return probMap;
    }

    @Override
    public Direction selectNewDirection(GrowthLogicKitConfiguration configuration, DirectionSelectionContext context) {
        final GrowSignal signal = context.signal();
        final Direction newDir = super.selectNewDirection(configuration, context);
        if (signal.isInTrunk() && newDir != Direction.UP && signal.delta.getY() < configuration.getLowestBranchHeight(context) + 3) {
            signal.energy = 2;
        }
        return newDir;
    }

}
