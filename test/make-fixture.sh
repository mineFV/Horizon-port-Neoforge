#!/usr/bin/env bash
# Writes the test fixture into test/fixture/:
#   pocketcompass/  "Pocket Compass", a made-up NeoForge mod: its Java, its resources, its manifest;
#   oddities/       a second one, of things done oddly, that an analysis has to own up to not reading;
#   tidy/           a third one, with nothing in it that a converter can't finish;
#   extras/         classes laid over Pocket Compass for one test each;
#   horizon-src/    a stand-in for Horizon's source: a few hook points, to convert against;
#   fake-port/      a stand-in for horizon-port.jar that compiles nothing, for `check --compile`;
#   stubs/          stand-ins for the game, NeoForge, FML, the event bus and Mixin that they compile
#                   against. Only their names and shapes matter: they do nothing and aren't put in the
#                   mods' jars.
# Nothing here comes from a real mod, from NeoForge or from the game.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

F=fixture/pocketcompass
R=$F/resources
S=fixture/stubs
rm -rf fixture
mkdir -p "$R/META-INF" "$R/assets/pocketcompass/lang" "$R/data/pocketcompass/recipe"

# javafile <source root> <class>: writes that class's file from standard input.
javafile() {
  local file="$1/${2//.//}.java"
  mkdir -p "$(dirname "$file")"
  cat > "$file"
}

# ---------------------------------------------------------------- the mod's resources

cat > "$R/META-INF/neoforge.mods.toml" <<'EOF'
# Pocket Compass: the converter's made-up test mod.
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"
issueTrackerURL = "https://example.com/pocketcompass/issues"

[[mods]]
modId = "pocketcompass"
version = "${file.jarVersion}"
displayName = "Pocket Compass"
authors = "Mine, Second Author"
credits = "Thanks to the testers"
logoFile = "pocketcompass.png"
description = '''
Shows which way you face on the HUD.
Made up for the converter's tests.
'''

[[mixins]]
config = "pocketcompass.mixins.json"

[[accessTransformers]]
file = "META-INF/accesstransformer.cfg"

[[dependencies.pocketcompass]]
modId = "neoforge"
type = "required"
versionRange = "[26.3,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.pocketcompass]]
modId = "minecraft"
type = "required"
versionRange = "[26.3,27)"

[[dependencies.pocketcompass]]
modId = "jei"
type = "optional"
versionRange = "*"
side = "CLIENT"
reason = "Shows the compass recipe"
EOF

# The jar's manifest: ${file.jarVersion} in the metadata stands for its Implementation-Version.
cat > "$F/MANIFEST.MF" <<'EOF'
Implementation-Title: Pocket Compass
Implementation-Version: 1.2.3
EOF

# A Mixin config, with a comment and a trailing comma as real ones have.
cat > "$R/pocketcompass.mixins.json" <<'EOF'
{
  // Client only.
  "required": true,
  "package": "com.example.pocketcompass.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["MinecraftMixin"],
}
EOF

cat > "$R/META-INF/accesstransformer.cfg" <<'EOF'
# Made up for the test.
public net.minecraft.client.Minecraft someField
public-f net.minecraft.client.Minecraft otherField # a trailing comment
EOF

cat > "$R/assets/pocketcompass/lang/en_us.json" <<'EOF'
{ "key.pocketcompass.toggle": "Toggle the compass" }
EOF

cat > "$R/data/pocketcompass/recipe/pocket_compass.json" <<'EOF'
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": ["minecraft:compass"],
  "result": { "id": "pocketcompass:pocket_compass" }
}
EOF

# ---------------------------------------------------------------- stand-ins: the game

javafile $S net.minecraft.client.Minecraft <<'EOF'
package net.minecraft.client;
public class Minecraft {
    public static Minecraft getInstance() { return null; }
    public void tick() {}
    public void setScreen(Object screen) {}
}
EOF
javafile $S net.minecraft.client.gui.screens.Screen <<'EOF'
package net.minecraft.client.gui.screens;
public class Screen {
    protected void init() {}
}
EOF
javafile $S net.minecraft.world.level.block.state.BlockBehaviour <<'EOF'
package net.minecraft.world.level.block.state;
public class BlockBehaviour {
    public static class Properties {
        public static Properties of() { return new Properties(); }
        public Properties strength(float strength) { return this; }
    }
}
EOF
javafile $S net.minecraft.world.level.block.Block <<'EOF'
package net.minecraft.world.level.block;
import net.minecraft.world.level.block.state.BlockBehaviour;
public class Block extends BlockBehaviour {
    public Block(BlockBehaviour.Properties properties) {}
    public void stepOn() {}
}
EOF
javafile $S net.minecraft.world.item.Item <<'EOF'
package net.minecraft.world.item;
public class Item {
    public static class Properties {}
    public Item(Properties properties) {}
}
EOF
javafile $S net.minecraft.world.item.BlockItem <<'EOF'
package net.minecraft.world.item;
import net.minecraft.world.level.block.Block;
public class BlockItem extends Item {
    public BlockItem(Block block, Item.Properties properties) { super(properties); }
}
EOF
javafile $S net.minecraft.sounds.SoundEvent <<'EOF'
package net.minecraft.sounds;
public class SoundEvent {
    public static SoundEvent createVariableRangeEvent(String id) { return null; }
}
EOF
javafile $S net.minecraft.core.Registry <<'EOF'
package net.minecraft.core;
public interface Registry<T> {}
EOF
javafile $S net.minecraft.resources.ResourceKey <<'EOF'
package net.minecraft.resources;
public class ResourceKey<T> {}
EOF
javafile $S net.minecraft.core.registries.Registries <<'EOF'
package net.minecraft.core.registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
public class Registries {
    public static final ResourceKey<Registry<Block>> BLOCK = null;
    public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT = null;
}
EOF

# ---------------------------------------------------------------- stand-ins: the event bus, FML

javafile $S net.neoforged.bus.api.Event <<'EOF'
package net.neoforged.bus.api;
public abstract class Event {}
EOF
javafile $S net.neoforged.bus.api.ICancellableEvent <<'EOF'
package net.neoforged.bus.api;
public interface ICancellableEvent {
    default void setCanceled(boolean canceled) {}
}
EOF
javafile $S net.neoforged.bus.api.EventPriority <<'EOF'
package net.neoforged.bus.api;
public enum EventPriority { HIGHEST, HIGH, NORMAL, LOW, LOWEST }
EOF
javafile $S net.neoforged.bus.api.SubscribeEvent <<'EOF'
package net.neoforged.bus.api;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
EOF
javafile $S net.neoforged.bus.api.IEventBus <<'EOF'
package net.neoforged.bus.api;
import java.util.function.Consumer;
public interface IEventBus {
    void register(Object target);
    <T extends Event> void addListener(Consumer<T> consumer);
    <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer);
    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer);
}
EOF
javafile $S net.neoforged.api.distmarker.Dist <<'EOF'
package net.neoforged.api.distmarker;
public enum Dist { CLIENT, DEDICATED_SERVER }
EOF
javafile $S net.neoforged.api.distmarker.OnlyIn <<'EOF'
package net.neoforged.api.distmarker;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface OnlyIn {
    Dist value();
}
EOF
javafile $S net.neoforged.fml.common.Mod <<'EOF'
package net.neoforged.fml.common;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value();
    Dist[] dist() default {};
}
EOF
javafile $S net.neoforged.fml.common.EventBusSubscriber <<'EOF'
package net.neoforged.fml.common;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid() default "";
    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};
}
EOF
javafile $S net.neoforged.fml.ModContainer <<'EOF'
package net.neoforged.fml;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
public class ModContainer {
    public void registerConfig(ModConfig.Type type, IConfigSpec spec) {}
    public IEventBus getEventBus() { return null; }
}
EOF
javafile $S net.neoforged.fml.config.ModConfig <<'EOF'
package net.neoforged.fml.config;
public class ModConfig {
    public enum Type { COMMON, CLIENT, SERVER, STARTUP }
}
EOF
javafile $S net.neoforged.fml.config.IConfigSpec <<'EOF'
package net.neoforged.fml.config;
public interface IConfigSpec {}
EOF
javafile $S net.neoforged.fml.event.IModBusEvent <<'EOF'
package net.neoforged.fml.event;
public interface IModBusEvent {}
EOF
javafile $S net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent <<'EOF'
package net.neoforged.fml.event.lifecycle;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
public class FMLCommonSetupEvent extends Event implements IModBusEvent {}
EOF
javafile $S net.neoforged.fml.event.lifecycle.FMLClientSetupEvent <<'EOF'
package net.neoforged.fml.event.lifecycle;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
public class FMLClientSetupEvent extends Event implements IModBusEvent {}
EOF

# ---------------------------------------------------------------- stand-ins: NeoForge

javafile $S net.neoforged.neoforge.common.NeoForge <<'EOF'
package net.neoforged.neoforge.common;
import net.neoforged.bus.api.IEventBus;
public class NeoForge {
    public static final IEventBus EVENT_BUS = null;
}
EOF
javafile $S net.neoforged.neoforge.common.ModConfigSpec <<'EOF'
package net.neoforged.neoforge.common;
import net.neoforged.fml.config.IConfigSpec;
public class ModConfigSpec implements IConfigSpec {
    public static class BooleanValue {}
    public static class IntValue {}
    public static class Builder {
        public Builder comment(String comment) { return this; }
        public BooleanValue define(String path, boolean defaultValue) { return null; }
        public IntValue defineInRange(String path, int defaultValue, int min, int max) { return null; }
        public ModConfigSpec build() { return null; }
    }
}
EOF
javafile $S net.neoforged.neoforge.registries.DeferredHolder <<'EOF'
package net.neoforged.neoforge.registries;
import java.util.function.Supplier;
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    public T get() { return null; }
}
EOF
javafile $S net.neoforged.neoforge.registries.DeferredBlock <<'EOF'
package net.neoforged.neoforge.registries;
import net.minecraft.world.level.block.Block;
public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> {}
EOF
javafile $S net.neoforged.neoforge.registries.DeferredItem <<'EOF'
package net.neoforged.neoforge.registries;
import net.minecraft.world.item.Item;
public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> {}
EOF
javafile $S net.neoforged.neoforge.registries.DeferredRegister <<'EOF'
package net.neoforged.neoforge.registries;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
public class DeferredRegister<T> {
    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) { return null; }
    public static Blocks createBlocks(String namespace) { return null; }
    public static Items createItems(String namespace) { return null; }
    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) { return null; }
    public void register(IEventBus bus) {}

    public static class Blocks extends DeferredRegister<Block> {
        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier) { return null; }
        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> maker, BlockBehaviour.Properties properties) { return null; }
        public DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) { return null; }
    }

    public static class Items extends DeferredRegister<Item> {
        public DeferredItem<Item> registerSimpleItem(String name) { return null; }
        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) { return null; }
    }
}
EOF
javafile $S net.neoforged.neoforge.client.event.ClientTickEvent <<'EOF'
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public abstract class ClientTickEvent extends Event {
    public static class Pre extends ClientTickEvent {}
    public static class Post extends ClientTickEvent {}
}
EOF
javafile $S net.neoforged.neoforge.client.event.RenderGuiEvent <<'EOF'
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public abstract class RenderGuiEvent extends Event {
    public static class Pre extends RenderGuiEvent {}
    public static class Post extends RenderGuiEvent {}
}
EOF
javafile $S net.neoforged.neoforge.client.event.ClientChatReceivedEvent <<'EOF'
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
public class ClientChatReceivedEvent extends Event implements ICancellableEvent {}
EOF
javafile $S net.neoforged.neoforge.client.event.InputEvent <<'EOF'
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public abstract class InputEvent extends Event {
    public static class Key extends InputEvent {}
}
EOF
javafile $S net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent <<'EOF'
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
public class RegisterKeyMappingsEvent extends Event implements IModBusEvent {}
EOF
javafile $S net.neoforged.neoforge.event.tick.ServerTickEvent <<'EOF'
package net.neoforged.neoforge.event.tick;
import net.neoforged.bus.api.Event;
public abstract class ServerTickEvent extends Event {
    public static class Post extends ServerTickEvent {}
}
EOF
javafile $S net.neoforged.neoforge.event.level.BlockEvent <<'EOF'
package net.neoforged.neoforge.event.level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
public abstract class BlockEvent extends Event {
    public static class BreakEvent extends BlockEvent implements ICancellableEvent {}
}
EOF
javafile $S net.neoforged.neoforge.event.entity.player.PlayerEvent <<'EOF'
package net.neoforged.neoforge.event.entity.player;
import net.neoforged.bus.api.Event;
public abstract class PlayerEvent extends Event {
    public static class PlayerLoggedInEvent extends PlayerEvent {}
}
EOF
javafile $S net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent <<'EOF'
package net.neoforged.neoforge.network.event;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
public class RegisterPayloadHandlersEvent extends Event implements IModBusEvent {}
EOF

# ---------------------------------------------------------------- stand-ins: Mixin

javafile $S org.spongepowered.asm.mixin.Mixin <<'EOF'
package org.spongepowered.asm.mixin;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.CLASS)
public @interface Mixin {
    Class<?>[] value() default {};
    String[] targets() default {};
}
EOF
javafile $S org.spongepowered.asm.mixin.injection.At <<'EOF'
package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    String value();
    String target() default "";
}
EOF
javafile $S org.spongepowered.asm.mixin.injection.Inject <<'EOF'
package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String[] method();
    At[] at();
    boolean cancellable() default false;
}
EOF
javafile $S org.spongepowered.asm.mixin.injection.Redirect <<'EOF'
package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {
    String[] method();
    At at();
}
EOF
javafile $S org.spongepowered.asm.mixin.injection.callback.CallbackInfo <<'EOF'
package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfo {}
EOF

# ---------------------------------------------------------------- the mod

M=$F/src
javafile $M com.example.pocketcompass.PocketCompass <<'EOF'
package com.example.pocketcompass;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** The mod's entry point on both sides. */
@Mod(PocketCompass.MODID)
public class PocketCompass {
    public static final String MODID = "pocketcompass";

    public PocketCompass(IEventBus modBus, ModContainer container, Dist dist) {
        ModRegistry.BLOCKS.register(modBus);
        ModRegistry.ITEMS.register(modBus);
        ModRegistry.SOUNDS.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(Net::register);
        NeoForge.EVENT_BUS.addListener(this::onBreak);
        NeoForge.EVENT_BUS.register(new ServerEvents());
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {}

    // Marked, and handed to addListener too, as real mods do: one listener, not two.
    @SubscribeEvent
    private void onBreak(BlockEvent.BreakEvent event) {
        event.setCanceled(false);
    }
}
EOF
javafile $M com.example.pocketcompass.PocketCompassClient <<'EOF'
package com.example.pocketcompass;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The mod's entry point on the client only. */
@Mod(value = PocketCompass.MODID, dist = Dist.CLIENT)
public class PocketCompassClient {
    public PocketCompassClient(IEventBus modBus) {
        modBus.addListener(PocketCompassClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, InputEvent.Key.class, PocketCompassClient::onKey);
        NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.class, event -> event.setCanceled(false));
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {}

    private static void onKey(InputEvent.Key event) {}
}
EOF
javafile $M com.example.pocketcompass.ClientEvents <<'EOF'
package com.example.pocketcompass;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = PocketCompass.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft.getInstance().tick();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {}

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onChat(ClientChatReceivedEvent event) {}

    // Not static: an @EventBusSubscriber class registers only its static methods.
    @SubscribeEvent
    public void notStatic(ClientTickEvent.Pre event) {}
}
EOF
javafile $M com.example.pocketcompass.ServerEvents <<'EOF'
package com.example.pocketcompass;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Registered as an object: NeoForge.EVENT_BUS.register(new ServerEvents()). */
public class ServerEvents {
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {}

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {}
}
EOF
javafile $M com.example.pocketcompass.Orphan <<'EOF'
package com.example.pocketcompass;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** A listener nothing registers. */
public class Orphan {
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {}
}
EOF
javafile $M com.example.pocketcompass.Net <<'EOF'
package com.example.pocketcompass;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

final class Net {
    static void register(RegisterPayloadHandlersEvent event) {}
}
EOF
javafile $M com.example.pocketcompass.ModRegistry <<'EOF'
package com.example.pocketcompass;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PocketCompass.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PocketCompass.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, PocketCompass.MODID);

    public static final DeferredBlock<Block> COMPASS_BLOCK =
            BLOCKS.registerSimpleBlock("compass_block", BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredBlock<SpinningBlock> SPINNING_BLOCK =
            BLOCKS.register("spinning_block", () -> new SpinningBlock(BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> PLAIN_BLOCK =
            BLOCKS.registerBlock("plain_block", Block::new, BlockBehaviour.Properties.of());
    public static final DeferredItem<Item> POCKET_COMPASS = ITEMS.registerSimpleItem("pocket_compass");
    public static final DeferredItem<BlockItem> COMPASS_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("compass_block", COMPASS_BLOCK);
    public static final DeferredHolder<SoundEvent, SoundEvent> DING =
            SOUNDS.register("ding", () -> SoundEvent.createVariableRangeEvent("pocketcompass:ding"));

    private ModRegistry() {}
}
EOF
javafile $M com.example.pocketcompass.SpinningBlock <<'EOF'
package com.example.pocketcompass;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A block with behaviour of its own. */
public class SpinningBlock extends Block {
    public SpinningBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn() {}
}
EOF
javafile $M com.example.pocketcompass.Config <<'EOF'
package com.example.pocketcompass;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SHOW = BUILDER.comment("Show the compass").define("show", true);
    public static final ModConfigSpec.IntValue SIZE = BUILDER.defineInRange("size", 2, 1, 5);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
EOF
javafile $M com.example.pocketcompass.CompassScreen <<'EOF'
package com.example.pocketcompass;

import net.minecraft.client.gui.screens.Screen;

public class CompassScreen extends Screen {
    @Override
    protected void init() {}
}
EOF
# Not part of the mod's story: shapes of bytecode the class reader has to step over.
javafile $M com.example.pocketcompass.Shapes <<'EOF'
package com.example.pocketcompass;

final class Shapes {
    static String name(int n, String s) {
        String out;
        switch (n) {
            case 0: out = "zero"; break;
            case 1: out = "one"; break;
            case 2: out = "two"; break;
            default: out = "many";
        }
        switch (n) {
            case 10: out += "ten"; break;
            case 1000: out += "thousand"; break;
            default: break;
        }
        switch (s) {
            case "a": out += "A"; break;
            case "b": out += "B"; break;
            default: break;
        }
        long big = 123456789012L;
        double ratio = 2.5;
        int[][] grid = new int[3][4];
        for (int i = 0; i < 300; i++) n += 1000;
        String marker = "after-the-switches";
        return out + big + ratio + grid.length + n + marker;
    }
}
EOF
javafile $M com.example.pocketcompass.mixin.MinecraftMixin <<'EOF'
package com.example.pocketcompass.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void pocketcompass$onTick(CallbackInfo ci) {}

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Ljava/lang/Object;)V"))
    private void pocketcompass$noScreen(Minecraft minecraft, Object screen) {}
}
EOF
# A Mixin class that no config lists.
javafile $M com.example.pocketcompass.mixin.UnlistedMixin <<'EOF'
package com.example.pocketcompass.mixin;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class UnlistedMixin {}
EOF

# ---------------------------------------------------------------- classes laid over the mod for one test each

# A Mixin class no config lists, with an injector a converter would switch on if it were handed over.
X=fixture/extras
javafile $X/stray com.example.pocketcompass.mixin.StrayMixin <<'EOF'
package com.example.pocketcompass.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class StrayMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void pocketcompass$stray(CallbackInfo ci) {}
}
EOF
# The mod saying it has a config screen of its own.
javafile $X/screen com.example.pocketcompass.ConfigScreens <<'EOF'
package com.example.pocketcompass;

import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ConfigScreens implements IConfigScreenFactory {
    @Override
    public Object createScreen(Object container, Object parent) {
        return new CompassScreen();
    }
}
EOF
javafile $S net.neoforged.neoforge.client.gui.IConfigScreenFactory <<'EOF'
package net.neoforged.neoforge.client.gui;

public interface IConfigScreenFactory {
    Object createScreen(Object container, Object parent);
}
EOF

# Assets that a content mod declares things from: an item model, and a blockstate with no property.
mkdir -p "$X/content/assets/pocketcompass/models/item" "$X/content/assets/pocketcompass/models/block" "$X/content/assets/pocketcompass/blockstates"
echo '{ "parent": "minecraft:item/generated", "textures": { "layer0": "pocketcompass:item/pocket_compass" } }' > "$X/content/assets/pocketcompass/models/item/pocket_compass.json"
echo '{ "variants": { "": { "model": "pocketcompass:block/plain_block" } } }' > "$X/content/assets/pocketcompass/blockstates/plain_block.json"
echo '{ "parent": "minecraft:block/cube_all", "textures": { "all": "pocketcompass:block/plain_block" } }' > "$X/content/assets/pocketcompass/models/block/plain_block.json"

# ---------------------------------------------------------------- a third mod: nothing a converter can't finish

# No event, no registry, no Mixin: horizon-port sees nothing left to do in it.
T=fixture/tidy
mkdir -p "$T/resources/META-INF"
cat > "$T/resources/META-INF/neoforge.mods.toml" <<'EOF'
license = "MIT"

[[mods]]
modId = "tidy"
version = "1.0.0"
displayName = "Tidy"
authors = "Mine"
description = "Adds two numbers."
EOF
javafile $T/src com.example.tidy.Tidy <<'EOF'
package com.example.tidy;

import net.neoforged.fml.common.Mod;

@Mod("tidy")
public final class Tidy {
    public Tidy() {}

    public static int sum(int a, int b) {
        return a + b;
    }
}
EOF

# ---------------------------------------------------------------- a stand-in for Horizon's source

# What horizon-port reads of it: its commit, the game version, the horizon-mixins commit, and Rust to index.
# The hook points are a handful with made-up contents. Nothing here is Horizon's code.
H=fixture/horizon-src
mkdir -p "$H/crates/horizon-client/src/game" "$H/crates/horizon-core/src" "$H/launcher/src"
echo "0123456789abcdef0123456789abcdef01234567" > "$H/.horizon-revision"
echo 'pub const VANILLA_VERSION: &str = "26.3";' > "$H/crates/horizon-core/src/version.rs"
echo 'pub const MIXINS_REV: &str = "89abcdef0123456789abcdef0123456789abcdef";' > "$H/launcher/src/native_mods.rs"
cat > "$H/crates/horizon-client/src/game.rs" <<'EOF'
mod native_hooks;

pub struct Game<'a> {
    pub name: &'a str,
}
EOF
cat > "$H/crates/horizon-client/src/game/native_hooks.rs" <<'EOF'
use super::Game;

pub struct NativeMod {
    pub id: String,
    pub title: String,
    pub description: String,
    pub keys: Vec<[String; 3]>,
}

impl Game<'_> {
    /// The mods listed in Options > Mods.
    pub(super) fn native_mods(&mut self, mods: &mut Vec<NativeMod>) {
        let _ = mods;
    }

    /// Once per tick.
    pub(super) fn native_tick(&mut self) {}

    /// A mod's settings page.
    pub(super) fn native_settings(&mut self, mod_id: &str, buttons: &mut Vec<String>) {
        let _ = (mod_id, buttons);
    }

    /// A click on a mod's settings page.
    pub(super) fn native_settings_click(&mut self, mod_id: &str, button: usize) {
        let _ = (mod_id, button);
    }

    /// A player breaks the block at `pos`. Set `canceled` to keep it.
    pub(super) fn native_destroy_block(&mut self, pos: [i32; 3], canceled: &mut bool) {
        let _ = (pos, canceled);
    }

    /// The server's tick, before and after.
    pub(super) fn native_server_tick(&mut self, after: bool) {
        let _ = after;
    }

    /// `name` joined the game.
    pub(super) fn native_player_logged_in(&self, name: &str) {
        let _ = name;
    }

    /// Before the GUI is drawn.
    pub(super) fn native_gui_render_pre(&mut self) {}

    /// A key the mod listed was pressed.
    pub(super) fn native_key_press(&mut self, key: &str) {
        let _ = key;
    }
}
EOF
# Horizon's hook catalogue, in its shape: what the event table is read from. The labels are made up to
# give one event of each kind: named alone, named with a note, sharing a hook point, another phase only,
# said to have no counterpart, and named for a hook point that has no function in the source.
cat > "$H/native-hooks.json" <<'EOF'
{
  "helpers": [],
  "hooks": [
    { "name": "mods", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_mods", "mod_api": { "contract": "collect" } },
    { "name": "tick", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_tick", "neoforge": "ClientTickEvent.Post",
      "side": "client", "cancellable": false, "mod_api": { "contract": "listen" } },
    { "name": "settings", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_settings", "mod_api": { "contract": "collect" } },
    { "name": "settings_click", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_settings_click", "mod_api": { "contract": "listen" } },
    { "name": "destroy_block", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_destroy_block", "neoforge": "BlockEvent.BreakEvent",
      "side": "server", "cancellable": true, "mod_api": { "contract": "cancel", "out": "canceled" } },
    { "name": "server_tick", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_server_tick",
      "neoforge": "ServerTickEvent.Pre / .Post", "side": "server", "mod_api": { "contract": "listen" } },
    { "name": "player_logged_in", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_player_logged_in",
      "neoforge": "PlayerEvent.PlayerLoggedInEvent (once the player is in the world, not before)", "side": "server", "mod_api": { "contract": "listen" } },
    { "name": "gui_render_pre", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_gui_render_pre", "neoforge": "RenderGuiEvent.Pre",
      "side": "client", "mod_api": { "contract": "listen" } },
    { "name": "key_press", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_key_press",
      "neoforge": "none (InputEvent.Key hears every key; this hears the mod's own keys only)", "side": "client", "mod_api": { "contract": "listen" } },
    { "name": "register_key_mappings", "version": 1, "target": "horizon_client::game::native_hooks::Game<'_>::native_register_key_mappings",
      "neoforge": "RegisterKeyMappingsEvent", "side": "client", "mod_api": { "contract": "collect" } }
  ]
}
EOF
# A hook point that is only being prepared: not in the catalogue, so not in the hooks API.
mkdir -p "$H/tools/native-hook-fragments"
echo "Fragments: hook points asked for, not merged yet." > "$H/tools/native-hook-fragments/README.md"
cat > "$H/tools/native-hook-fragments/client_chat.json" <<'EOF'
{
  "hooks": [
    { "name": "client_chat_received", "target": "horizon_client::game::native_hooks::Game<'_>::native_client_chat_received",
      "neoforge": "ClientChatReceivedEvent", "side": "client", "cancellable": true, "mod_api": { "contract": "cancel", "out": "canceled" } }
  ]
}
EOF
# One very long line, as Horizon's source has: horizon-port needs a bigger stack than Java's default to read it.
{
  printf 'pub const LONG: &str = "'
  head -c 400000 /dev/zero | tr '\0' 'a'
  printf '";\n'
} > "$H/crates/horizon-client/src/game/long_line.rs"

# ---------------------------------------------------------------- a stand-in for horizon-port.jar

# For `check --compile`: it says what it was given and ends as FAKE_PORT tells it, so that nothing is
# compiled in the tests. It has no list of hook points to give.
mkdir -p fixture/fake-port
cat > fixture/fake-port/FakePort.java <<'EOF'
import java.lang.management.ManagementFactory;

public final class FakePort {
    public static void main(String[] args) {
        if (args.length == 0 || !args[0].equals("check")) {
            System.err.println("fake horizon-port: it only checks");
            System.exit(1);
        }
        System.out.println("fake horizon-port: " + String.join(" ", args));
        System.out.println("its stack: " + ManagementFactory.getRuntimeMXBean().getInputArguments().stream().filter(a -> a.startsWith("-Xss")).findFirst().orElse("default"));
        String how = String.valueOf(System.getenv("FAKE_PORT"));
        if (how.equals("fail")) {
            System.out.println("src/lib.rs:3:5: error[E0425]: cannot find value `x` in this scope");
            System.out.println("The bundle's Rust doesn't compile (above).");
            System.exit(1);
        }
        if (how.equals("no-cargo")) {
            System.out.println("No cargo found. The check crate is in target/horizon-check: run `cargo check` there.");
            return;
        }
        System.out.println("ok: the bundle's Rust compiles against Horizon's helpers");
    }
}
EOF

# ---------------------------------------------------------------- a second mod: things done oddly

# What an analysis can't be sure of, or that NeoForge itself would refuse. It has to say so, not guess.
O=fixture/oddities
mkdir -p "$O/resources/META-INF"
cat > "$O/resources/META-INF/neoforge.mods.toml" <<'EOF'
license = "MIT"

[[mods]]
modId = "oddities"

[[mixins]]
config = "missing.mixins.json"

[[mixins]]
config = "oddities.mixins.json"

[[accessTransformers]]
file = "META-INF/missing_at.cfg"
EOF
# It lists a class the jar doesn't have.
cat > "$O/resources/oddities.mixins.json" <<'EOF'
{ "package": "com.example.oddities.mixin", "mixins": ["GoneMixin"] }
EOF
# In the jar, but the metadata doesn't list it.
cat > "$O/resources/META-INF/accesstransformer.cfg" <<'EOF'
public net.minecraft.client.Minecraft someField
EOF
# A library carried inside the jar, as NeoForge's jar-in-jar does. Only its name matters: it is never opened.
mkdir -p "$O/resources/META-INF/jarjar"
echo "a library's jar would be here" > "$O/resources/META-INF/jarjar/library-1.0.jar"
javafile $O/src com.example.oddities.Oddities <<'EOF'
package com.example.oddities;

import java.util.function.Consumer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// The metadata's mod is "oddities", not this.
@Mod("somethingelse")
public class Oddities {
    // NeoForge hands a @Mod class no String.
    public Oddities(IEventBus bus, String extra) {
        // An object: its non-static listeners.
        NeoForge.EVENT_BUS.register(this);
        // A class: its static listeners.
        NeoForge.EVENT_BUS.register(StaticHandlers.class);
        // A mod bus event, on the game bus.
        NeoForge.EVENT_BUS.addListener(this::wrongBus);
        // A handler kept in a variable.
        Consumer<ServerTickEvent.Post> kept = event -> {};
        NeoForge.EVENT_BUS.addListener(kept);
        more(bus);
    }

    // A second public constructor.
    public Oddities() {}

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {}

    // Static, in a class that is registered as an object.
    @SubscribeEvent
    public static void misplaced(ServerTickEvent.Post event) {}

    private void wrongBus(FMLCommonSetupEvent event) {}

    // A bus handed on: nothing here tells which one it is.
    static void more(IEventBus anyBus) {
        anyBus.addListener(Oddities::late);
    }

    private static void late(ServerTickEvent.Post event) {}
}
EOF
# The mod's own events: one for the mod bus, one that extends an event of NeoForge's.
javafile $O/src com.example.oddities.OwnEvents <<'EOF'
package com.example.oddities;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class OwnEvents {
    public static class Setup extends Event implements IModBusEvent {}

    public static class Late extends ServerTickEvent.Post {}

    private OwnEvents() {}
}
EOF
javafile $O/src com.example.oddities.OwnEventHandlers <<'EOF'
package com.example.oddities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "oddities")
public final class OwnEventHandlers {
    @SubscribeEvent
    public static void onOwnSetup(OwnEvents.Setup event) {}

    @SubscribeEvent
    public static void onOwnLate(OwnEvents.Late event) {}
}
EOF
javafile $O/src com.example.oddities.StaticHandlers <<'EOF'
package com.example.oddities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class StaticHandlers {
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {}

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientOnly(ServerTickEvent.Post event) {}
}
EOF

echo "Wrote test/$F, test/$O, test/$T, test/$X, test/$H and test/$S"
