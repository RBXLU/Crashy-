package dev.pycodder.crashy.client;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * The glued positions the server last told this client about.
 *
 * <p>Deliberately free of any client-only imports: the network handler names this class, and that
 * code is also loaded on a dedicated server.
 */
public final class GlueHighlightState {

    /** Updates arrive twice a second; anything older than this means the highlight is over. */
    private static final long EXPIRY_MILLIS = 1_500L;

    private static volatile List<BlockPos> positions = List.of();
    private static volatile long updatedAt = 0L;

    private GlueHighlightState() {
    }

    public static void accept(final List<BlockPos> newPositions) {
        positions = List.copyOf(newPositions);
        updatedAt = System.currentTimeMillis();
    }

    public static List<BlockPos> current() {
        if (System.currentTimeMillis() - updatedAt > EXPIRY_MILLIS) {
            return List.of();
        }
        return positions;
    }

    public static void clear() {
        positions = List.of();
        updatedAt = 0L;
    }
}
