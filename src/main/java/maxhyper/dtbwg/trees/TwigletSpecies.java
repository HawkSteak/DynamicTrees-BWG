package maxhyper.dtbwg.trees;

import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.systems.nodemapper.NetVolumeNode;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.resources.ResourceLocation;

public class TwigletSpecies extends Species {

    public static final TypedRegistry.EntryType<Species> TYPE = createDefaultType(TwigletSpecies::new);

    public TwigletSpecies(ResourceLocation name, Family family, LeavesProperties leavesProperties) {
        super(name, family, leavesProperties);
        setBasicGrowingParameters(0.3f, 2.5f, 1, 2, 1.0f);
    }

    @Override
    public void processVolume(NetVolumeNode.Volume volume) {
        volume.addVolume(NetVolumeNode.Volume.VOXELSPERLOG);
        super.processVolume(volume);
    }

    @Override
    public LogsAndSticks getLogsAndSticks(NetVolumeNode.Volume volume) {
        NetVolumeNode.Volume modifiedVolume = new NetVolumeNode.Volume(volume.getRawVolumesArray());
        modifiedVolume.addVolume(NetVolumeNode.Volume.VOXELSPERLOG);
        return super.getLogsAndSticks(modifiedVolume);
    }
}