package com.minerarcana.runecarved;

import java.util.List;
import java.util.Random;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.*;

public class ContainerSimpleEnchanter extends Container {
	/** SlotEnchantmentTable object with ItemStack to be enchanted */
	public ItemStackHandler tableInventory;
	/** current world (for bookshelf counting) */
	private final World worldPointer;
	private final BlockPos position;
	private final Random rand;
	public int xpSeed;
	/** 3-member array storing the enchantment levels of each slot */
	public int[] enchantLevels;
	public int[] enchantClue;
	public int[] worldClue;

	public ContainerSimpleEnchanter(InventoryPlayer playerInv, World worldIn, BlockPos pos,
			TileEntitySimpleEnchanter tile) {
		this.tableInventory = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
				.cast(tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null));
		this.rand = new Random();
		this.enchantLevels = new int[3];
		this.enchantClue = new int[] {-1, -1, -1};
		this.worldClue = new int[] {-1, -1, -1};
		this.worldPointer = worldIn;
		this.position = pos;
		this.xpSeed = playerInv.player.getXPSeed();
		this.addSlotToContainer(new SlotItemHandler(this.tableInventory, 0, 15, 47) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return true;
			}

			@Override
			public int getSlotStackLimit() {
				return 1;
			}
		});

		for(int i = 0; i < 3; ++i) {
			for(int j = 0; j < 9; ++j) {
				this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for(int k = 0; k < 9; ++k) {
			this.addSlotToContainer(new Slot(playerInv, k, 8 + k * 18, 142));
		}
	}

	protected void broadcastData(IContainerListener crafting) {
		crafting.sendWindowProperty(this, 0, this.enchantLevels[0]);
		crafting.sendWindowProperty(this, 1, this.enchantLevels[1]);
		crafting.sendWindowProperty(this, 2, this.enchantLevels[2]);
		crafting.sendWindowProperty(this, 3, this.xpSeed & -16);
		crafting.sendWindowProperty(this, 4, this.enchantClue[0]);
		crafting.sendWindowProperty(this, 5, this.enchantClue[1]);
		crafting.sendWindowProperty(this, 6, this.enchantClue[2]);
		crafting.sendWindowProperty(this, 7, this.worldClue[0]);
		crafting.sendWindowProperty(this, 8, this.worldClue[1]);
		crafting.sendWindowProperty(this, 9, this.worldClue[2]);
	}

	@Override
	public void addListener(IContainerListener listener) {
		super.addListener(listener);
		this.broadcastData(listener);
	}

	/**
	 * Looks for changes made in the container, sends them to every listener.
	 */
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

		for(int i = 0; i < this.listeners.size(); ++i) {
			IContainerListener icontainerlistener = this.listeners.get(i);
			this.broadcastData(icontainerlistener);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int id, int data) {
		if(id >= 0 && id <= 2) {
			this.enchantLevels[id] = data;
		}
		else if(id == 3) {
			this.xpSeed = data;
		}
		else if(id >= 4 && id <= 6) {
			this.enchantClue[id - 4] = data;
		}
		else if(id >= 7 && id <= 9) {
			this.worldClue[id - 7] = data;
		}
		else {
			super.updateProgressBar(id, data);
		}
	}

	/**
	 * Callback for when the crafting matrix is changed.
	 */
	public void onCraftMatrixChanged(ItemStackHandler tableInventory2) {
		ItemStack itemstack = tableInventory2.getStackInSlot(0);

		if(!itemstack.isEmpty() && itemstack.isItemEnchantable()) {
			if(!this.worldPointer.isRemote) {
				float power = 3;

				this.rand.setSeed(this.xpSeed);

				for(int i1 = 0; i1 < 3; ++i1) {
					this.enchantLevels[i1] =
							EnchantmentHelper.calcItemStackEnchantability(this.rand, i1, (int) power, itemstack);
					this.enchantClue[i1] = -1;
					this.worldClue[i1] = -1;

					if(this.enchantLevels[i1] < i1 + 1) {
						this.enchantLevels[i1] = 0;
					}
					this.enchantLevels[i1] = net.minecraftforge.event.ForgeEventFactory.onEnchantmentLevelSet(
							worldPointer, position, i1, (int) power, itemstack, enchantLevels[i1]);
				}

				for(int j1 = 0; j1 < 3; ++j1) {
					if(this.enchantLevels[j1] > 0) {
						List<EnchantmentData> list = this.getEnchantmentList(itemstack, j1, this.enchantLevels[j1]);

						if(list != null && !list.isEmpty()) {
							EnchantmentData enchantmentdata = list.get(this.rand.nextInt(list.size()));
							this.enchantClue[j1] = Enchantment.getEnchantmentID(enchantmentdata.enchantment);
							this.worldClue[j1] = enchantmentdata.enchantmentLevel;
						}
					}
				}

				this.detectAndSendChanges();
			}
		}
		else {
			for(int i = 0; i < 3; ++i) {
				this.enchantLevels[i] = 0;
				this.enchantClue[i] = -1;
				this.worldClue[i] = -1;
			}
		}
	}

	/**
	 * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
	 */
	@Override
	public boolean enchantItem(EntityPlayer playerIn, int id) {
		ItemStack itemstack = this.tableInventory.getStackInSlot(0);
		ItemStack itemstack1 = this.tableInventory.getStackInSlot(1);
		int i = id + 1;

		if((itemstack1.isEmpty() || itemstack1.getCount() < i) && !playerIn.capabilities.isCreativeMode) {
			return false;
		}
		else if(this.enchantLevels[id] > 0 && !itemstack.isEmpty()
				&& (playerIn.experienceLevel >= i && playerIn.experienceLevel >= this.enchantLevels[id]
						|| playerIn.capabilities.isCreativeMode)) {
			if(!this.worldPointer.isRemote) {
				List<EnchantmentData> list = this.getEnchantmentList(itemstack, id, this.enchantLevels[id]);

				if(!list.isEmpty()) {
					playerIn.onEnchant(itemstack, i);
					boolean flag = itemstack.getItem() == Items.BOOK;
					boolean flag2 = itemstack.getItem() == Items.PAPER;

					if(flag) {
						itemstack = new ItemStack(Items.ENCHANTED_BOOK);
						this.tableInventory.setStackInSlot(0, itemstack);
					}
					if(flag2) {
						itemstack = new ItemStack(Items.GOLD_INGOT); // TODO
						this.tableInventory.setStackInSlot(0, itemstack);
					}

					for(int j = 0; j < list.size(); ++j) {
						EnchantmentData enchantmentdata = list.get(j);

						if(flag) {
							ItemEnchantedBook.addEnchantment(itemstack, enchantmentdata);
						}
						else {
							itemstack.addEnchantment(enchantmentdata.enchantment, enchantmentdata.enchantmentLevel);
						}
					}

					playerIn.addStat(StatList.ITEM_ENCHANTED);

					if(playerIn instanceof EntityPlayerMP) {
						CriteriaTriggers.ENCHANTED_ITEM.trigger((EntityPlayerMP) playerIn, itemstack, i);
					}

					this.xpSeed = playerIn.getXPSeed();
					this.onCraftMatrixChanged(this.tableInventory);
					this.worldPointer.playSound((EntityPlayer) null, this.position,
							SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F,
							this.worldPointer.rand.nextFloat() * 0.1F + 0.9F);
				}
			}

			return true;
		}
		else {
			return false;
		}
	}

	private List<EnchantmentData> getEnchantmentList(ItemStack stack, int enchantSlot, int level) {
		this.rand.setSeed(this.xpSeed + enchantSlot);
		List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(this.rand, stack, level, false);

		if(stack.getItem() == Items.BOOK && list.size() > 1) {
			list.remove(this.rand.nextInt(list.size()));
		}

		return list;
	}

	/**
	 * Called when the container is closed.
	 */
	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);

		if(!this.worldPointer.isRemote) {
			this.clearContainer(playerIn, playerIn.world, this.tableInventory);
		}
	}

	private void clearContainer(EntityPlayer playerIn, World world, ItemStackHandler tableInventory2) {
		if(!playerIn.isEntityAlive()
				|| playerIn instanceof EntityPlayerMP && ((EntityPlayerMP) playerIn).hasDisconnected()) {
			for(int j = 0; j < tableInventory2.getSlots(); ++j) {
				playerIn.dropItem(tableInventory2.extractItem(j, 64, false), false);
			}
		}
		else {
			for(int i = 0; i < tableInventory2.getSlots(); ++i) {
				playerIn.inventory.placeItemBackInInventory(world, tableInventory2.extractItem(i, 64, false));
			}
		}
	}

	/**
	 * Determines whether supplied player can use this container
	 */
	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {

		return playerIn.getDistanceSq(this.position.getX() + 0.5D, this.position.getY() + 0.5D,
				this.position.getZ() + 0.5D) <= 64.0D;
	}

	/**
	 * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
	 * inventory and the other inventory(s).
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			if(index == 0) {
				if(!this.mergeItemStack(itemstack1, 2, 38, true)) {
					return ItemStack.EMPTY;
				}
			}
			else if(index == 1) {
				if(!this.mergeItemStack(itemstack1, 2, 38, true)) {
					return ItemStack.EMPTY;
				}
			}
			else if(itemstack1.getItem() == Items.DYE
					&& EnumDyeColor.byDyeDamage(itemstack1.getMetadata()) == EnumDyeColor.BLUE) {
				if(!this.mergeItemStack(itemstack1, 1, 2, true)) {
					return ItemStack.EMPTY;
				}
			}
			else {
				if(this.inventorySlots.get(0).getHasStack() || !this.inventorySlots.get(0).isItemValid(itemstack1)) {
					return ItemStack.EMPTY;
				}

				if(itemstack1.hasTagCompound())// Forge: Fix MC-17431
				{
					this.inventorySlots.get(0).putStack(itemstack1.splitStack(1));
				}
				else if(!itemstack1.isEmpty()) {
					this.inventorySlots.get(0)
							.putStack(new ItemStack(itemstack1.getItem(), 1, itemstack1.getMetadata()));
					itemstack1.shrink(1);
				}
			}

			if(itemstack1.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			}
			else {
				slot.onSlotChanged();
			}

			if(itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(playerIn, itemstack1);
		}

		return itemstack;
	}
}