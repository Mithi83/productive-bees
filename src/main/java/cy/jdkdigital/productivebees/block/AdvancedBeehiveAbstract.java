package cy.jdkdigital.productivebees.block;

import cy.jdkdigital.productivebees.tileentity.AdvancedBeehiveTileEntityAbstract;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.*;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public abstract class AdvancedBeehiveAbstract extends ContainerBlock {

	public static final Direction[] DIRECTIONS;
	public static final IntegerProperty HONEY_LEVEL;

	protected static int MAX_HONEY_LEVEL = 5;

	public AdvancedBeehiveAbstract(Properties properties) {
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState().with(HONEY_LEVEL, 0).with(BlockStateProperties.FACING, Direction.NORTH));
	}

	public int getMaxHoneyLevel() {
		return MAX_HONEY_LEVEL;
	}

	public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}

	public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
		return state.get(HONEY_LEVEL);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);

		CompoundNBT entityNBT = stack.getChildTag("BlockEntityTag");
		CompoundNBT stateNBT = stack.getChildTag("BlockStateTag");
		if (stateNBT != null) {
			if (stateNBT.contains("honey_level")) {
				String honeyLevel = stateNBT.getString("honey_level");
				tooltip.add(new TranslationTextComponent("productivebees.hive.tooltip.honey_level", honeyLevel).applyTextStyle(TextFormatting.GOLD));
			}
		}
		if (entityNBT != null) {
			if (entityNBT.contains("Bees")) {
				ListNBT beeList = entityNBT.getCompound("Bees").getList("Inhabitants", Constants.NBT.TAG_COMPOUND);
				tooltip.add(new TranslationTextComponent("productivebees.hive.tooltip.bees").applyTextStyle(TextFormatting.BOLD));
				for (int i = 0; i < beeList.size(); ++i) {
					CompoundNBT tag = beeList.getCompound(i);
					CompoundNBT beeNBT = tag.getCompound("EntityData");
					tooltip.add(new TranslationTextComponent("" + beeNBT.getString("id")).applyTextStyle(TextFormatting.GREEN));
				}
			}
		}
	}

	public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity tileEntity, ItemStack itemStack) {
		super.harvestBlock(world, player, pos, state, tileEntity, itemStack);
		if (!world.isRemote && tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
			AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
			if (EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
				beehiveTileEntity.releaseBees(player, state, BeehiveTileEntity.State.EMERGENCY);
				world.updateComparatorOutputLevel(pos, this);
				this.activateBeeFrenzy(world, pos);
			}

			CriteriaTriggers.BEE_NEST_DESTROYED.test((ServerPlayerEntity)player, state.getBlock(), itemStack, beehiveTileEntity.getBeeCount());
		}
	}

	private void activateBeeFrenzy(World world, BlockPos pos) {
		List<BeeEntity> beeList = world.getEntitiesWithinAABB(BeeEntity.class, (new AxisAlignedBB(pos)).grow(8.0D, 6.0D, 8.0D));
		if (!beeList.isEmpty()) {
			List<PlayerEntity> playerList = world.getEntitiesWithinAABB(PlayerEntity.class, (new AxisAlignedBB(pos)).grow(8.0D, 6.0D, 8.0D));
			int playerCount = playerList.size();

			for (BeeEntity bee : beeList) {
				if (bee.getAttackTarget() == null) {
					bee.setBeeAttacker(playerList.get(world.rand.nextInt(playerCount)));
				}
			}
		}
	}

	public static void spawnHoneycomb(World world, BlockPos pos) {
		spawnAsEntity(world, pos, new ItemStack(Items.HONEYCOMB, 3));
	}

	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult blockRayTraceResult) {
		ItemStack heldItem = player.getHeldItem(hand);
		ItemStack heldItemCopy = heldItem.copy();
		int honeyLevel = state.get(HONEY_LEVEL);
		boolean itemUsed = false;
		if (honeyLevel >= getMaxHoneyLevel()) {
			if (heldItem.getItem() == Items.SHEARS) {
				world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.BLOCK_BEEHIVE_SHEAR, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				spawnHoneycomb(world, pos);
				heldItem.damageItem(1, player, (entity) -> {
					entity.sendBreakAnimation(hand);
				});
				itemUsed = true;
			} else if (heldItem.getItem() == Items.GLASS_BOTTLE) {
				heldItem.shrink(1);
				world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				if (heldItem.isEmpty()) {
					player.setHeldItem(hand, new ItemStack(Items.HONEY_BOTTLE));
				} else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.HONEY_BOTTLE))) {
					player.dropItem(new ItemStack(Items.HONEY_BOTTLE), false);
				}

				itemUsed = true;
			}
		}

		if (itemUsed) {
			if (!CampfireBlock.isLitCampfireInRange(world, pos, 5)) {
				if (this.tileEntityAtPositionHasBees(world, pos)) {
					this.activateBeeFrenzy(world, pos);
				}

				this.takeHoney(world, state, pos, player, BeehiveTileEntity.State.EMERGENCY);
			} else {
				this.takeHoney(world, state, pos);
				if (player instanceof ServerPlayerEntity) {
					CriteriaTriggers.SAFELY_HARVEST_HONEY.test((ServerPlayerEntity)player, pos, heldItemCopy);
				}
			}

			return ActionResultType.SUCCESS;
		} else {
			return super.onBlockActivated(state, world, pos, player, hand, blockRayTraceResult);
		}
	}

	private boolean tileEntityAtPositionHasBees(World world, BlockPos pos) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
			AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
			return !beehiveTileEntity.isHiveEmpty();
		} else {
			return false;
		}
	}

	public void takeHoney(World world, BlockState state, BlockPos pos, @Nullable PlayerEntity player, BeehiveTileEntity.State beeState) {
		this.takeHoney(world, state, pos);
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
			AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
			beehiveTileEntity.releaseBees(player, state, beeState);
		}
	}

	public void takeHoney(World world, BlockState state, BlockPos pos) {
		world.setBlockState(pos, state.with(HONEY_LEVEL, getMaxHoneyLevel() - 5), 3);
	}

	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(HONEY_LEVEL) >= MAX_HONEY_LEVEL) {
			for(int i = 0; i < random.nextInt(1) + 1; ++i) {
				this.func_226879_a_(world, pos, state);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void func_226879_a_(World world, BlockPos pos, BlockState state) {
		if (state.getFluidState().isEmpty() && world.rand.nextFloat() >= 0.3F) {
			VoxelShape shape = state.getCollisionShape(world, pos);
			double shapeEnd = shape.getEnd(Direction.Axis.Y);
			if (shapeEnd >= 1.0D && !state.isIn(BlockTags.IMPERMEABLE)) {
				double shapeStart = shape.getStart(Direction.Axis.Y);
				if (shapeStart > 0.0D) {
					this.addHoneyParticle(world, pos, shape, (double)pos.getY() + shapeStart - 0.05D);
				} else {
					BlockPos posDown = pos.down();
					BlockState stateDown = world.getBlockState(posDown);
					VoxelShape shapeDown = stateDown.getCollisionShape(world, posDown);
					double shapeDownEnd = shapeDown.getEnd(Direction.Axis.Y);
					if ((shapeDownEnd < 1.0D || !stateDown.isCollisionShapeOpaque(world, posDown)) && stateDown.getFluidState().isEmpty()) {
						this.addHoneyParticle(world, pos, shape, (double)pos.getY() - 0.05D);
					}
				}
			}

		}
	}

	@OnlyIn(Dist.CLIENT)
	private void addHoneyParticle(World world, BlockPos pos, VoxelShape shape, double p_226880_4_) {
		this.addHoneyParticle(world, (double)pos.getX() + shape.getStart(Direction.Axis.X), (double)pos.getX() + shape.getEnd(Direction.Axis.X), (double)pos.getZ() + shape.getStart(Direction.Axis.Z), (double)pos.getZ() + shape.getEnd(Direction.Axis.Z), p_226880_4_);
	}

	@OnlyIn(Dist.CLIENT)
	private void addHoneyParticle(World world, double d1, double d2, double d3, double d4, double d5) {
		world.addParticle(ParticleTypes.DRIPPING_HONEY, MathHelper.lerp(world.rand.nextDouble(), d1, d2), d5, MathHelper.lerp(world.rand.nextDouble(), d3, d4), 0.0D, 0.0D, 0.0D);
	}

	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return this.getDefaultState().with(BlockStateProperties.FACING, context.getPlacementHorizontalFacing().getOpposite());
	}

	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(HONEY_LEVEL, BlockStateProperties.FACING);
	}

	@Nonnull
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	public void onBlockHarvested(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isRemote && player.isCreative() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
				AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
				int honeyLevel = state.get(HONEY_LEVEL);
				boolean hasBees = !beehiveTileEntity.isHiveEmpty();
				if (!hasBees && honeyLevel == 0) {
					return;
				}

				ItemStack itemStack = new ItemStack(this);
				CompoundNBT compoundNBT = new CompoundNBT();
				if (hasBees) {
					compoundNBT.put("Bees", beehiveTileEntity.getBeeListAsNBTList());
				}

				compoundNBT.putInt("honey_level", honeyLevel);
				itemStack.setTagInfo("BlockStateTag", compoundNBT);
				ItemEntity hiveEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);
				hiveEntity.setDefaultPickupDelay();
				world.addEntity(hiveEntity);
			}
		}
		super.onBlockHarvested(world, pos, state, player);
	}

	public List<ItemStack> getDrops(BlockState state, net.minecraft.world.storage.loot.LootContext.Builder builder) {
		Entity entity = builder.get(LootParameters.THIS_ENTITY);
		if (entity instanceof TNTEntity || entity instanceof CreeperEntity || entity instanceof WitherSkullEntity || entity instanceof WitherEntity || entity instanceof TNTMinecartEntity) {
			TileEntity tileEntity = builder.get(LootParameters.BLOCK_ENTITY);
			if (tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
				AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
				beehiveTileEntity.releaseBees(null, state, BeehiveTileEntity.State.EMERGENCY);
			}
		}

		return super.getDrops(state, builder);
	}

	public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState state1, IWorld world, BlockPos pos, BlockPos fireBlockPos) {
		if (world.getBlockState(fireBlockPos).getBlock() instanceof FireBlock) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity instanceof AdvancedBeehiveTileEntityAbstract) {
				AdvancedBeehiveTileEntityAbstract beehiveTileEntity = (AdvancedBeehiveTileEntityAbstract)tileEntity;
				beehiveTileEntity.releaseBees(null, state, BeehiveTileEntity.State.EMERGENCY);
			}
		}

		return super.updatePostPlacement(state, direction, state1, world, pos, fireBlockPos);
	}

	static {
		DIRECTIONS = new Direction[]{Direction.WEST, Direction.EAST, Direction.SOUTH};
		HONEY_LEVEL = BlockStateProperties.HONEY_LEVEL;
	}
}
