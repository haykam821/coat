package de.siphalor.coat.list;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.siphalor.coat.Coat;
import de.siphalor.coat.handler.Message;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.TickableElement;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This is mostly a copy of {@link net.minecraft.client.gui.widget.EntryListWidget} to enable variable item heights.
 */
@Environment(EnvType.CLIENT)
public class ConfigEntryListWidget extends ConfigListCompoundEntry implements Drawable, TickableElement {
	protected final MinecraftClient client;
	private final List<ConfigListEntry> children = new Entries();
	private final IntList entryBottoms = new IntArrayList();
	private final int rowWidth;
	protected int width;
	protected int height;
	protected int top;
	protected int bottom;
	protected int right;
	protected int left;
	private double scrollAmount;
	private boolean renderSelection = true;
	private boolean renderBackground;
	private Identifier background = DrawableHelper.OPTIONS_BACKGROUND_TEXTURE;
	private boolean scrolling;
	private ConfigListEntry selected;

	public ConfigEntryListWidget(MinecraftClient client, int width, int height, int top, int bottom, int rowWidth) {
		this.client = client;
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		this.left = 0;
		this.right = width;
		this.rowWidth = rowWidth;
	}

	public void setRenderSelection(boolean renderSelection) {
		this.renderSelection = renderSelection;
	}

	public Identifier getBackground() {
		return background;
	}

	public void setBackground(Identifier background) {
		this.background = background;
	}

	public void setRenderBackground(boolean renderBackground) {
		this.renderBackground = renderBackground;
	}

	public int getEntryWidth() {
		return Math.min(rowWidth, width);
	}

	@Nullable
	public ConfigListEntry getSelected() {
		return this.selected;
	}

	public void setSelected(@Nullable ConfigListEntry entry) {
		this.selected = entry;
	}

	@Nullable
	public ConfigListEntry getFocused() {
		return (ConfigListEntry) super.getFocused();
	}

	public final List<ConfigListEntry> children() {
		return this.children;
	}

	protected final void clearEntries() {
		children.clear();
		entryBottoms.clear();
	}

	protected void replaceEntries(Collection<ConfigListEntry> newEntries) {
		clearEntries();
		addEntries(newEntries);
	}

	protected ConfigListEntry getEntry(int index) {
		return this.children().get(index);
	}

	public int addEntry(ConfigListEntry entry) {
		children.add(entry);
		entryBottoms.add(getMaxEntryPosition() + entry.getHeight());
		entry.setParentList(this);
		return children.size() - 1;
	}

	public void addEntries(Collection<ConfigListEntry> newEntries) {
		int oldSize = newEntries.size();
		int bottom = entryBottoms.size() == 0 ? 0 : entryBottoms.getInt(0);
		children.addAll(newEntries);
		for (int i = oldSize, l = children.size(); i < l; i++) {
			bottom += children.get(i).getHeight();
			entryBottoms.add(bottom);
		}
	}

	protected int getEntryCount() {
		return this.children().size();
	}

	protected boolean isSelectedEntry(int index) {
		return Objects.equals(this.getSelected(), this.children().get(index));
	}

	@Nullable
	protected final ConfigListEntry getEntryAtPosition(double x, double y) {
		int halfRowWidth = this.getEntryWidth() / 2;
		int screenCenter = this.left + this.width / 2;
		int rowLeft = screenCenter - halfRowWidth;
		int rowRight = screenCenter + halfRowWidth;
		if (x >= getScrollbarPositionX() || x < rowLeft || x > rowRight) {
			return null;
		}
		y -= getEntryAreaTop();
		if (y < 0 || y > getMaxEntryPosition()) {
			return null;
		}
		IntListIterator iterator = entryBottoms.iterator();
		while (iterator.hasNext()) {
			if (y < iterator.nextInt()) {
				return getEntry(iterator.nextIndex() - 1);
			}
		}
		return null;
	}

	public void entryHeightChanged(ConfigListEntry entry) {
		int index = children.indexOf(entry);
		int bottom = index == 0 ? 0 : entryBottoms.getInt(index - 1);
		for (int i = index, l = children.size(); i < l; i++) {
			bottom += children.get(i).getHeight();
			entryBottoms.set(i, bottom);
		}
	}

	@Override
	public void widthChanged(int newWidth) {
		super.widthChanged(newWidth);
		width = newWidth;
		right = left + newWidth;

		for (ConfigListEntry entry : children) {
			entry.widthChanged(getEntryWidth());
		}
	}

	public void setLeftPos(int left) {
		this.left = left;
		this.right = left + this.width;
	}

	protected int getMaxEntryPosition() {
		if (entryBottoms.isEmpty()) {
			return 0;
		}
		return entryBottoms.getInt(entryBottoms.size() - 1);
	}

	protected int getMaxPosition() {
		return getMaxEntryPosition();
	}

	protected void renderBackground(Tessellator tessellator, BufferBuilder bufferBuilder) {
		this.client.getTextureManager().bindTexture(background);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		bufferBuilder.vertex(left,  bottom, 0D).color(0x44, 0x44, 0x44, 0xff).texture(left / 32F,  (bottom + (int)getScrollAmount()) / 32F).next();
		bufferBuilder.vertex(right, bottom, 0D).color(0x44, 0x44, 0x44, 0xff).texture(right / 32F, (bottom + (int)getScrollAmount()) / 32F).next();
		bufferBuilder.vertex(right, top,    0D).color(0x44, 0x44, 0x44, 0xff).texture(right / 32F, (top    + (int)getScrollAmount()) / 32F).next();
		bufferBuilder.vertex(left,  top,    0D).color(0x44, 0x44, 0x44, 0xff).texture(left / 32F,  (top    + (int)getScrollAmount()) / 32F).next();
		tessellator.draw();
	}

	protected void renderShadows(Tessellator tessellator, BufferBuilder bufferBuilder) {
		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(left,  top + 4,    0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(right, top + 4,    0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(right, top,        0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(left,  top,        0D).color(0, 0, 0, 255).next();

		bufferBuilder.vertex(left,  bottom,     0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(right, bottom,     0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(right, bottom - 4, 0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(left,  bottom - 4, 0D).color(0, 0, 0, 0).next();
		tessellator.draw();

	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		int scrollbarXBegin = this.getScrollbarPositionX();
		int scrollbarXEnd = scrollbarXBegin + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();

		if (renderBackground) {
			renderBackground(tessellator, bufferBuilder);
		}

		int k = this.getEntryLeft();
		int l = this.top + 4 - (int)this.getScrollAmount();

		this.renderList(matrices, k, l, mouseX, mouseY, delta);

		renderShadows(tessellator, bufferBuilder);

		int maxScroll = this.getMaxScroll();
		if (maxScroll > 0) {
			RenderSystem.disableTexture();
			int p = (int)((float)((this.bottom - this.top) * (this.bottom - this.top)) / (float)this.getMaxPosition());
			p = MathHelper.clamp(p, 32, this.bottom - this.top - 8);
			int q = (int)this.getScrollAmount() * (this.bottom - this.top - p) / maxScroll + this.top;
			if (q < this.top) {
				q = this.top;
			}

			bufferBuilder.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
			bufferBuilder.vertex(scrollbarXBegin,   bottom,    0D).color(0, 0, 0, 255).texture(0F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd,     bottom,    0D).color(0, 0, 0, 255).texture(1F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd,     top,       0D).color(0, 0, 0, 255).texture(1F, 0F).next();
			bufferBuilder.vertex(scrollbarXBegin,   top,       0D).color(0, 0, 0, 255).texture(0F, 0F).next();

			bufferBuilder.vertex(scrollbarXBegin,   q + p,     0D).color(128, 128, 128, 255).texture(0F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd,     q + p,     0D).color(128, 128, 128, 255).texture(1F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd,     q,         0D).color(128, 128, 128, 255).texture(1F, 0F).next();
			bufferBuilder.vertex(scrollbarXBegin,   q,         0D).color(128, 128, 128, 255).texture(0F, 0F).next();

			bufferBuilder.vertex(scrollbarXBegin,   q + p - 1, 0D).color(192, 192, 192, 255).texture(0F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd - 1, q + p - 1, 0D).color(192, 192, 192, 255).texture(1F, 1F).next();
			bufferBuilder.vertex(scrollbarXEnd - 1, q,         0D).color(192, 192, 192, 255).texture(1F, 0F).next();
			bufferBuilder.vertex(scrollbarXBegin,   q,         0D).color(192, 192, 192, 255).texture(0F, 0F).next();
			tessellator.draw();
		}

		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}

	protected void centerScrollOn(ConfigListEntry entry) {
		int index = children.indexOf(entry);
		setScrollAmount(entryBottoms.getInt(index) - entry.getHeight() / 2D - (bottom - top) / 2D);
	}

	protected void ensureVisible(ConfigListEntry entry) {
		int index = children.indexOf(entry);
		int bottom = entryBottoms.getInt(index);
		if (scrollAmount + height > bottom) {
			setScrollAmount(bottom - height);
		}

		int top = index == 0 ? 0 : entryBottoms.getInt(index - 1);

		if (scrollAmount > top) {
			setScrollAmount(top);
		}
	}

	private void scroll(int amount) {
		this.setScrollAmount(this.getScrollAmount() + amount);
	}

	public double getScrollAmount() {
		return this.scrollAmount;
	}

	public void setScrollAmount(double amount) {
		this.scrollAmount = MathHelper.clamp(amount, 0.0D, this.getMaxScroll());
	}

	public int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.bottom - this.top) + 4 + Coat.DOUBLE_MARGIN);
	}

	protected void updateScrollingState(double mouseX, double mouseY, int button) {
		this.scrolling = button == 0 && mouseX >= (double)this.getScrollbarPositionX() && mouseX < (double)(this.getScrollbarPositionX() + 6);
	}

	protected int getScrollbarPositionX() {
		return left + width / 2 + getEntryWidth() / 2 + Coat.MARGIN;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.updateScrollingState(mouseX, mouseY, button);
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		} else {
			ConfigListEntry entry = getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				if (entry.mouseClicked(mouseX, mouseY, button)) {
					setFocused(entry);
					setDragging(true);
					return true;
				}
			}

			return scrolling;
		}
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.getFocused() != null) {
			this.getFocused().mouseReleased(mouseX, mouseY, button);
		}

		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		} else if (button == 0 && this.scrolling) {
			if (mouseY < (double)this.top) {
				this.setScrollAmount(0.0D);
			} else if (mouseY > (double)this.bottom) {
				this.setScrollAmount(this.getMaxScroll());
			} else {
				double d = Math.max(1, this.getMaxScroll());
				int i = this.bottom - this.top;
				int j = MathHelper.clamp((int)((float)(i * i) / (float)this.getMaxPosition()), 32, i - 8);
				double e = Math.max(1.0D, d / (double)(i - j));
				this.setScrollAmount(this.getScrollAmount() + deltaY * e);
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		this.setScrollAmount(this.getScrollAmount() - amount * 10.0D);
		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (keyCode == 264) {
			this.moveSelection(MoveDirection.DOWN);
			return true;
		} else if (keyCode == 265) {
			this.moveSelection(MoveDirection.UP);
			return true;
		} else {
			return false;
		}
	}


	protected void moveSelection(MoveDirection direction) {
		this.moveSelectionIf(direction, (entry) -> true);
	}

	protected void ensureSelectedEntryVisible() {
		ConfigListEntry entry = this.getSelected();
		if (entry != null) {
			this.setSelected(entry);
			this.ensureVisible(entry);
		}
	}

	protected void moveSelectionIf(MoveDirection direction, Predicate<ConfigListEntry> predicate) {
		int offset = direction == MoveDirection.UP ? -1 : 1;
		if (!this.children().isEmpty()) {
			int index = this.children().indexOf(this.getSelected());

			while(true) {
				int newIndex = MathHelper.clamp(index + offset, 0, this.getEntryCount() - 1);
				if (index == newIndex) {
					break;
				}

				ConfigListEntry entry = this.children().get(newIndex);
				if (predicate.test(entry)) {
					this.setSelected(entry);
					this.ensureVisible(entry);
					break;
				}

				index = newIndex;
			}
		}
	}

	@Override
	public void render(MatrixStack matrices, int x, int y, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
		this.left = x;
		this.top = y;
		render(matrices, mouseX, mouseY, tickDelta);
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseY >= (double)this.top && mouseY <= (double)this.bottom && mouseX >= (double)this.left && mouseX <= (double)this.right;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public Collection<Message> getMessages() {
		return children.stream().flatMap(entry -> entry.getMessages().stream()).collect(Collectors.toList());
	}

	protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
		IntListIterator bottomIter = entryBottoms.iterator();
		Iterator<ConfigListEntry> entryIter = children.iterator();
		int relBottom = 0, relTop = 0;
		ConfigListEntry entry = null;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();

		while (bottomIter.hasNext()) {
			relTop = relBottom;
			relBottom = bottomIter.nextInt();
			entry = entryIter.next();
			if (relBottom > scrollAmount) {
				break;
			}
		}

		ConfigListEntry hoveredEntry = getEntryAtPosition(mouseX, mouseY);

		int rowWidth = getEntryWidth();
		int rowLeft = this.left + this.width / 2 - rowWidth / 2;
		int rowRight = this.left + this.width / 2 + rowWidth / 2;
		while(true) {
			if (entry == null) {
				break;
			}

			int rowTop = relTop + getEntryAreaTop();
			if (this.renderSelection && getSelected() == entry) {
				int selectionHeight = (relTop - relBottom) - 4;
				RenderSystem.disableTexture();
				float f = this.isFocused() ? 1.0F : 0.5F;
				RenderSystem.color4f(f, f, f, 1.0F);
				bufferBuilder.begin(7, VertexFormats.POSITION);
				bufferBuilder.vertex(rowLeft,  rowTop + selectionHeight + 2, 0.0D).next();
				bufferBuilder.vertex(rowRight, rowTop + selectionHeight + 2, 0.0D).next();
				bufferBuilder.vertex(rowRight, rowTop - 2,                   0.0D).next();
				bufferBuilder.vertex(rowLeft,  rowTop - 2,                   0.0D).next();
				tessellator.draw();
				RenderSystem.color4f(0.0F, 0.0F, 0.0F, 1.0F);
				bufferBuilder.begin(7, VertexFormats.POSITION);
				bufferBuilder.vertex(rowLeft + 1,  rowTop + selectionHeight + 1, 0.0D).next();
				bufferBuilder.vertex(rowRight - 1, rowTop + selectionHeight + 1, 0.0D).next();
				bufferBuilder.vertex(rowRight - 1, rowTop - 1,                   0.0D).next();
				bufferBuilder.vertex(rowLeft + 1,  rowTop - 1,                   0.0D).next();
				tessellator.draw();
				RenderSystem.enableTexture();
			}

			entry.render(matrices, rowLeft, rowTop, rowWidth, relBottom - relTop, mouseX, mouseY, hoveredEntry == entry, delta);

			if (bottomIter.hasNext()) {
				relTop = relBottom;
				relBottom = bottomIter.nextInt();
				entry = entryIter.next();
			} else {
				break;
			}
		}
	}

	public int getEntryLeft() {
		return this.left + this.width / 2 - this.getEntryWidth() / 2 + 2;
	}

	public int getEntryRight() {
		return this.getEntryLeft() + this.getEntryWidth();
	}

	protected int getEntryAreaTop() {
		return top + 4 - (int) scrollAmount;
	}

	protected int getEntryTop(int index) {
		if (index == 0) {
			return getEntryAreaTop();
		}
		return getEntryAreaTop() + entryBottoms.getInt(index - 1);
	}

	private int getEntryBottom(int index) {
		return getEntryAreaTop() + entryBottoms.getInt(index);
	}

	protected boolean isFocused() {
		return false;
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		ConfigListEntry old = getFocused();
		if (old != null && old != focused) {
			old.focusLost();
		}
		super.setFocused(focused);
	}

	protected ConfigListEntry removeEntry(int index) {
		ConfigListEntry entry = this.children.get(index);
		return this.removeEntry(index, entry) ? entry : null;
	}

	protected boolean removeEntry(ConfigListEntry entry) {
		return removeEntry(children.indexOf(entry), entry);
	}

	protected boolean removeEntry(int index, ConfigListEntry entry) {
		boolean success = this.children.remove(entry);
		if (success) {
			entryBottoms.removeInt(index);
		}
		if (success && entry == this.getSelected()) {
			this.setSelected(null);
		}

		return success;
	}

	@Override
	public void tick() {
		for (ConfigListEntry child : children()) {
			child.tick();
		}
	}

	@Environment(EnvType.CLIENT)
	class Entries extends AbstractList<ConfigListEntry> {
		private final List<ConfigListEntry> entries;

		private Entries() {
			this.entries = Lists.newArrayList();
		}

		public ConfigListEntry get(int i) {
			return this.entries.get(i);
		}

		public int size() {
			return this.entries.size();
		}

		public ConfigListEntry set(int i, ConfigListEntry entry) {
			ConfigListEntry entry2 = this.entries.set(i, entry);
			entry.setParentList(ConfigEntryListWidget.this);
			return entry2;
		}

		public void add(int i, ConfigListEntry entry) {
			this.entries.add(i, entry);
			entry.setParentList(ConfigEntryListWidget.this);
		}

		public ConfigListEntry remove(int i) {
			return this.entries.remove(i);
		}
	}

	@Environment(EnvType.CLIENT)
	public enum MoveDirection {
		UP, DOWN
	}
}
