package maxhyper.dtbwg.trees;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.NodeInspector;
import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.api.treedata.TreePart;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SimpleVoxmap;
import maxhyper.dtbwg.cells.DTBWGLeafClusters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class PoplarSpecies extends Species {

    public static final TypedRegistry.EntryType<Species> TYPE = createDefaultType(PoplarSpecies::new);

    public PoplarSpecies(ResourceLocation name, Family family, LeavesProperties leavesProperties) {
        super(name, family, leavesProperties);
    }

    @Override
    public NodeInspector getNodeInflator(SimpleVoxmap leafMap) {
        return new NodeInflatorPoplar(this, leafMap);
    }

    public static class NodeInflatorPoplar implements NodeInspector {

        private float radius;
        private BlockPos last;

        Species species;
        SimpleVoxmap leafMap;

        public NodeInflatorPoplar(Species species, SimpleVoxmap leafMap) {
            this.species = species;
            this.leafMap = leafMap;
            last = BlockPos.ZERO;
        }

        @Override
        public boolean run(BlockState state, LevelAccessor level, BlockPos pos, Direction fromDir) {
            BranchBlock branch = TreeHelper.getBranch(state);

            if (branch != null) {
                radius = species.getFamily().getPrimaryThickness();
            }
            return false;
        }

        @Override
        public boolean returnRun(BlockState state, LevelAccessor level, BlockPos pos, Direction fromDir) {
            // Calculate Branch Thickness based on neighboring branches
            BranchBlock branch = TreeHelper.getBranch(state);

            if (branch != null) {
                float areaAccum = radius * radius; // Start by accumulating the branch we just came from
                boolean isTwig = true;
                boolean isTop = (level.getBlockState(pos.below()).getBlock() == branch);

                for (Direction dir : Direction.values()) {
                    if (!dir.equals(fromDir)) { // Don't count where the signal originated from

                        BlockPos dPos = pos.offset(dir.getNormal());

                        if (dPos.equals(last)) { // or the branch we just came back from
                            isTwig = false; // on the return journey if the block we just came from is a branch we are obviously not the endpoint(twig)
                            isTop = false;
                            continue;
                        }

                        BlockState deltaBlockState = level.getBlockState(dPos);
                        TreePart treepart = TreeHelper.getTreePart(deltaBlockState);
                        if (branch.isSameTree(treepart)) {
                            int branchRadius = treepart.getRadius(deltaBlockState);
                            areaAccum += branchRadius * branchRadius;
                        }
                    }
                }

                if (isTop) {
                    // Handle top leaves here
                    leafMap.setVoxel(pos, (byte) 16); // 16(bit 5) is code for a twig
                    SimpleVoxmap leafCluster = DTBWGLeafClusters.POPLAR_TOP;
                    leafMap.blitMax(pos, leafCluster);
                } else if (isTwig) {
                    // Handle branch leaves here
                    leafMap.setVoxel(pos, (byte) 16); // 16(bit 5) is code for a twig
                    SimpleVoxmap leafCluster = species.getLeavesProperties().getCellKit().getLeafCluster();
                    leafMap.blitMax(pos, leafCluster);
                } else {
                    // The new branch should be the square root of all of the sums of the areas of the branches coming into it.
                    radius = (float) Math.sqrt(areaAccum) + (species.getTapering() * species.getWorldGenTaperingFactor());

                    //Ensure the branch is never inflated past it's species maximum
                    int maxRadius = species.getMaxBranchRadius();
                    if (radius > maxRadius) {
                        radius = maxRadius;
                    }

                    // Make sure that non-twig branches are at least radius 2
                    float secondaryThickness = species.getFamily().getSecondaryThickness();
                    if (radius < secondaryThickness) {
                        radius = secondaryThickness;
                    }

                    branch.setRadius(level, pos, (int) Math.floor(radius), null);
                    leafMap.setVoxel(pos, (byte) 32); // 32(bit 6) is code for a branch
                    if (Math.floor(radius) < 3) {
                        SimpleVoxmap leafCluster = species.getLeavesProperties().getCellKit().getLeafCluster();
                        leafMap.blitMax(pos, leafCluster);
                    }
                }
                last = pos;
            }
            return false;
        }

    }
}