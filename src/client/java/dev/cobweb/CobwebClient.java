package dev.cobweb;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.client.network.ClientPlayerEntity;

public final class CobwebClient implements ClientModInitializer {
    private static KeyBinding key; private long restoreAt;
    private long lastColorRefresh; private int original = -1;
    private int webSlot = -1;
    private ClientPlayerEntity owner;
    private boolean bindingsInitialized;

    @Override public void onInitializeClient() {
        CobwebConfig.load();
        MinecraftClient client = MinecraftClient.getInstance();
        CobwebAppearance.register();
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
            (state, world, pos, index) -> CobwebConfig.tint(), net.minecraft.block.Blocks.COBWEB);
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cobweb.place", InputUtil.Type.KEYSYM, -1,
            KeyBinding.Category.create(Identifier.of("cobweb", "actions"))));
        ClientTickEvents.START_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (!bindingsInitialized) {
            if (client.options == null) return;
            restoreSavedHotbarBinding(client);
            suppressConflictingHotbarBinding(client, KeyBindingHelper.getBoundKeyOf(key));
            bindingsInitialized = true;
        }
 if (CobwebConfig.style == 2 && client.world != null && System.nanoTime()-lastColorRefresh > 250_000_000L) { if (client.player != null) { var origin=client.player.getBlockPos(); for (var pos : net.minecraft.util.math.BlockPos.iterate(origin.add(-8,-8,-8), origin.add(8,8,8))) { if (client.world.getBlockState(pos).isOf(net.minecraft.block.Blocks.COBWEB)) client.worldRenderer.scheduleBlockRenders(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()); } } lastColorRefresh=System.nanoTime(); }
        if (original != -1) { if (client.player == owner && System.nanoTime() < restoreAt) { while (key.wasPressed()) {} return; }
            if (client.player == owner && owner.getInventory().getSelectedSlot() == webSlot) {
                owner.getInventory().setSelectedSlot(original);
            }
            original = -1;
            webSlot = -1;
            owner = null;
            while (key.wasPressed()) {}
            return;
        }
        boolean pressed = false;
        while (key.wasPressed()) pressed = true;
        if (!pressed || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null
                || !client.player.isAlive() || client.player.isSpectator()) return;
        if (client.player.isUsingItem()) {
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        var inventory = client.player.getInventory();
        int slot = -1;
        if (inventory.getSelectedStack().isOf(Items.COBWEB)) slot = inventory.getSelectedSlot();
        for (int i = 0; slot == -1 && i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.COBWEB)) slot = i;
        }
        if (slot == -1) {
            return;
        }
        owner = client.player;
        original = inventory.getSelectedSlot();
        webSlot = slot; restoreAt = System.nanoTime() + CobwebConfig.getDelayMillis() * 1_000_000L;
        inventory.setSelectedSlot(slot);
        try {
            var result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
            if (result.isAccepted()) client.player.swingHand(Hand.MAIN_HAND);

        } catch (RuntimeException error) {
            inventory.setSelectedSlot(original);
            original = -1;
            webSlot = -1;
            owner = null;
            throw error;
        }
    }

    private static void restoreSavedHotbarBinding(MinecraftClient client) {
        int index=CobwebConfig.getDisplacedHotbarIndex();
        String translation=CobwebConfig.getDisplacedHotbarKey();
        if(index>=0 && index<client.options.hotbarKeys.length && !translation.isEmpty()) {
            client.options.hotbarKeys[index].setBoundKey(InputUtil.fromTranslationKey(translation));
            CobwebConfig.setDisplacedHotbar(-1, "");
            KeyBinding.updateKeysByCode();
        }
    }

    private static void suppressConflictingHotbarBinding(MinecraftClient client, InputUtil.Key cobwebKey) {
        int oldIndex=CobwebConfig.getDisplacedHotbarIndex();
        String oldTranslation=CobwebConfig.getDisplacedHotbarKey();
        if(oldIndex>=0 && oldIndex<client.options.hotbarKeys.length && !oldTranslation.isEmpty()) {
            client.options.hotbarKeys[oldIndex].setBoundKey(InputUtil.fromTranslationKey(oldTranslation));
            CobwebConfig.setDisplacedHotbar(-1, "");
        }
        if(cobwebKey==null || cobwebKey==InputUtil.UNKNOWN_KEY || cobwebKey.getCategory()!=InputUtil.Type.KEYSYM) {
            KeyBinding.updateKeysByCode();
            return;
        }
        for(int i=0;i<client.options.hotbarKeys.length;i++) {
            KeyBinding hotbar=client.options.hotbarKeys[i];
            if(KeyBindingHelper.getBoundKeyOf(hotbar).equals(cobwebKey)) {
                String prior=KeyBindingHelper.getBoundKeyOf(hotbar).getTranslationKey();
                CobwebConfig.setDisplacedHotbar(i,prior);
                hotbar.setBoundKey(InputUtil.UNKNOWN_KEY);
                break;
            }
        }
        KeyBinding.updateKeysByCode();
    }

    public static Text getActivationKeyText() { return key.getBoundKeyLocalizedText(); }
    public static boolean setActivationKey(InputUtil.Key value) { key.setBoundKey(value); MinecraftClient client=MinecraftClient.getInstance(); suppressConflictingHotbarBinding(client,value); client.options.write(); KeyBinding.updateKeysByCode(); return true; }
}










