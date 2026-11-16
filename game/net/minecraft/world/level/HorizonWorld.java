package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.HorizonRegistries;

/**
 * Not the game's: a level's questions to Horizon's world (horizon-port-neoforge's bridge). `ask` is native: the
 * translation sends the question to the thread that holds Horizon's world (bridge.rs) and waits for the answer.
 * One question for now: the block at a position, by its id.
 */
public final class HorizonWorld {
    private HorizonWorld() {}

    /** Horizon's answer to a question ("block x y z": the block's id there); null when no world answers. */
    static native String ask(String question);

    /**
     * The state at a position: its block (the mod's own, or one made for a block the mod didn't register, with its id),
     * in the state of the properties Horizon gives ("id[key=value,...]"), its default state when they don't match.
     */
    public static BlockState blockState(BlockPos pos) {
        String answer = ask("block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        if (answer == null) throw new IllegalStateException("no world answers for " + pos);
        int bracket = answer.indexOf('[');
        String id = bracket < 0 ? answer : answer.substring(0, bracket);
        Block block = HorizonRegistries.block(id);
        if (bracket < 0 || !answer.endsWith("]")) return block.defaultBlockState();
        java.util.Map<String, String> given = new java.util.HashMap<>();
        for (String pair : answer.substring(bracket + 1, answer.length() - 1).split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) given.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        for (BlockState state : block.getStateDefinition().horizonStates()) {
            if (matches(state, given)) return state;
        }
        return block.defaultBlockState();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean matches(BlockState state, java.util.Map<String, String> given) {
        for (java.util.Map.Entry<net.minecraft.world.level.block.state.properties.Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
            net.minecraft.world.level.block.state.properties.Property p = e.getKey();
            if (!p.getName((Comparable) e.getValue()).equals(given.get(p.getName()))) return false;
        }
        return true;
    }

    /**
     * Level.setBlock: when the block there is the state's, each of the state's properties set in Horizon's world
     * ("set x y z key value flags"); true when one changed. Another block isn't set (false).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean setBlock(BlockPos pos, BlockState state, int flags) {
        String answer = ask("block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        if (answer == null) return false;
        int bracket = answer.indexOf('[');
        String id = bracket < 0 ? answer : answer.substring(0, bracket);
        Block block = state.getBlock();
        if (block.horizonId() == null || !block.horizonId().toString().equals(id)) return false;
        boolean changed = false;
        for (java.util.Map.Entry<net.minecraft.world.level.block.state.properties.Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
            net.minecraft.world.level.block.state.properties.Property p = e.getKey();
            String said = ask("set " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + p.getName() + " " + p.getName((Comparable) e.getValue()) + " " + flags);
            if ("true".equals(said)) changed = true;
        }
        return changed;
    }
}
