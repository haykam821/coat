package de.siphalor.coat.screen;

import de.siphalor.coat.Coat;
import de.siphalor.coat.handler.Message;
import de.siphalor.coat.list.DynamicEntryListWidget;
import de.siphalor.coat.list.entry.MessageListEntry;
import de.siphalor.coat.util.CoatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.List;
import java.util.stream.Collectors;

public class MessagesScreen extends Screen {
	private final ConfigScreen parent;
	private final Runnable acceptRunnable;
	private final List<Message> messages;
	private MultilineText titleLines;
	private ButtonWidget acceptButton;
	private ButtonWidget abortButton;
	private DynamicEntryListWidget messagesList;

	public MessagesScreen(Text title, ConfigScreen parent, Runnable acceptRunnable, List<Message> messages) {
		super(title);
		this.parent = parent;
		this.acceptRunnable = acceptRunnable;
		this.messages = messages;
	}

	public ConfigScreen getParent() {
		return parent;
	}

	@Override
	protected void init() {
		super.init();

		abortButton = new ButtonWidget(0, 38, 100, 20,
				new TranslatableText(Coat.MOD_ID + ".action.abort"),
				button -> MinecraftClient.getInstance().openScreen(parent)
		);
		acceptButton = new ButtonWidget(0, 38, 100, 20,
				new TranslatableText(Coat.MOD_ID + ".action.accept_risk"),
				button -> acceptRunnable.run()
		);
		addDrawableChild(abortButton);
		addDrawableChild(acceptButton);

		messagesList = new DynamicEntryListWidget(MinecraftClient.getInstance(), width, height - 62, 62, 260);
		messagesList.addEntries(messages.stream().map(MessageListEntry::new).collect(Collectors.toList()));
		addDrawableChild(messagesList);

		resize(MinecraftClient.getInstance(), width, height);
	}

	@Override
	public void resize(MinecraftClient client, int width, int height) {
		this.width = width;
		this.height = height;

		messagesList.resize(width, height);
		titleLines = MultilineText.create(client.textRenderer, title, 260);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		int left = width / 2 - 130;
		renderBackground(matrices);
		abortButton.x  = width / 2 - CoatUtil.MARGIN - abortButton.getWidth();
		acceptButton.x = width / 2 + CoatUtil.MARGIN;
		titleLines.draw(matrices, left, CoatUtil.DOUBLE_MARGIN, 10, CoatUtil.TEXT_COLOR);
		// messagesList.render(matrices, mouseX, mouseY, delta);

		super.render(matrices, mouseX, mouseY, delta);
	}
}
