package com.commandert3706.client.gui.screen;

import com.commandert3706.BetterLitematicaMaterialsList;
import com.commandert3706.client.BetterLitematicaMaterialsListClient;
import com.commandert3706.client.logic.MaterialTrackerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public class LitematicSelectionScreen extends Screen {
    private FileListWidget fileList;
    private Button selectButton;
    private File selectedFile;

    public LitematicSelectionScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.fileList = new FileListWidget(this.minecraft, this.width, this.height - 64, 32, 20);
        this.addWidget(this.fileList);

        this.selectButton = this.addRenderableWidget(
                Button.builder(Component.literal("Load Materials List"), (button) -> {
                    if (this.selectedFile != null) {
                        MaterialTrackerManager.loadSchematicAsync(selectedFile);
                        this.minecraft.setScreen(null);
                    }
                }).bounds(this.width / 2 - 122, this.height - 26, 120, 20).build()
        );

        this.selectButton.active = false;

        this.addRenderableWidget(
                Button.builder(Component.literal("Cancel"), (button) -> this.minecraft.setScreen(null))
                        .bounds(this.width / 2 + 2, this.height - 26, 120, 20)
                        .build()
        );

        this.loadSchematics();
    }

    private void loadSchematics() {
        File schematicDir = new File(this.minecraft.gameDirectory, "schematics");
        if (schematicDir.exists() && schematicDir.isDirectory()) {
            scanFolderRecursive(schematicDir, "");
        }
    }

    private void scanFolderRecursive(File currentFolder, String relativePath) {
        File[] files = currentFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newPath = relativePath.isEmpty() ? file.getName() : relativePath + File.separator + file.getName();
                scanFolderRecursive(file, newPath);
            } else if (file.getName().endsWith(".litematic")) {
                this.fileList.addFile(new FileEntry(file, relativePath));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        this.fileList.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, (this.width / 2), 16, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (BetterLitematicaMaterialsListClient.selectSchematicKey.matches(event)) {
            this.minecraft.setScreen(null);
            return true;
        }

        return super.keyPressed(event);
    }

    class FileListWidget extends ObjectSelectionList<FileEntry> {
        public FileListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public void setSelected(FileEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                LitematicSelectionScreen.this.selectedFile = entry.file;
                LitematicSelectionScreen.this.selectButton.active = true;
            }
        }

        public void addFile(FileEntry entry) {
            this.addEntry(entry);
        }
    }

    class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
        public final File file;
        private final String displayName;

        public FileEntry(File file, String relativePath) {
            this.file = file;

            if (relativePath.isEmpty()) {
                this.displayName = file.getName();
            } else {
                this.displayName = relativePath.replace(File.separator, "/") + "/" + file.getName();
            }
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.file.getName());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.text(LitematicSelectionScreen.this.font, this.displayName, this.getX() + 4, this.getY() + 4, 0xFFFFFFFF, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return super.mouseClicked(event, doubleClick);
        }
    }
}
