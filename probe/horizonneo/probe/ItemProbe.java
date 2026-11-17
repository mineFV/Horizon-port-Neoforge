package horizonneo.probe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Items and fluids, which need the game started: asked in a server only (InServer, GameProbe's "server" argument).
 * Hashes start from objects' identities, which change from run to run: only whether two are equal is asked.
 */
final class ItemProbe {
    private ItemProbe() {}

    static void run() {
        p("items", () -> Items.AIR + " " + Items.DIAMOND + " " + Items.STONE + " " + Items.DIAMOND_SWORD + " " + (Items.DIAMOND == Items.DIAMOND));
        p("item empty", () -> ItemStack.EMPTY + " " + ItemStack.EMPTY.isEmpty() + " " + ItemStack.EMPTY.getItem() + " " + ItemStack.EMPTY.getCount() + " " + (ItemStack.EMPTY.copy() == ItemStack.EMPTY));
        int[] counts = {-2, 0, 1, 5, 64, 65, 1000};
        for (int n : counts) {
            p("item stack " + n, () -> {
                ItemStack s = new ItemStack(Items.DIAMOND, n);
                return s + " " + s.isEmpty() + " " + s.getItem() + " " + s.getCount() + " " + new ItemStack(Items.AIR, n).isEmpty() + " " + new ItemStack(Items.AIR, n).getCount();
            });
            p("item change " + n, () -> {
                ItemStack s = new ItemStack(Items.STONE, 10);
                ItemStack part = s.split(n);
                StringBuilder b = new StringBuilder().append(part).append(' ').append(s).append(' ').append(part.isEmpty());
                s.grow(n);
                b.append(' ').append(s);
                s.shrink(n * 2);
                b.append(' ').append(s).append(' ').append(s.isEmpty()).append(' ').append(s.getItem());
                s.setCount(n);
                b.append(' ').append(s).append(' ').append(s.isEmpty());
                ItemStack c = s.copy();
                b.append(' ').append(c == s).append(' ').append(c).append(' ').append(s.copyWithCount(n + 1)).append(' ').append(ItemStack.EMPTY.copyWithCount(3));
                return b;
            });
        }
        ItemStack[] stacks = {ItemStack.EMPTY, new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.STONE), new ItemStack(Items.AIR, 4), new ItemStack(Items.DIAMOND, 0)};
        for (int i = 0; i < stacks.length; i++) {
            for (int j = 0; j < stacks.length; j++) {
                ItemStack a = stacks[i], b = stacks[j];
                p("item compare " + i + " " + j, () -> ItemStack.matches(a, b) + " " + ItemStack.isSameItem(a, b) + " " + ItemStack.isSameItemSameComponents(a, b)
                        + " " + (ItemStack.hashItemAndComponents(a) == ItemStack.hashItemAndComponents(b)));
            }
        }
        p("fluids", () -> Fluids.EMPTY + " " + Fluids.WATER + " " + Fluids.LAVA + " " + (Fluids.WATER == Fluids.WATER));
        p("fluid empty", () -> FluidStack.EMPTY + " " + FluidStack.EMPTY.isEmpty() + " " + FluidStack.EMPTY.getFluid() + " " + FluidStack.EMPTY.getAmount() + " " + (FluidStack.EMPTY.copy() == FluidStack.EMPTY));
        for (int n : counts) {
            p("fluid stack " + n, () -> {
                FluidStack s = new FluidStack(Fluids.WATER, n);
                StringBuilder b = new StringBuilder().append(s).append(' ').append(s.isEmpty()).append(' ').append(s.getFluid()).append(' ').append(s.getAmount());
                b.append(' ').append(new FluidStack(Fluids.EMPTY, n).isEmpty()).append(' ').append(new FluidStack(Fluids.EMPTY, n).getAmount());
                FluidStack c = s.copy();
                c.grow(n);
                b.append(' ').append(c).append(' ').append(s.copyWithAmount(n + 1)).append(' ').append(c == s);
                c.shrink(n * 3);
                b.append(' ').append(c).append(' ').append(c.isEmpty()).append(' ').append(c.getFluid());
                c.setAmount(n);
                return b.append(' ').append(c).append(' ').append(c.isEmpty());
            });
        }
        FluidStack[] fluids = {FluidStack.EMPTY, new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.WATER, 500), new FluidStack(Fluids.LAVA, 1000), new FluidStack(Fluids.EMPTY, 10)};
        for (int i = 0; i < fluids.length; i++) {
            for (int j = 0; j < fluids.length; j++) {
                FluidStack a = fluids[i], b = fluids[j];
                p("fluid compare " + i + " " + j, () -> FluidStack.matches(a, b) + " " + FluidStack.isSameFluid(a, b) + " " + FluidStack.isSameFluidSameComponents(a, b)
                        + " " + (FluidStack.hashFluidAndComponents(a) == FluidStack.hashFluidAndComponents(b)));
            }
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
