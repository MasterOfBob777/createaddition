package com.mrh0.createaddition.blocks.tesla_coil;

import java.util.List;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.config.Config;
import com.mrh0.createaddition.energy.BaseElectricTileEntity;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CAEffects;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.item.ChargingChromaticCompound;
import com.mrh0.createaddition.item.Multimeter;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.relays.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.block.depot.DepotBehaviour;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.BeltProcessingBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour.TransportedResult;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class TeslaCoilTileEntity extends BaseElectricTileEntity implements IHaveGoggleInformation {

	private static final int 
			MAX_IN = Config.TESLA_COIL_MAX_INPUT.get(), 
			CHARGE_RATE = Config.TESLA_COIL_CHARGE_RATE.get(),
			CAPACITY = Math.max(Config.TESLA_COIL_CAPACITY.get(), CHARGE_RATE), 
			HURT_ENERGY_REQUIRED = Config.TESLA_COIL_HURT_ENERGY_REQUIRED.get(), 
			HURT_DMG_MOB = Config.TESLA_COIL_HURT_DMG_MOB.get(),
			HURT_DMG_PLAYER = Config.TESLA_COIL_HURT_DMG_PLAYER.get(),
			HURT_RANGE = Config.TESLA_COIL_HURT_RANGE.get(), 
			HURT_EFFECT_TIME_MOB = Config.TESLA_COIL_HURT_EFFECT_TIME_MOB.get(),
			HURT_EFFECT_TIME_PLAYER = Config.TESLA_COIL_HURT_EFFECT_TIME_PLAYER.get(),
			HURT_FIRE_COOLDOWN = Config.TESLA_COIL_HURT_FIRE_COOLDOWN.get();
	
	protected ItemStack chargedStackCache;
	protected int poweredTimer = 0;
	
	private static DamageSource dmgSource = new DamageSource("tesla_coil");
	
	public TeslaCoilTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn, CAPACITY, MAX_IN, 0);
	}
	
	public BeltProcessingBehaviour processingBehaviour;

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		processingBehaviour =
			new BeltProcessingBehaviour(this).whenItemEnters((s, i) -> TeslaCoilBeltCallbacks.onItemReceived(s, i, this))
				.whileItemHeld((s, i) -> TeslaCoilBeltCallbacks.whenItemHeld(s, i, this));
		behaviours.add(processingBehaviour);
	}

	@Override
	public boolean isEnergyInput(Direction side) {
		return side != getBlockState().getValue(TeslaCoil.FACING);
	}

	@Override
	public boolean isEnergyOutput(Direction side) {
		return false;
	}
	
	/*protected boolean canStackReceiveCharge(ItemStack stack) {
		if(stack == null)
			return false;
		if(!stack.getCapability(CapabilityEnergy.ENERGY).isPresent())
			return false;
		IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY).orElse(null);
		if(es.receiveEnergy(1, true) != 1)
			return false;
		return true;
	}*/
	
	public int getConsumption() {
		return CHARGE_RATE;
	}
	
	/*@Override
	public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
		tooltip.add(new StringTextComponent(spacing)
				.append(new TranslationTextComponent("block.createaddition.tesla_coil.info").withStyle(TextFormatting.WHITE)));
		
		
		tooltip.add(new StringTextComponent(spacing).append(new TranslationTextComponent(CreateAddition.MODID + ".tooltip.energy.consumption").withStyle(TextFormatting.GRAY)));
		tooltip.add(new StringTextComponent(spacing).append(new StringTextComponent(" " + Multimeter.format(hasEnoughEnergy() ? getConsumption() : 0) + "fe/t ")).withStyle(TextFormatting.AQUA));
		return true;
	}*/
	
	protected float getItemCharge(IEnergyStorage energy) {
		if (energy == null)
			return 0f;
		return (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
	}
	
	public float getCharge(ItemStack itemStack) {
		if (chargedStackCache != null)
			return 0f;
		if (itemStack.getCapability(CapabilityEnergy.ENERGY).isPresent())
			return getItemCharge(itemStack.getCapability(CapabilityEnergy.ENERGY).orElse(null));
		if (itemStack.getItem() == CAItems.CHARGING_CHROMATIC_COMPOUND.get())
			return (float) ChargingChromaticCompound.getCharge(itemStack) * 90f;
		if (itemStack.getItem() == CAItems.OVERCHARGED_ALLOY.get())
			return 90f;
		return 0f;
	}
	
	public String getChargeString() {
		float c = Math.round(getCharge(chargedStackCache) * 100);
		if(c >= 9000)
			return "OVER9000% ";
		return Math.round(getCharge(chargedStackCache) * 100) + "% ";
	}
	
	/*@Override
	public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
		tooltip.add(new StringTextComponent(spacing).append(
				new TranslationTextComponent("block.createaddition.charger.info").withStyle(TextFormatting.WHITE)));
		if (chargedStackCache != null) {
			tooltip.add(new StringTextComponent(spacing).append(" ")
					.append(new StringTextComponent(getChargeString()).withStyle(TextFormatting.AQUA))
					.append(new TranslationTextComponent(CreateAddition.MODID + ".tooltip.energy.charged")
							.withStyle(TextFormatting.GRAY)));
		} else {
			tooltip.add(new StringTextComponent(spacing).append(" ").append(
					new TranslationTextComponent("block.createaddition.charger.empty").withStyle(TextFormatting.GRAY)));
		}

		return true;
	}*/
	
	protected ProcessingResult onCharge(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		ProcessingResult res = chargeCompundAndStack(transported, handler);
		/*if(res == ProcessingResult.HOLD)
			if(getWorld().getRandom().nextInt(20)>18) {
				//AxisAlignedBB bounds = new AxisAlignedBB(getBlockPos().subtract(new BlockPos(5,5,5)), getBlockPos().offset(new BlockPos(5,5,5)));
				List<PlayerEntity> players = getWorld().getEntitiesOfClass(ServerPlayerEntity.class, new AxisAlignedBB(getBlockPos()).inflate(5));//getWorld().getNearbyPlayers(EntityPredicate.DEFAULT, null, bounds);
				for(PlayerEntity p : players) {
					getWorld().playSound(p, getBlockPos(), SoundEvents.SPIDER_AMBIENT, SoundCategory.BLOCKS, 1f, 1f);
					System.out.println("Sound");
				}
			}*/
		return res;
	}
	
	private void doDmg() {
		energy.internalConsumeEnergy(HURT_ENERGY_REQUIRED);
		BlockPos origin = getBlockPos().relative(getBlockState().getValue(TeslaCoil.FACING));
		List<LivingEntity> ents = getWorld().getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(origin).inflate(HURT_RANGE));
		for(LivingEntity e : ents) {
			int dmg = HURT_DMG_MOB;
			int time = HURT_EFFECT_TIME_MOB;
			if(e instanceof PlayerEntity) {
				dmg = HURT_DMG_PLAYER;
				time = HURT_EFFECT_TIME_PLAYER;
			}
			if(dmg > 0)
				e.hurt(dmgSource, dmg);
			if(time > 0)
				e.addEffect(new EffectInstance(CAEffects.SHOCKING, time));
		}
	}
	
	int dmgTick = 0;
	
	@Override
	public void tick() {
		super.tick();
		int signal = getWorld().getBestNeighborSignal(getBlockPos());
		if(signal > 0 && energy.getEnergyStored() >= HURT_ENERGY_REQUIRED)
			poweredTimer = 10;
		
		dmgTick++;
		if((dmgTick%=HURT_FIRE_COOLDOWN) == 0 && energy.getEnergyStored() >= HURT_ENERGY_REQUIRED && signal > 0)
			doDmg();
		
		if(level.isClientSide())
			return;
		if(poweredTimer > 0) {
			if(!shouldPower(signal))
				CABlocks.TESLA_COIL.get().setPowered(level, getBlockPos(), true);
			poweredTimer--;
		}
		else
			if(shouldPower(signal))
				CABlocks.TESLA_COIL.get().setPowered(level, getBlockPos(), false);
	}
	
	public boolean shouldPower(int signal) {
		return getBlockState().getValue(TeslaCoil.POWERED);
	}
	
	protected ProcessingResult chargeCompundAndStack(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		
		ItemStack stack = transported.stack;
		if(stack == null)
			return ProcessingResult.PASS;
		
		if(chargeStack(stack, transported, handler)) {
			if(energy.getEnergyStored() >= stack.getCount())
				poweredTimer = 10;
			return ProcessingResult.HOLD;
		}
		if (stack.getItem() == CAItems.CHARGING_CHROMATIC_COMPOUND.get()) {
			if(energy.getEnergyStored() >= stack.getCount())
				poweredTimer = 10;
			
			int energyPush = Math.min(energy.getEnergyStored(), CHARGE_RATE)/stack.getCount();
			int energyRemoved = ChargingChromaticCompound.charge(stack, energyPush);
			energy.internalConsumeEnergy(energyRemoved*stack.getCount());

			if (ChargingChromaticCompound.getEnergy(stack) >= ChargingChromaticCompound.MAX_CHARGE) {
				TransportedItemStack res = new TransportedItemStack(new ItemStack(CAItems.OVERCHARGED_ALLOY.get(), stack.getCount()));
				handler.handleProcessingOnItem(transported, TransportedResult.convertTo(res));
			}
			return ProcessingResult.HOLD;
		}
		return ProcessingResult.PASS;
	}
	
	protected boolean chargeStack(ItemStack stack, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		if(stack.getItem() == AllItems.CHROMATIC_COMPOUND.get()) {
			TransportedItemStack res = new TransportedItemStack(new ItemStack(CAItems.CHARGING_CHROMATIC_COMPOUND.get(), stack.getCount()));
			handler.handleProcessingOnItem(transported, TransportedResult.convertTo(res));
			
			//handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(collect, left));
		}
		if(!stack.getCapability(CapabilityEnergy.ENERGY).isPresent())
			return false;
		IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY).orElse(null);
		energy.extractEnergy(es.receiveEnergy(energy.extractEnergy(getConsumption(), true), false), false);
		if(es.receiveEnergy(1, true) != 1)
			return false;
		return true;
	}
	
	/*@Override
	public void tick() {
		super.tick();
		
		DepotBehaviour depot = TileEntityBehaviour.get(level, getBlockPos().below(2), DepotBehaviour.TYPE);
		if(depot == null) {
			chargedStackCache = null;
			return;
		}
		chargedStackCache = depot.getHeldItemStack();
	}*/
}
