package horizonneo.probe;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * The probe as a NeoForge mod (`standins --horizon-src`): when the server has started, with the game's registries
 * filled, it asks GameProbe's questions and writes the answers to horizon-probe.txt where the server runs. Run by
 * `dump`'s path (Horizon's NeoForge oracle), first among the listeners, as the oracle stops the server in its own.
 */
@Mod("horizonprobe")
public final class InServer {
    public InServer(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStartedEvent event) -> {
            GameProbe.REGISTRIES = event.getServer().registryAccess();
            run();
            ids();
        });
    }

    /** The game's fluid ids, which `dump`'s facts don't have, to horizon-ids.txt: the bridge gives them to the stand-ins. */
    static void ids() {
        try {
            java.util.List<String> fluids = new java.util.ArrayList<>();
            for (net.minecraft.resources.ResourceLocation id : net.minecraft.core.registries.BuiltInRegistries.FLUID.keySet()) fluids.add(id.toString());
            java.util.Collections.sort(fluids);
            Files.writeString(Path.of("horizon-ids.txt"), "fluid " + String.join(",", fluids) + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // The file's absence tells it.
        }
    }

    static void run() {
        try (PrintStream out = new PrintStream(Files.newOutputStream(Path.of("horizon-probe.txt")), true, StandardCharsets.UTF_8)) {
            GameProbe.OUT = out;
            GameProbe.main(new String[] {"server"});
        } catch (Throwable e) {
            try {
                Files.writeString(Path.of("horizon-probe.txt"), "failed: " + e, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // Nothing more can be said: the file's absence tells it.
            }
        }
    }
}
