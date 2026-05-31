package com.commandert3706.client.gui.screen;

import com.commandert3706.client.BetterLitematicaMaterialsListClient;
import com.commandert3706.client.logic.MaterialTrackerManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MaterialListScreen extends Screen {
    private MaterialTableList materialList;

    public MaterialListScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.materialList = new MaterialTableList(this.minecraft, this.width, this.height - 88, 40, 24);
        this.addRenderableWidget(this.materialList);

        Button sortButton = Button.builder(Component.literal(MaterialTrackerManager.getSortPreferenceCaption()), (btn) -> {
            MaterialTrackerManager.SortPreference next = switch (MaterialTrackerManager.getSortPreference()) {
                case ALPHABETICAL -> MaterialTrackerManager.SortPreference.AMOUNT_DESC;
                case AMOUNT_DESC -> MaterialTrackerManager.SortPreference.AMOUNT_ASC;
                case AMOUNT_ASC -> MaterialTrackerManager.SortPreference.ALPHABETICAL;
            };

            MaterialTrackerManager.setSortPreference(next);
            this.minecraft.setScreen(this);
        }).bounds(4, this.height - 24, 120, 20).build();
        this.addRenderableWidget(sortButton);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), (btn) -> this.minecraft.setScreen(null))
                .bounds(this.width - 124, this.height - 24, 120, 20).build());

        Map<String, Integer> materials = MaterialTrackerManager.getActiveMaterials();
        List<MaterialRowEntry> sortingList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            sortingList.add(new MaterialRowEntry(entry.getKey(), entry.getValue()));
        }

        MaterialTrackerManager.SortPreference currentPreference = MaterialTrackerManager.getSortPreference();

        sortingList.sort((entry1, entry2) -> {
            switch (currentPreference) {
                case AMOUNT_DESC: return Integer.compare(entry2.getCount(), entry1.getCount());
                case AMOUNT_ASC: return Integer.compare(entry1.getCount(), entry2.getCount());
                case ALPHABETICAL:
                default:
                    String n1 = I18n.get(entry1.getTranslationKey());
                    String n2 = I18n.get(entry2.getTranslationKey());
                    return n1.compareToIgnoreCase(n2);
            }
        });

        boolean dark = false;
        for (MaterialRowEntry sortedRow : sortingList) {
            sortedRow.setDark(dark);
            this.materialList.addTableRow(sortedRow);
            dark = !dark;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (BetterLitematicaMaterialsListClient.openMaterialListKey.matches(event)) {
            this.minecraft.setScreen(null);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.text(this.font, Component.literal("Name"), 4, 24, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.literal("Amount"), width / 2, 24, 0xFFFFFFFF, true);
    }

    class MaterialTableList extends ObjectSelectionList<MaterialRowEntry> {
        public MaterialTableList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addTableRow(MaterialRowEntry entry) {
            this.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int scrollBarX() {
            return this.width - 6;
        }
    }

    class MaterialRowEntry extends ObjectSelectionList.Entry<MaterialRowEntry> {
        private final Identifier materialId;
        private final String translationKey;
        private final Component displayName;
        private final int totalCount;
        private final String totalCountFormatted;
        private final Checkbox checkbox;
        private boolean dark = false;

        public MaterialRowEntry(String itemId, int count) {
            this.materialId = Identifier.parse(itemId);
            this.totalCount = count;
            this.totalCountFormatted = MaterialTrackerManager.formatCount(count);

            String translationKey;
            if (BuiltInRegistries.ITEM.containsKey(materialId)) {
                translationKey = BuiltInRegistries.ITEM.getOptional(materialId).orElse(Items.AIR).getDescriptionId();
            } else if (BuiltInRegistries.BLOCK.containsKey(materialId)) {
                translationKey = BuiltInRegistries.BLOCK.getOptional(materialId).orElse(Blocks.AIR).getDescriptionId();
            } else {
                translationKey = "item." + itemId.replace(":", ".");
            }
            this.displayName = Component.translatable(translationKey);
            this.translationKey = translationKey;

            boolean initiallyChecked = MaterialTrackerManager.isItemChecked(this.materialId.toString());
            this.checkbox = Checkbox.builder(Component.empty(), MaterialListScreen.this.font).pos(0, 0).maxWidth(12).selected(initiallyChecked).build();
        }

        @Override
        public Component getNarration() {
            return this.displayName;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.checkbox.setX(this.getX() + 3);
            this.checkbox.setY(this.getY() + 3);

            int color = this.checkbox.selected() ? 0xFF55FF55 : 0xFFFFFFFF;

            graphics.fill(this.getX(), this.getY(), this.getX() + getWidth() - 6, this.getY() + getHeight(), dark ? 0x44AAAAAA : 0x44555555);
            this.checkbox.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.fakeItem(BuiltInRegistries.ITEM.getOptional(materialId).orElse(Items.BARRIER).getDefaultInstance(), this.getX() + 26, this.getY() + 4);
            graphics.text(MaterialListScreen.this.font, displayName, this.getX() + 48, this.getY() + 8, color, true);
            graphics.text(MaterialListScreen.this.font, totalCountFormatted, this.getWidth() / 2, this.getY() + 8, color, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                if (this.checkbox.mouseClicked(event, doubleClick)) {
                    MaterialTrackerManager.setItemCheckedState(this.materialId.toString(), this.checkbox.selected());
                }
            }
            return true;
        }

        public int getCount() {
            return this.totalCount;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public void setDark(boolean val) {
            this.dark = val;
        }
    }
}
