package com.signallost.ui;

import javax.swing.JPanel;
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
import java.util.function.Consumer;

/**
 * The screen shown before a run: six animals laid out as cards, one of which is
 * highlighted, picked with the keyboard or the mouse.
 *
 * <p>It is deliberately the bright screen in a dark game. Once the level starts
 * the palette goes cold again, which makes the facility feel like somewhere the
 * character does not belong.
 */
public final class CharacterSelectPanel extends JPanel {

    private static final int MARGIN = 44;
    private static final int TOP = 128;
    private static final int CARD_W = 250;
    private static final int CARD_H = 196;
    private static final int GAP = 22;
    private static final int COLUMNS = 3;
    private static final int FOOTER = 70;

    private static final Color BACKDROP_TOP = new Color(0x17203C);
    private static final Color BACKDROP_BOTTOM = new Color(0x0B1020);
    private static final Color CARD_FACE = new Color(0x1E2944);
    private static final Color CARD_FACE_SELECTED = new Color(0x293758);

    private final Critter[] roster = Critter.values();
    private final Consumer<Critter> onChosen;

    private int selected;
    private int hovered = -1;

    public CharacterSelectPanel(Critter initial, Consumer<Critter> onChosen) {
        this.onChosen = onChosen;
        this.selected = indexOf(initial);

        int rows = (roster.length + COLUMNS - 1) / COLUMNS;
        setPreferredSize(new Dimension(
                MARGIN * 2 + CARD_W * COLUMNS + GAP * (COLUMNS - 1),
                TOP + rows * CARD_H + (rows - 1) * GAP + FOOTER));
        setBackground(BACKDROP_BOTTOM);
        installMouse();
    }

    // ------------------------------------------------------------ selection

    public Critter selectedCritter() {
        return roster[selected];
    }

    /** Moves the highlight by whole cards; {@code rowStep} jumps a row at a time. */
    public void moveSelection(int columnStep, int rowStep) {
        int next = selected + columnStep + rowStep * COLUMNS;
        if (next >= 0 && next < roster.length) {
            selected = next;
            repaint();
        }
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < roster.length) {
            selected = index;
            repaint();
        }
    }

    public void confirmSelection() {
        onChosen.accept(selectedCritter());
    }

    private int indexOf(Critter critter) {
        for (int i = 0; i < roster.length; i++) {
            if (roster[i] == critter) {
                return i;
            }
        }
        return 0;
    }

    private void installMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int under = cardAt(event.getX(), event.getY());
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
                int under = cardAt(event.getX(), event.getY());
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

    private int cardAt(int x, int y) {
        for (int i = 0; i < roster.length; i++) {
            if (cardBounds(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private Rectangle cardBounds(int index) {
        int column = index % COLUMNS;
        int row = index / COLUMNS;
        return new Rectangle(
                MARGIN + column * (CARD_W + GAP),
                TOP + row * (CARD_H + GAP),
                CARD_W, CARD_H);
    }

    // -------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, BACKDROP_TOP, 0, getHeight(), BACKDROP_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(null);

        drawHeading(g);
        for (int i = 0; i < roster.length; i++) {
            drawCard(g, i);
        }
        drawFooter(g);

        g.dispose();
    }

    private void drawHeading(Graphics2D g) {
        g.setColor(Palette.ACCENT);
        g.setFont(Fonts.bodyBold(12f));
        g.drawString("SIGNAL LOST", MARGIN, 46);

        g.setColor(Palette.TEXT);
        g.setFont(Fonts.display(30f));
        g.drawString("Pick your escape artist", MARGIN, 84);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(14f));
        g.drawString("They all move the same. The facility will not be charmed.", MARGIN, 108);
    }

    private void drawCard(Graphics2D g, int index) {
        Critter critter = roster[index];
        Rectangle bounds = cardBounds(index);
        boolean isSelected = index == selected;
        boolean isHovered = index == hovered;

        RoundRectangle2D card = new RoundRectangle2D.Double(
                bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

        if (isSelected) {
            // A wash of the animal's own colour behind the selected card.
            g.setColor(Palette.withAlpha(critter.fur(), 38));
            g.fill(new RoundRectangle2D.Double(
                    bounds.x - 5, bounds.y - 5, bounds.width + 10, bounds.height + 10, 22, 22));
        }

        g.setColor(isSelected ? CARD_FACE_SELECTED : CARD_FACE);
        g.fill(card);

        g.setColor(isSelected
                ? critter.fur()
                : Palette.withAlpha(Palette.TEXT, isHovered ? 70 : 26));
        g.setStroke(new java.awt.BasicStroke(isSelected ? 2.2f : 1.2f));
        g.draw(card);

        double headY = bounds.y + bounds.height * 0.42;
        double headRadius = 38;
        if (isSelected) {
            paintSoftGlow(g, bounds.getCenterX(), headY, headRadius * 2.4, critter.fur(), 60);
        }
        CritterArt.paint(g, critter, bounds.getCenterX(), headY, headRadius);

        g.setFont(Fonts.display(19f));
        g.setColor(isSelected ? Palette.TEXT : Palette.withAlpha(Palette.TEXT, 200));
        centreString(g, critter.displayName(), bounds, bounds.y + bounds.height - 48);

        g.setFont(Fonts.body(12f));
        g.setColor(Palette.TEXT_DIM);
        centreString(g, critter.tagline(), bounds, bounds.y + bounds.height - 26);

        // The number key that jumps straight to this card.
        g.setFont(Fonts.mono(11f));
        g.setColor(Palette.withAlpha(Palette.TEXT, isSelected ? 150 : 70));
        g.drawString(String.valueOf(index + 1), bounds.x + 14, bounds.y + 24);
    }

    private void drawFooter(Graphics2D g) {
        int y = getHeight() - FOOTER / 2 - 2;

        g.setFont(Fonts.body(13f));
        g.setColor(Palette.TEXT_DIM);
        String hint = "Arrow keys or 1-6 to browse  ·  Enter or click to begin";
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(hint, (getWidth() - metrics.stringWidth(hint)) / 2, y);
    }

    private void centreString(Graphics2D g, String text, Rectangle bounds, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text,
                bounds.x + (bounds.width - metrics.stringWidth(text)) / 2,
                baselineY);
    }

    private void paintSoftGlow(Graphics2D g, double cx, double cy, double radius,
                               Color colour, int alpha) {
        java.awt.RadialGradientPaint paint = new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Double(cx, cy), (float) radius,
                new float[] {0f, 1f},
                new Color[] {Palette.withAlpha(colour, alpha), Palette.withAlpha(colour, 0)});
        g.setPaint(paint);
        g.fill(new java.awt.geom.Ellipse2D.Double(
                cx - radius, cy - radius, radius * 2, radius * 2));
        g.setPaint(null);
    }
}
