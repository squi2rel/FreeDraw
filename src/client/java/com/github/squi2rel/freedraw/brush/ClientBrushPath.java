package com.github.squi2rel.freedraw.brush;

import com.github.squi2rel.freedraw.FreeDraw;
import com.github.squi2rel.freedraw.FreeDrawClient;
import com.github.squi2rel.freedraw.network.ClientPacketHandler;
import com.github.squi2rel.freedraw.render.DirtyBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientBrushPath extends BrushPath {
    public static final int MAX = 4096;
    private static final Vector3d tmp1 = new Vector3d(), tmp2 = new Vector3d(),  tmp3 = new Vector3d(),  tmp4 = new Vector3d();
    public ArrayList<DirtyBuffer> buffers = new ArrayList<>();
    public float[] posCache;
    public int lastIndex = 0;
    public boolean created = false, finalized = false;
    public RingSampler sampler = new RingSampler(0.01f);

    public ClientBrushPath(UUID uuid, Vec3d offset) {
        super(uuid, Objects.requireNonNull(MinecraftClient.getInstance().player).getWorld().getRegistryKey().getValue().toString(), new Vector3d(offset.x, offset.y, offset.z));
    }

    public ClientBrushPath(UUID uuid, Vector3d offset) {
        super(uuid, Objects.requireNonNull(MinecraftClient.getInstance().player).getWorld().getRegistryKey().getValue().toString(), offset);
    }

    public void add(Vec3d prev1, Vec3d now1, int color) {
        Vector3d prev = tmp1.set(prev1.x, prev1.y, prev1.z).sub(offset);
        Vector3d now = tmp2.set(now1.x, now1.y, now1.z).sub(offset);
        addRaw(prev, now, color, FreeDrawClient.maxPoints);
    }

    public void addFirst(Vector3d first, Vector3d second, int color) {
        points.add(new BrushPoint(sampler.sample(first.add(first.sub(second, tmp1), tmp2).sub(first), second), new Vector3f(first), color, false));
    }

    public void addRaw(Vector3d prev, Vector3d now, int color, int limit) {
        double dist = now.sub(prev, tmp3).length();
        if (dist < 1e-6) return;
        Vector3d p = prev, tmp = tmp3.set(prev);
        int steps = (int) (dist * 10);
        if (steps > 10000) {
            FreeDraw.LOGGER.warn("Max steps reached ({})", steps);
            return;
        }
        for (int i = 1; i <= steps; i++) {
            if (points.size() >= limit) {
                actionBar(limit, points.size());
                return;
            }
            double t = (double) i / (steps + 1);
            p = prev.lerp(now, t, tmp4);
            points.add(new BrushPoint(sampler.sample(tmp, p), new Vector3f(p), color, true));
            tmp = tmp3.set(p);
        }
        if (points.size() >= limit) {
            actionBar(limit, points.size());
            return;
        }
        points.add(new BrushPoint(sampler.sample(p, now), new Vector3f(now), color, false));
        updateBounds((float) now.x, (float) now.y, (float) now.z);
        if (limit != Integer.MAX_VALUE) actionBar(limit, points.size());
        if (buffers.size() <= points.size() / MAX) {
            if (!buffers.isEmpty()) buffers.getLast().make();
            buffers.add(new DirtyBuffer());
        }
        buffers.getLast().dirty = true;
    }

    private static void actionBar(int limit, int s) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("" + s).formatted(s < limit * 0.3 ? Formatting.GREEN : s < limit * 0.6 ? Formatting.YELLOW : s < limit * 0.9 ? Formatting.GOLD : Formatting.RED).append(" / " + limit), false);
    }

    public void cache() {
        posCache = new float[points.size() * 3];
        for (int i = 0; i < points.size(); i += 3) {
            Vector3f pos = points.get(i).pos;
            posCache[i] = pos.x;
            posCache[i + 1] = pos.y;
            posCache[i + 2] = pos.z;
        }
        sampler = null;
    }

    public void remove() {
        for (DirtyBuffer buffer : buffers) {
            buffer.close();
        }
    }

    public List<BrushPoint> getRange(int id) {
        return points.subList(Math.max(id * MAX - 1, 0), Math.min((id + 1) * MAX, points.size()));
    }

    public List<BrushPoint> getNewPoints() {
        List<BrushPoint> subList = points.subList(lastIndex, points.size());
        lastIndex = points.size();
        return subList.stream().filter(s -> !s.isGenerated).collect(Collectors.toList());
    }

    public void created() {
        if (!created && finalized) {
            flush(true);
        }
        created = true;
    }

    public void flush(boolean finalize) {
        if (!created) return;
        List<BrushPoint> points = getNewPoints();
        if (points.isEmpty() && !finalize) return;
        ClientPacketHandler.addPoints(uuid, points, finalize);
    }
}
