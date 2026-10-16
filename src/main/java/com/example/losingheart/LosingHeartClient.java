package com.example.losingheart;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class LosingHeartClient implements ClientModInitializer {
    private static final Identifier HEART = Identifier.ofVanilla("hud/heart/full");
    private static final int SIZE = 18;          // 9px сердечко x2
    private static final long KEEP_MS = 3000;    // сколько помнить последнюю цель, которую ударил

    private LivingEntity lastTarget;
    private long lastHitTime;

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof PlayerEntity target && target != player) {
                lastTarget = target;
                lastHitTime = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null) return;

        LivingEntity target = findTarget(mc);
        if (target == null || !target.isAlive()) return;

        float mine = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float his = target.getHealth() + target.getAbsorptionAmount();
        if (mine >= his) return; // проигрываем только когда HP строго меньше

        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int x = cx - SIZE / 2;
        int y = cy - 14 - SIZE;

        ctx.drawGuiTexture(RenderLayer::getGuiTextured, HEART, x, y, SIZE, SIZE);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, "-", cx, y + (SIZE - 8) / 2, 0xFFFFFFFF);
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        // 1) игрок под прицелом
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Object e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof PlayerEntity p && p != mc.player) return p;
        }
        // 2) иначе тот, кого недавно ударили
        if (lastTarget != null && System.currentTimeMillis() - lastHitTime < KEEP_MS) return lastTarget;
        return null;
    }
}
