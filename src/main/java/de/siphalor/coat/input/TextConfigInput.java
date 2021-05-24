package de.siphalor.coat.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class TextConfigInput extends TextFieldWidget implements ConfigInput<String> {
	public TextConfigInput(Text placeholder) {
		super(MinecraftClient.getInstance().textRenderer, 0, 0, 10, 16, placeholder);
	}

	@Override
	public int getHeight() {
		return super.getHeight() + 4;
	}

	@Override
	public String getValue() {
		return getText();
	}

	@Override
	public void setValue(String value) {
		setText(value);
	}

	@Override
	public void setChangeListener(InputChangeListener<String> changeListener) {
		setChangedListener(changeListener);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
	}

	@Override
	public void render(MatrixStack matrices, int x, int y, int width, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
		this.x = x + 2;
		this.y = y + 2;
		this.width = width - 4;
		render(matrices, mouseX, mouseY, tickDelta);
	}
}
