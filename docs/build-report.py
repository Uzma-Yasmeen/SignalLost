"""Turns docs/report.md into the formatted Word document the course requires.

The template specifies Times New Roman throughout: 16pt centred for main
headings, 14pt left-aligned for subheadings, 12pt justified for body text. That
is applied here rather than by hand, so the report can be regenerated whenever
the source changes without the formatting drifting.

    python docs/build-report.py [output.docx]

Diagrams must have been exported first — see render-diagrams.js and
diagram-server.py.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor

DOCS = Path(__file__).resolve().parent
REPORT = DOCS / "report.md"

FONT = "Times New Roman"
MAIN_PT = 16          # main headings, centred
SUB_PT = 14           # subheadings, left
BODY_PT = 12          # body, justified
TABLE_PT = 10.5       # tables need to fit the page
PAGE_WIDTH_IN = 6.27  # A4 less one-inch margins

# Title page, from the template. Each entry is (text, size, bold, space_after).
TITLE_PAGE = [
    ("SIGNAL LOST", 20, True, 4),
    ("A Fog-of-War Stealth Strategy Game", 14, False, 22),
    ("A Course Project Report", 13, False, 4),
    ("Submitted in the partial fulfillment of the completion of the course", 12, False, 10),
    ("SOFTWARE ENGINEERING AND SYSTEM DESIGN", 14, True, 2),
    ("(24CS301PC311)", 12, False, 10),
    ("In III Year, I Semester B. Tech-CSE", 12, False, 20),
    ("Submitted by", 12, False, 6),
    ("Uzma Yasmeen", 14, True, 2),
    ("(Hall Ticket Number)", 12, False, 20),
    ("Under the supervision of the faculty", 12, False, 6),
    ("Dr. <Faculty Name>", 13, True, 2),
    ("<Assistant or Associate Professor>,", 12, False, 2),
    ("School of Computer Science & Artificial Intelligence", 12, False, 24),
    ("2026-2027", 13, True, 24),
    ("Department of Computer Science & Artificial Intelligence", 13, True, 2),
    ("SR University", 13, True, 2),
    ("Warangal, Telangana, India", 12, False, 0),
]


def style_run(run, size=BODY_PT, bold=False, italic=False, mono=False):
    run.font.name = "Consolas" if mono else FONT
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    if mono:
        run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
    # East-Asian font name must be set separately or Word may substitute.
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:eastAsia"), "Consolas" if mono else FONT)


INLINE = re.compile(r"(\*\*.+?\*\*|(?<!\*)\*[^*]+?\*(?!\*)|`[^`]+`|\[[^\]]+\]\([^)]+\))")


def add_inline(paragraph, text, size=BODY_PT, bold=False):
    """Writes markdown inline formatting into a paragraph as real runs."""
    for piece in INLINE.split(text):
        if not piece:
            continue
        if piece.startswith("**") and piece.endswith("**"):
            style_run(paragraph.add_run(piece[2:-2]), size, bold=True)
        elif piece.startswith("`") and piece.endswith("`"):
            style_run(paragraph.add_run(piece[1:-1]), size - 0.5, bold=bold, mono=True)
        elif piece.startswith("[") and "](" in piece:
            label = piece[1:piece.index("](")]
            style_run(paragraph.add_run(label), size, bold=bold)
        elif piece.startswith("*") and piece.endswith("*"):
            style_run(paragraph.add_run(piece[1:-1]), size, bold=bold, italic=True)
        else:
            style_run(paragraph.add_run(piece), size, bold=bold)


def body(doc, text, size=BODY_PT, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
         space_after=8, indent=0.0, italic_all=False):
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.15
    if indent:
        p.paragraph_format.left_indent = Inches(indent)
    if italic_all:
        style_run(p.add_run(re.sub(r"[*`]", "", text)), size, italic=True)
    else:
        add_inline(p, text, size)
    return p


def heading(doc, text, level):
    p = doc.add_paragraph()
    if level == 2:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        size, before, after = MAIN_PT, 18, 12
    else:
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        size, before, after = SUB_PT, 14, 8
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.keep_with_next = True
    style_run(p.add_run(text), size, bold=True)
    return p


def shade(cell, colour="E8E8E8"):
    tc = cell._tc.get_or_add_tcPr()
    el = OxmlElement("w:shd")
    el.set(qn("w:val"), "clear")          # never "solid": it renders black
    el.set(qn("w:fill"), colour)
    tc.append(el)


def add_table(doc, rows):
    header, body_rows = rows[0], rows[1:]
    table = doc.add_table(rows=len(rows), cols=len(header))
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for r, row in enumerate([header] + body_rows):
        for c in range(len(header)):
            cell = table.cell(r, c)
            cell.text = ""
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            add_inline(p, row[c] if c < len(row) else "", TABLE_PT, bold=(r == 0))
            if r == 0:
                shade(cell)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)
    return table


def add_image(doc, path: Path, caption: str | None = None):
    from PIL import Image
    with Image.open(path) as im:
        w, h = im.size
    width = min(PAGE_WIDTH_IN, PAGE_WIDTH_IN)
    # Keep tall diagrams on one page: cap the height at roughly half a page.
    height = width * h / w
    if height > 7.5:
        width = width * 7.5 / height
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(4)
    p.add_run().add_picture(str(path), width=Inches(width))
    if caption:
        body(doc, caption, size=10.5, align=WD_ALIGN_PARAGRAPH.CENTER,
             space_after=12, italic_all=True)


def split_row(line):
    return [c.strip() for c in line.strip().strip("|").split("|")]


def build(out_path: Path):
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Inches(8.27), Inches(11.69)   # A4
    for margin in ("top_margin", "bottom_margin", "left_margin", "right_margin"):
        setattr(section, margin, Inches(1))

    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal.font.size = Pt(BODY_PT)

    # ---------------------------------------------------------- title page
    doc.add_paragraph().paragraph_format.space_after = Pt(30)
    for text, size, bold, after in TITLE_PAGE:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(after)
        style_run(p.add_run(text), size, bold=bold)
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

    # --------------------------------------------------------------- index
    heading(doc, "INDEX", 2)
    lines = REPORT.read_text(encoding="utf-8").splitlines()
    for line in lines:
        if re.match(r"^## \d", line):
            body(doc, line[3:], size=12.5, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=4)
        elif re.match(r"^### \d", line):
            body(doc, line[4:], size=12, align=WD_ALIGN_PARAGRAPH.LEFT,
                 space_after=3, indent=0.35)
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

    # -------------------------------------------------------------- content
    i = 0
    first_section = True
    in_front_matter = True
    paragraph_buffer: list[str] = []

    def flush():
        nonlocal paragraph_buffer
        if paragraph_buffer:
            body(doc, " ".join(paragraph_buffer))
            paragraph_buffer = []

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # Skip the front matter table and note at the top of report.md.
        if in_front_matter:
            if re.match(r"^## \d", line):
                in_front_matter = False
            else:
                i += 1
                continue

        if not stripped or stripped == "---":
            flush()
            i += 1
            continue

        m = re.match(r"^(#{2,4}) (.+)$", stripped)
        if m:
            flush()
            level = len(m.group(1))
            if level == 2 and not first_section:
                doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)
            first_section = False
            heading(doc, m.group(2), 2 if level == 2 else 3)
            i += 1
            continue

        m = re.match(r"^!\[(.*?)\]\((.+?)\)$", stripped)
        if m:
            flush()
            target = (DOCS / m.group(2)).resolve()
            caption = None
            # A bold "Figure n — ..." line directly below is the caption.
            if i + 1 < len(lines) and lines[i + 1].strip().startswith("**Figure"):
                caption_lines = []
                j = i + 1
                while j < len(lines) and lines[j].strip():
                    caption_lines.append(lines[j].strip())
                    j += 1
                caption = re.sub(r"[*]", "", " ".join(caption_lines))
                i = j - 1
            if target.exists():
                add_image(doc, target, caption)
            else:
                body(doc, f"[missing image: {m.group(2)}]", italic_all=True)
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < len(lines) and set(
                lines[i + 1].strip().replace("|", "").replace(" ", "")) <= {"-", ":"}:
            flush()
            rows = [split_row(stripped)]
            j = i + 2
            while j < len(lines) and lines[j].strip().startswith("|"):
                rows.append(split_row(lines[j]))
                j += 1
            add_table(doc, rows)
            i = j
            continue

        if stripped.startswith("> "):
            flush()
            quote = []
            while i < len(lines) and lines[i].strip().startswith(">"):
                quote.append(lines[i].strip().lstrip("> ").strip())
                i += 1
            body(doc, " ".join(q for q in quote if q), size=11.5,
                 indent=0.3, space_after=10)
            continue

        m = re.match(r"^(\d+)\. (.+)$", stripped)
        if m:
            flush()
            text, j = m.group(2), i + 1
            while j < len(lines) and lines[j].startswith("   ") and lines[j].strip():
                text += " " + lines[j].strip()
                j += 1
            p = doc.add_paragraph(style="List Number")
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(5)
            add_inline(p, text)
            i = j
            continue

        if stripped.startswith("- "):
            flush()
            text, j = stripped[2:], i + 1
            while j < len(lines) and lines[j].startswith("  ") and lines[j].strip():
                text += " " + lines[j].strip()
                j += 1
            p = doc.add_paragraph(style="List Bullet")
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(5)
            add_inline(p, text)
            i = j
            continue

        paragraph_buffer.append(stripped)
        i += 1

    flush()
    fix_zoom(doc)
    doc.save(str(out_path))
    return out_path


def fix_zoom(doc):
    """python-docx ships a settings.xml whose <w:zoom> has no percent.

    Word opens it anyway, but it fails schema validation, and a report that
    will not validate is a report that may not open somewhere it matters.
    """
    settings = doc.settings.element
    zoom = settings.find(qn("w:zoom"))
    if zoom is not None and zoom.get(qn("w:percent")) is None:
        zoom.set(qn("w:percent"), "100")


if __name__ == "__main__":
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DOCS / "Signal-Lost-Project-Report.docx"
    built = build(target)
    print(f"wrote {built}  ({built.stat().st_size / 1024:.0f} KB)")
