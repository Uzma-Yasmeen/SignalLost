package com.signallost.ui;

import com.signallost.level.LevelCatalog;
import com.signallost.model.Score;
import com.signallost.progress.LevelResult;
import com.signallost.progress.PlayerProfile;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * The level select: one box per level, showing the stars earned on it.
 *
 * <p>The boxes are the point. A list of level names tells a player what exists;
 * a grid of boxes with two of three stars filled in tells them what is left to
 * do, which is the only reason to replay a level they have already beaten.
 *
 * <p>A level is locked until the player has enough stars across all levels, so
 * the stars are a currency rather than only a decoration.
 */
public final class LevelSelectPanel extends JPanel {

    private static final int MARGIN = 44;
    private static final int TOP = 128;
    private static final int BOX = 168;
    private static final int GAP = 22;
    /** At most this many boxes to a row; fewer when there are fewer levels. */
    private static final int MAX_COLUMNS = 4;
    private static final int FOOTER = 72;

    private static final Color BACKDROP_TOP = new Color(0x17203C);
    private static final Color BACKDROP_BOTTOM = new Color(0x0B1020);
    private static final Color BOX_FACE = new Color(0x1E2944);
    private static final Color BOX_FACE_SELECTED = new Color(0x293758);
    private static final Color BOX_FACE_LOCKED = new Color(0x141B2C);

    private final List<LevelCatalog.Entry> levels;
    private final int columns;
    private final PlayerProfile profile;
    private final Consumer<LevelCatalog.Entry> onChosen;

    private int selected;
    private int hovered = -1;

    public LevelSelectPanel(List<LevelCatalog.Entry> levels, PlayerProfile profile,
                            Consumer<LevelCatalog.Entry> onChosen) {
        this.levels = levels;
        this.profile = profile;
        this.onChosen = onChosen;
        // Two levels should not leave two empty columns of sky.
        this.columns = Math.max(1, Math.min(MAX_COLUMNS, levels.size()));

        int rows = Math.max(1, (levels.size() + columns - 1) / columns);
        setPreferredSize(new Dimension(
                Math.max(620, MARGIN * 2 + BOX * columns + GAP * (columns - 1)),
                TOP + rows * BOX + (rows - 1) * GAP + FOOTER));
        setBackground(BACKDROP_BOTTOM);
        installMouse();
    }

    // -------------------------------------------------------------- choosing

    public void moveSelection(int columnStep, int rowStep) {
        int next = selected + columnStep + rowStep * columns;
        if (next >= 0 && next < levels.size()) {
            selected = next;
            repaint();
        }
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < levels.size()) {
            selected = index;
            repaint();
        }
    }

    /** Opens the highlighted level, unless it is still locked. */
    public void confirmSelection() {
        LevelCatalog.Entry entry = levels.get(selected);
        if (isUnlocked(entry)) {
            onChosen.accept(entry);
        }
    }

    private boolean isUnlocked(LevelCatalog.Entry entry) {
        return profile.totalStars() >= entry.starsToUnlock();
    }

    private void installMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int under = boxAt(event.getX(), event.getY());
                if (under != hovered) {
                    hovered = under;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                int under = boxAt(event.getX(), event.getY());
                if (under >= 0) {
                    selected = under;
                    repaint();
                    confirmSelection();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private int boxAt(int x, int y) {
        for (int i = 0; i < levels.size(); i++) {
            if (boxBounds(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private Rectangle boxBounds(int index) {
        int column = index % columns;
        int row = index / columns;
        return new Rectangle(
                MARGIN + column * (BOX + GAP),
                TOP + row * (BOX + GAP),
                BOX, BOX);
    }

    // -------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, BACKDROP_TOP, 0, getHeight(), BACKDROP_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(null);

        drawHeading(g);
        for (int i = 0; i < levels.size(); i++) {
            drawBox(g, i);
        }
        drawFooter(g);
        g.dispose();
    }

    private void drawHeading(Graphics2D g) {
        g.setColor(Palette.ACCENT);
        g.setFont(Fonts.bodyBold(12f));
        g.drawString(profile.name().toUpperCase(), MARGIN, 46);

        g.setColor(Palette.TEXT);
        g.setFont(Fonts.display(30f));
        g.drawString("Choose a level", MARGIN, 84);

        int earned = profile.totalStars();
        int available = levels.size() * Score.MAX_STARS;
        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(14f));
        g.drawString(earned + " of " + available + " stars", MARGIN, 108);

        // The running total, top right, where a score belongs.
        String tally = earned + "/" + available;
        g.setFont(Fonts.display(22f));
        FontMetrics metrics = g.getFontMetrics();
        int x = getWidth() - MARGIN - metrics.stringWidth(tally);
        g.setColor(Palette.KEY);
        g.drawString(tally, x, 58);
        ObjectSprite.paintStar(g, x - 26, 50, 30, true);
    }

    private void drawBox(Graphics2D g, int index) {
        LevelCatalog.Entry entry = levels.get(index);
        Rectangle bounds = boxBounds(index);
        boolean unlocked = isUnlocked(entry);
        boolean isSelected = index == selected;
        boolean isHovered = index == hovered;

        RoundRectangle2D box = new RoundRectangle2D.Double(
                bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

        if (isSelected && unlocked) {
            g.setColor(Palette.withAlpha(Palette.ACCENT, 34));
            g.fill(new RoundRectangle2D.Double(
                    bounds.x - 5, bounds.y - 5, bounds.width + 10, bounds.height + 10, 22, 22));
        }

        g.setColor(unlocked ? (isSelected ? BOX_FACE_SELECTED : BOX_FACE) : BOX_FACE_LOCKED);
        g.fill(box);
        g.setColor(!unlocked
                ? Palette.withAlpha(Palette.TEXT, 18)
                : isSelected ? Palette.ACCENT : Palette.withAlpha(Palette.TEXT, isHovered ? 70 : 26));
        g.setStroke(new BasicStroke(isSelected && unlocked ? 2.2f : 1.2f));
        g.draw(box);

        if (unlocked) {
            drawStars(g, bounds, profile.starsOn(entry.name()));
            drawNumber(g, bounds, entry.shortLabel(), isSelected);
            drawBest(g, bounds, profile.bestOn(entry.name()));
        } else {
            drawLock(g, bounds, entry.starsToUnlock());
        }
    }

    private void drawStars(Graphics2D g, Rectangle bounds, int earned) {
        double size = 30;
        double gap = 6;
        double total = Score.MAX_STARS * size + (Score.MAX_STARS - 1) * gap;
        double x = bounds.getCenterX() - total / 2 + size / 2;
        for (int i = 0; i < Score.MAX_STARS; i++) {
            ObjectSprite.paintStar(g, x + i * (size + gap), bounds.y + 32, size, i < earned);
        }
    }

    private void drawNumber(Graphics2D g, Rectangle bounds, String label, boolean isSelected) {
        g.setFont(Fonts.display(46f));
        g.setColor(isSelected ? Palette.TEXT : Palette.withAlpha(Palette.TEXT, 190));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(label,
                bounds.x + (bounds.width - metrics.stringWidth(label)) / 2,
                bounds.y + 108);
    }

    private void drawBest(Graphics2D g, Rectangle bounds, LevelResult best) {
        String line = best == null
                ? "not yet escaped"
                : best.turns() + " turns  ·  " + (best.sightings() == 0
                        ? "unseen" : "seen " + best.sightings());
        g.setFont(Fonts.body(11f));
        g.setColor(Palette.TEXT_DIM);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(line,
                bounds.x + (bounds.width - metrics.stringWidth(line)) / 2,
                bounds.y + bounds.height - 22);
    }

    private void drawLock(Graphics2D g, Rectangle bounds, int starsNeeded) {
        g.setFont(Fonts.display(34f));
        g.setColor(Palette.withAlpha(Palette.TEXT, 90));
        String label = String.valueOf(starsNeeded);
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(label);

        double starSize = 30;
        double totalWidth = textWidth + 8 + starSize;
        double x = bounds.getCenterX() - totalWidth / 2;

        g.drawString(label, (int) x, bounds.y + bounds.height / 2 + 10);
        ObjectSprite.paintStar(g, x + textWidth + 8 + starSize / 2,
                bounds.y + bounds.height / 2.0, starSize, false);

        g.setFont(Fonts.body(11f));
        g.setColor(Palette.TEXT_DIM);
        String hint = "stars to unlock";
        g.drawString(hint,
                bounds.x + (bounds.width - g.getFontMetrics().stringWidth(hint)) / 2,
                bounds.y + bounds.height - 28);
    }

    private void drawFooter(Graphics2D g) {
        String hint = "Arrow keys to browse  ·  Enter or click to play  ·  N to change name";
        g.setFont(Fonts.body(13f));
        g.setColor(Palette.TEXT_DIM);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(hint, (getWidth() - metrics.stringWidth(hint)) / 2,
                getHeight() - FOOTER / 2 - 2);
    }
}
