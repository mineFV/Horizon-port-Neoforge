#!/usr/bin/env bash
# Writes the test fixture into test/fixture/:
#   pocketcompass/  "Pocket Compass", a made-up NeoForge mod: its Java, its resources, its manifest;
#   oddities/       a second one, of things done oddly, that an analysis has to own up to not reading;
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

echo "Wrote test/$F, test/$O and test/$S"
