package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.IWiredElement;
import net.minecraft.util.EnumFacing;

public interface IWiredElementTile
{
    IWiredElement getWiredElement( EnumFacing side );
}
