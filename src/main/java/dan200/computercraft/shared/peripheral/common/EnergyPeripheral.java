package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.util.EnergyUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.client.FMLClientHandler;

public class EnergyPeripheral implements IPeripheral
{
	
	private final TileEntity tile;
	private final EnumFacing side;
	
	private String[] methods;
	
	private int previousEnergyStored = 0;
	private int previousTickCount = 0;
	
	public EnergyPeripheral(TileEntity tile, EnumFacing side)
	{
		this.tile = tile;
		this.side = side;
	}

	@Override
	public String getType()
	{
		String name = "energy";
		final String blockName = tile.getBlockType().getUnlocalizedName();
		
		if (blockName.contains("."))
		{
			final String[] splitName = blockName.split("\\.");
			name += "_" + splitName[splitName.length - 1];
		}
		return name;
	}

	@Override
	public String[] getMethodNames()
	{
		this.methods = EnergyUtils.getPossibleMethods(tile, side.getOpposite());
		
		return this.methods;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException
	{
		Object[] returnValues = new Object[] {};
		
		switch( this.methods[method] )
        {
            case "getEnergyStored":
            {
            	synchronized( this )
                {
            		returnValues =  new Object[] {EnergyUtils.getEnergyStored(tile, side.getOpposite())};
            		break;
                }
            }
            case "getMaxEnergyStored":
            {
            	synchronized( this )
                {
            		returnValues =  new Object[] {EnergyUtils.getMaxEnergyStored(tile, side.getOpposite())};
            		break;
                }
            }
            case "getEnergyStoredPercentage":
            {
            	synchronized( this )
                {
            		final double max = EnergyUtils.getMaxEnergyStored(tile, side.getOpposite());
            		final double stored = EnergyUtils.getEnergyStored(tile, side.getOpposite());
            		if  ( max <= 0 ) {
            			return new Object[] {"NaN"};
            		}
            		returnValues =  new Object[] {stored / max * 100};
            		break;
                }
            }
            case "getEnergyPerTick":
            {
            	synchronized( this )
                {
            		final int tickCount = FMLClientHandler.instance().getClient().player.ticksExisted - this.previousTickCount;
            		final int energyDiff = EnergyUtils.getEnergyStored(tile, side.getOpposite()) - this.previousEnergyStored;
            		if  ( tickCount == 0 ) {
            			return new Object[] {"NaN"};
            		}
            		returnValues =  new Object[] {Math.floor(energyDiff / tickCount)};
            		break;
                }
            }
            case "getMaxEnergyExtract":
            {
            	synchronized( this )
                {
            		returnValues =  new Object[] {EnergyUtils.getMaxEnergyExtract(tile, side.getOpposite())};
            		break;
                }
            }
            case "getMaxEnergyReceive":
            {
            	synchronized( this )
                {
            		returnValues =  new Object[] {EnergyUtils.getMaxEnergyReceive(tile, side.getOpposite())};
            		break;
                }
            }
        }

		this.previousTickCount = FMLClientHandler.instance().getClient().player.ticksExisted;
		this.previousEnergyStored = EnergyUtils.getEnergyStored(tile, side.getOpposite());

		return returnValues;
	}

	@Override
	public boolean equals(IPeripheral other) {
		return false;
	}
}