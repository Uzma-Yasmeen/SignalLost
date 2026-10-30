/**
 * Builds a page that renders every Mermaid diagram in uml.md, so they can be
 * checked for syntax errors and exported as PNG for the course report.
 *
 * A diagram with a mistake in it does not fail loudly; Mermaid draws a small
 * error box instead, which is easy to paste into a document without noticing.
 * This page reports a count, so a broken diagram cannot go unnoticed.
 *
 *   node docs/render-diagrams.js          # writes docs/diagrams/index.html
 */
const fs = require('fs');
const path = require('path');

const docs = __dirname;
const outDir = path.join(docs, 'diagrams');
fs.mkdirSync(outDir, { recursive: true });

const markdown = fs.readFileSync(path.join(docs, 'uml.md'), 'utf8');

// Each diagram is named by the heading above it, so the exported files are
// recognisable rather than diagram-1.png.
const blocks = [];
const lines = markdown.split('\n');
let heading = 'diagram';
let collecting = null;

for (const line of lines) {
    const headingMatch = line.match(/^#{2,3}\s+(.+)$/);
    if (headingMatch && collecting === null) {
        heading = headingMatch[1]
            .replace(/[^A-Za-z0-9. ]/g, '')
            .trim()
            .replace(/\s+/g, '-')
            .toLowerCase();
    }
    if (line.trim() === '```mermaid') {
        collecting = [];
        continue;
    }
    if (collecting !== null && line.trim() === '```') {
        const name = blocks.some(b => b.name === heading)
            ? `${heading}-${blocks.filter(b => b.name.startsWith(heading)).length + 1}`
            : heading;
        blocks.push({ name, source: collecting.join('\n') });
        collecting = null;
        continue;
    }
    if (collecting !== null) {
        collecting.push(line);
    }
}

const page = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Signal Lost diagrams</title>
<style>
  body { background:#fff; color:#111; font-family:system-ui, sans-serif; margin:0; padding:24px; }
  h2 { font-size:15px; margin:32px 0 8px; color:#334155; font-weight:600; }
  .d { background:#fff; padding:16px 0; }
  #status { position:sticky; top:0; background:#111; color:#fff; padding:10px 14px;
            border-radius:6px; font:13px ui-monospace, monospace; z-index:9; }
</style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/mermaid/10.9.1/mermaid.min.js"></script>
</head>
<body>
<div id="status">rendering…</div>
${blocks.map(b => `<h2>${b.name}</h2>\n<div class="d mermaid" data-name="${b.name}">${
    b.source.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}</div>`).join('\n')}
<script>
  // Every diagram type needs useMaxWidth off, not just the two obvious ones:
  // a type left out renders at width="100%" with no height and collapses to
  // nothing, which mermaid.render reports as a success.
  mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose',
                       flowchart: { useMaxWidth: false }, sequence: { useMaxWidth: false },
                       state: { useMaxWidth: false }, class: { useMaxWidth: false },
                       er: { useMaxWidth: false }, gantt: { useMaxWidth: false } });
  (async () => {
    let ok = 0;
    const failures = [];
    const nodes = [...document.querySelectorAll('.mermaid')];
    for (const node of nodes) {
      const name = node.dataset.name;
      const source = node.textContent;
      try {
        const { svg } = await mermaid.render('svg-' + Math.random().toString(36).slice(2), source);
        node.innerHTML = svg;
        ok++;
      } catch (e) {
        failures.push(name + ': ' + (e && e.message ? e.message.split('\\n')[0] : e));
        node.innerHTML = '<pre style="color:#b91c1c">' + name + ' FAILED</pre>';
      }
    }
    // A diagram that renders to nothing is a failure too, however quietly.
    for (const node of nodes) {
      const svg = node.querySelector('svg');
      const box = svg && svg.getBoundingClientRect();
      if (!box || box.width < 20 || box.height < 20) {
        failures.push(node.dataset.name + ': rendered empty');
        ok--;
      }
    }
    window.__diagramReport = { total: nodes.length, ok, failures };
    document.getElementById('status').textContent =
      failures.length ? (ok + '/' + nodes.length + ' rendered — ' + failures.length + ' FAILED')
                      : (ok + '/' + nodes.length + ' rendered, no errors');
  })();
</script>
</body>
</html>`;

fs.writeFileSync(path.join(outDir, 'index.html'), page, 'utf8');
console.log(`Wrote ${path.join(outDir, 'index.html')} with ${blocks.length} diagrams:`);
blocks.forEach(b => console.log('  ' + b.name));
