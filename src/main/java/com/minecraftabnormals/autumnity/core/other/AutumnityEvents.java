package com.minecraftabnormals.autumnity.core.other;

import java.util.UUID;

import com.minecraftabnormals.autumnity.common.block.RedstoneJackOLanternBlock;
import com.minecraftabnormals.autumnity.common.entity.passive.SnailEntity;
import com.minecraftabnormals.autumnity.common.entity.passive.TurkeyEntity;
import com.minecraftabnormals.autumnity.core.Reference;
import com.minecraftabnormals.autumnity.core.registry.AutumnityBiomes;
import com.minecraftabnormals.autumnity.core.registry.AutumnityBlocks;
import com.minecraftabnormals.autumnity.core.registry.AutumnityEffects;
import com.minecraftabnormals.autumnity.core.registry.AutumnityEntities;
import com.minecraftabnormals.autumnity.core.registry.AutumnityItems;
import com.mojang.datafixers.util.Pair;
import com.teamabnormals.abnormals_core.core.utils.TradeUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.monster.AbstractSkeletonEntity;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SuspiciousStewItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class AutumnityEvents
{
	private static final AttributeModifier KNOCKBACK_MODIFIER = (new AttributeModifier(UUID.fromString("98D5CD1F-601F-47E6-BEEC-5997E1C4216F"), "Knockback modifier", 1.0D, AttributeModifier.Operation.ADDITION));

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
		if(!event.getWorld().isRemote)
		{
			Entity entity = event.getEntity();

			if(entity instanceof PillagerEntity)
			{
				((CreatureEntity) entity).targetSelector.addGoal(4, new NearestAttackableTargetGoal<>((CreatureEntity)entity, SnailEntity.class, true));
			}
			else if(entity instanceof MooshroomEntity)
			{
				((CreatureEntity) entity).goalSelector.addGoal(4, new AvoidEntityGoal<>((CreatureEntity)entity, SnailEntity.class, 16.0F, 1.25D, 1.6D, EntityPredicates.CAN_AI_TARGET::test));
			}
		}
	}

	@SubscribeEvent
	public static void onLivingSpawn(LivingSpawnEvent.SpecialSpawn event)
	{
		LivingEntity livingentity = event.getEntityLiving();
		IWorld world = event.getWorld();
		Biome biome = world.getBiome(livingentity.getPosition());

		if (livingentity instanceof ZombieEntity || livingentity instanceof AbstractSkeletonEntity)
		{
			if (livingentity.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty())
			{
				if (event.getWorld().getBiome(livingentity.getPosition()) == AutumnityBiomes.PUMPKIN_FIELDS.get() && event.getWorld().getRandom().nextFloat() < 0.05F)
				{
					livingentity.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(Blocks.CARVED_PUMPKIN));
					((MobEntity) livingentity).setDropChance(EquipmentSlotType.HEAD, 0.0F);
				}
			}
		}

		if (biome == AutumnityBiomes.MAPLE_FOREST.get() || biome == AutumnityBiomes.MAPLE_FOREST_HILLS.get() || biome == AutumnityBiomes.PUMPKIN_FIELDS.get())
		{
			if (livingentity instanceof ZombieEntity)
			{
				ZombieEntity zombie = (ZombieEntity) livingentity;
				if (zombie.isChild() && zombie.getRidingEntity() != null && zombie.getRidingEntity().getType() == EntityType.CHICKEN)
				{
					ChickenEntity chicken = (ChickenEntity) zombie.getRidingEntity();
					chicken.remove();
					zombie.stopRiding();
					
					TurkeyEntity turkey = AutumnityEntities.TURKEY.get().create(world.getWorld());
					turkey.setLocationAndAngles(chicken.getPosX(), chicken.getPosY(), chicken.getPosZ(), chicken.rotationYaw, 0.0F);
					turkey.onInitialSpawn(world, world.getDifficultyForLocation(chicken.getPosition()), SpawnReason.JOCKEY, (ILivingEntityData)null, (CompoundNBT)null);
					turkey.setTurkeyJockey(true);
					world.addEntity(turkey);

					zombie.startRiding(turkey, true);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onSnailShellChestplateSneak(LivingUpdateEvent event)
	{
		LivingEntity entity = event.getEntityLiving();

		entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(KNOCKBACK_MODIFIER);
		if(entity.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == AutumnityItems.SNAIL_SHELL_CHESTPLATE.get() && entity.isSneaking())
		{
			entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).applyNonPersistentModifier(KNOCKBACK_MODIFIER);
		}
	}

	@SubscribeEvent
	public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if (ModList.get().isLoaded("berry_good") && event.getItemStack().getItem() == AutumnityItems.FOUL_BERRIES.get())
		{
			event.setUseItem(Event.Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onFoulBerriesEaten(LivingEntityUseItemEvent.Finish event)
	{
		ItemStack itemstack = event.getItem();
		if (event.getEntityLiving().isPotionActive(AutumnityEffects.FOUL_TASTE.get()) && event.getEntityLiving() instanceof PlayerEntity && itemstack.isFood())
		{
			Item item = itemstack.getItem();
			Food food = item.getFood();
			boolean flag = true;

			if (item instanceof SuspiciousStewItem)
			{
				CompoundNBT compoundnbt = itemstack.getTag();
				if (compoundnbt != null && compoundnbt.contains("Effects", 9))
				{
					ListNBT listnbt = compoundnbt.getList("Effects", 10);

					for(int i = 0; i < listnbt.size(); ++i)
					{
						CompoundNBT compoundnbt1 = listnbt.getCompound(i);

						Effect effect = Effect.get(compoundnbt1.getByte("EffectId"));
						if (effect == AutumnityEffects.FOUL_TASTE.get())
						{
							flag = false;
							break;
						}
					}
				}
			}
			else
			{
				for(Pair<EffectInstance, Float> pair : food.getEffects())
				{
					if (pair.getFirst().getPotion() == AutumnityEffects.FOUL_TASTE.get())
					{
						flag = false;
						break;
					}
				}
			}

			if (flag)
			{
				PlayerEntity player = (PlayerEntity) event.getEntityLiving();
				EffectInstance effect = player.getActivePotionEffect(AutumnityEffects.FOUL_TASTE.get());

				int i = food.getHealing();
				int j = i == 1 ? i : (int) (i * 0.5F);

				player.getFoodStats().addStats(j, 0.0F);
				player.removePotionEffect(AutumnityEffects.FOUL_TASTE.get());
				if (effect.getAmplifier() > 0)
				{
					player.addPotionEffect(new EffectInstance(AutumnityEffects.FOUL_TASTE.get(), effect.getDuration(), effect.getAmplifier() - 1));
				}

				if (player instanceof ServerPlayerEntity)
				{
					ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) player;
					if (!event.getEntityLiving().getEntityWorld().isRemote())
					{
						AutumnityCriteriaTriggers.CURE_FOUL_TASTE.trigger((serverplayerentity));
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onWandererTradesEvent(WandererTradesEvent event)
	{
		event.getGenericTrades().add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityBlocks.MAPLE_SAPLING.get().asItem(), 5, 1, 8, 1));
		event.getGenericTrades().add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityBlocks.YELLOW_MAPLE_SAPLING.get().asItem(), 5, 1, 8, 1));
		event.getGenericTrades().add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityBlocks.ORANGE_MAPLE_SAPLING.get().asItem(), 5, 1, 8, 1));
		event.getGenericTrades().add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityBlocks.RED_MAPLE_SAPLING.get().asItem(), 5, 1, 8, 1));
		event.getGenericTrades().add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityBlocks.SNAIL_SLIME.get().asItem(), 4, 1, 5, 1));
	}

	@SubscribeEvent
	public static void onVillagerTradesEvent(VillagerTradesEvent event)
	{
		if (event.getType() == VillagerProfession.FARMER)
		{
			event.getTrades().get(2).add(new TradeUtils.ItemsForEmeraldsTrade(AutumnityItems.FOUL_BERRIES.get(), 2, 16, 12, 10));
		}
	}

	@SubscribeEvent
	public static void onMakeJackOLantern(PlayerInteractEvent.RightClickBlock event)
	{
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == Items.TORCH || itemstack.getItem() == Items.SOUL_TORCH || itemstack.getItem() == Items.REDSTONE_TORCH || itemstack.getItem() == ModCompatibility.ENDER_TORCH)
		{
			World world = event.getWorld();
			BlockPos blockpos = event.getPos();
			BlockState blockstate = event.getWorld().getBlockState(event.getPos());
			PlayerEntity player = event.getPlayer();

			boolean flag = !player.getHeldItemMainhand().doesSneakBypassUse(world, blockpos, player) || !player.getHeldItemOffhand().doesSneakBypassUse(world, blockpos, player);
			boolean flag1 = player.isSecondaryUseActive() && flag;

			if (blockstate.getBlock() == Blocks.CARVED_PUMPKIN && !flag1)
			{
				Direction direction = event.getFace();
				Direction direction1 = blockstate.get(CarvedPumpkinBlock.FACING);

				if (direction == direction1)
				{
					if (!world.isRemote)
					{
						Item item = itemstack.getItem();
						BlockState blockstate1 = item == Items.TORCH ? Blocks.JACK_O_LANTERN.getDefaultState() :
							item == Items.SOUL_TORCH ? AutumnityBlocks.SOUL_JACK_O_LANTERN.get().getDefaultState() :
								item == Items.REDSTONE_TORCH ? AutumnityBlocks.REDSTONE_JACK_O_LANTERN.get().getDefaultState().with(RedstoneJackOLanternBlock.LIT, world.isBlockPowered(blockpos)) :
									AutumnityBlocks.ENDER_JACK_O_LANTERN.get().getDefaultState();
								BlockState blockstate2 = blockstate1.with(CarvedPumpkinBlock.FACING, direction1);
								world.setBlockState(blockpos, blockstate2, 11);

								world.playSound((PlayerEntity)null, blockpos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
								if (!event.getPlayer().abilities.isCreativeMode)
								{
									itemstack.shrink(1);
								}
					}

					player.swingArm(event.getHand());
					event.setCancellationResult(ActionResultType.func_233537_a_(world.isRemote));
					event.setUseItem(Result.DENY);
				}
			}
		}
	}
}