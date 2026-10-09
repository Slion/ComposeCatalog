"""Render a Mermaid diagram to a self-contained, draggable HTML page.

Usage:
    python scripts/render_diagram.py [diagram.mmd] [output.html]

Defaults to ``scripts/api-class-diagram.mmd`` next to this file. The HTML embeds
mermaid.js (downloaded once into ``scripts/`` so the page also works offline)
and adds drag support for class nodes — connected edges are re-routed while
dragging. No environment beyond the Python standard library is required.
"""
from __future__ import annotations

import sys
import urllib.request
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
MERMAID_JS = SCRIPT_DIR / "mermaid.min.js"
MERMAID_URL = "https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>__TITLE__</title>
<script src="mermaid.min.js"></script>
<style>
  body {
    margin: 0; height: 100vh; display: flex; flex-direction: column; overflow: hidden;
    font-family: system-ui, sans-serif; background: #1e1e1e; color: #ddd;
  }
  #toolbar {
    flex: none; z-index: 10; display: flex; gap: 8px; align-items: center;
    padding: 8px 12px; background: #252526; border-bottom: 1px solid #3c3c3c;
  }
  #toolbar h1 { font-size: 14px; margin: 0 auto 0 0; font-weight: 600; color: #e0e0e0; }
  #toolbar button { padding: 4px 12px; cursor: pointer; }
  #zoom { display: flex; align-items: center; gap: 4px; margin-left: 8px; }
  #zoom-level { font-size: 12px; color: #888; min-width: 44px; text-align: center; }
  #hint { font-size: 12px; color: #888; }
  #legend { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #888; }
  #canvas { flex: 1 1 auto; overflow: auto; }
  #canvas svg { display: block; margin: 24px; user-select: none; }
  #canvas g.node { cursor: grab; }
  #canvas g.node:active { cursor: grabbing; }
  /* make solid (is-a / uses) vs dashed (dependency) edges easy to tell apart */
  #canvas path.edge-pattern-solid { stroke: #d3d3d3; stroke-width: 1.6px; }
  #canvas path.edge-pattern-dashed {
    stroke: #6ea8ff;
    stroke-width: 1.2px;
    stroke-dasharray: 8 6;
  }
  #error { white-space: pre-wrap; color: #b00020; font-family: ui-monospace, monospace;
           padding: 16px; font-size: 13px; }
</style>
</head>
<body>
<div id="toolbar">
  <button id="reset">Reset layout</button>
  <span id="zoom">
    <button id="zoom-out" title="Zoom out (Ctrl + mouse wheel)">&minus;</button>
    <button id="zoom-reset" title="Reset zoom"><span id="zoom-level">100%</span></button>
    <button id="zoom-in" title="Zoom in (Ctrl + mouse wheel)">+</button>
  </span>
  <h1>__TITLE__</h1>
  <span id="legend">
    <svg width="26" height="8"><line x1="0" y1="4" x2="26" y2="4" stroke="#d3d3d3" stroke-width="1.6"/></svg> is-a / uses
    <svg width="26" height="8" style="margin-left:10px"><line x1="0" y1="4" x2="26" y2="4" stroke="#6ea8ff" stroke-width="1.2" stroke-dasharray="8 6"/></svg> dependency
  </span>
  <span id="hint">Drag a class to move it; drag the background to pan; connected edges follow.</span>
</div>
<div id="canvas"></div>
<script type="text/plain" id="diagram">
__DIAGRAM__
</script>
<script>
const diagramSource = document.getElementById('diagram').textContent;
mermaid.initialize({ startOnLoad: false, securityLevel: 'loose', theme: 'dark' });

(async () => {
  const canvas = document.getElementById('canvas');
  let svg;
  try {
    const out = await mermaid.render('diagram', diagramSource);
    canvas.innerHTML = out.svg;
    svg = canvas.querySelector('svg');
  } catch (e) {
    canvas.innerHTML = '<pre id="error">MERMAID PARSE ERROR:\\n' + e.message + '</pre>';
    return;
  }
  // Mermaid paints edgePaths before the nodes, so boxes cover the arrowheads.
  // Move the edge groups to the end of the SVG (top of the paint order).
  const edgePaths = svg.querySelector('g.edgePaths');
  const edgeLabels = svg.querySelector('g.edgeLabels');
  if (edgePaths) svg.appendChild(edgePaths);
  if (edgeLabels) svg.appendChild(edgeLabels);
  fixMarkerTips(svg);
  setupDrag(svg);
  setupPan(svg);
  setupZoom(svg);
})();
// Pan: dragging the background (anywhere that is not a node) scrolls the
// canvas, so the diagram can be moved around at any zoom level.
function setupPan(svg) {
  const canvas = document.getElementById('canvas');
  let pan = null;
  svg.addEventListener('pointerdown', (ev) => {
    if (ev.button !== 0) return;
    if (ev.target.closest && ev.target.closest('g.node')) return;
    ev.preventDefault();
    pan = { sx: ev.clientX, sy: ev.clientY, sl: canvas.scrollLeft, st: canvas.scrollTop };
    svg.setPointerCapture(ev.pointerId);
  });
  svg.addEventListener('pointermove', (ev) => {
    if (!pan) return;
    canvas.scrollLeft = pan.sl - (ev.clientX - pan.sx);
    canvas.scrollTop = pan.st - (ev.clientY - pan.sy);
  });
  svg.addEventListener('pointerup', () => { pan = null; });
  svg.addEventListener('pointercancel', () => { pan = null; });
}
// Zoom: the SVG has a viewBox, so changing its rendered width scales it 1:1.
// The content point under the reference viewport position (vx, vy — viewport
// center by default, or the cursor for wheel zoom) is kept fixed.
function setupZoom(svg) {
  const canvas = document.getElementById('canvas');
  const level = document.getElementById('zoom-level');
  // The SVG has a fixed height (width=100%), so scaling only the width would
  // letterbox it; pin both dimensions from the natural rendered size.
  const base = svg.getBoundingClientRect();
  const baseWidth = base.width, baseHeight = base.height;
  let zoom = 1;
  function setZoom(nz, vx, vy) {
    const z0 = zoom;
    zoom = Math.min(4, Math.max(0.25, nz));
    if (zoom === z0) return;
    if (vx == null) vx = canvas.clientWidth / 2;
    if (vy == null) vy = canvas.clientHeight / 2;
    const cx = canvas.scrollLeft + vx;
    const cy = canvas.scrollTop + vy;
    svg.style.width = (baseWidth * zoom) + 'px';
    svg.style.height = (baseHeight * zoom) + 'px';
    canvas.scrollLeft = cx * (zoom / z0) - vx;
    canvas.scrollTop = cy * (zoom / z0) - vy;
    level.textContent = Math.round(zoom * 100) + '%';
  }
  document.getElementById('zoom-in').addEventListener('click', () => setZoom(zoom * 1.25));
  document.getElementById('zoom-out').addEventListener('click', () => setZoom(zoom / 1.25));
  document.getElementById('zoom-reset').addEventListener('click', () => setZoom(1));
  // Ctrl/Cmd + wheel zooms around the cursor (matches browser page-zoom habit).
  canvas.addEventListener('wheel', (ev) => {
    if (!ev.ctrlKey && !ev.metaKey) return;
    ev.preventDefault();
    const c = canvas.getBoundingClientRect();
    setZoom(zoom * (ev.deltaY < 0 ? 1.15 : 1 / 1.15), ev.clientX - c.left, ev.clientY - c.top);
  }, { passive: false });
}
// Our paths start/end exactly on the box borders, so each marker must be anchored
// so its body stays OUT of the box:
//  - End markers (arrowheads): anchor the TIP (max x) on the border.
//  - Start markers (diamonds/triangles): anchor the BASE (min x) on the border so
//    the body extends out along the line. (Mermaid's own refX values assumed the
//    boxes are painted OVER the edges; we paint edges on top instead.)
function fixMarkerTips(svg) {
  for (const m of svg.querySelectorAll('defs marker')) {
    const p = m.querySelector('path');
    if (!p) continue;
    const id = m.id;
    if (id.includes('lollipop') || id.includes('margin')) continue;
    const d = p.getAttribute('d') || '';
    const xs = [...d.matchAll(/(?:M|L)\\s*([\\d.]+)/g)].map(x => parseFloat(x[1]));
    if (!xs.length) continue;
    const target = id.endsWith('Start') ? Math.min(...xs) : Math.max(...xs);
    if (m.getAttribute('refX') !== String(target)) m.setAttribute('refX', String(target));
  }
}


function setupDrag(svg) {
  const nodes = Array.from(svg.querySelectorAll('g.node'));
  const nodeById = new Map();
  for (const el of nodes) {
    const m = (el.getAttribute('transform') || '').match(/translate\\(\\s*([\\d.\\-]+)[ ,]+([\\d.\\-]+)/);
    const rect = el.querySelector('rect');
    nodeById.set(el.id, {
      el, rect,
      x: m ? parseFloat(m[1]) : 0,
      y: m ? parseFloat(m[2]) : 0,
      dx: 0, dy: 0,
    });
  }

  // --- edge resolution -----------------------------------------------------
  // Mermaid names edge paths "L_<from>_<to>". Match that against the node ids;
  // fall back to geometric proximity (path endpoints vs node centers).
  // Real visual bounds of the whole node group (rects, labels), in node-local
  // coords (the group's translate is not applied to getBBox()).
  function localBox(n) {
    const b = n.el.getBBox();
    return { x: b.x, y: b.y, w: b.width, h: b.height };
  }
  function center(n) {
    const b = localBox(n);
    return { x: n.x + n.dx + b.x + b.w / 2, y: n.y + n.dy + b.y + b.h / 2 };
  }
  function nearestNode(px, py) {
    let best = null, bestD = Infinity;
    for (const n of nodeById.values()) {
      const c = center(n);
      const d = (c.x - px) ** 2 + (c.y - py) ** 2;
      if (d < bestD) { bestD = d; best = n; }
    }
    return best;
  }
  function matchIdPart(s) {
    s = s.replace(/^_/, '');
    for (const id of nodeById.keys()) if (s === id) return [id, ''];
    for (const id of nodeById.keys())
      if (s.startsWith(id + '_')) return [id, s.slice(id.length + 1)];
    for (const id of nodeById.keys())
      if (s.startsWith(id) && s.length > id.length) return [id, s.slice(id.length)];
    return null;
  }
  function resolveEndpoints(path) {
    const pid = (path.id || '').replace(/^L_/, '');
    const a = matchIdPart(pid);
    if (a) {
      const b = matchIdPart(a[1]);
      if (b && b[0]) return [nodeById.get(a[0]), nodeById.get(b[0])];
    }
    try {
      const len = path.getTotalLength();
      const p0 = path.getPointAtLength(0), p1 = path.getPointAtLength(len);
      const from = nearestNode(p0.x, p0.y), to = nearestNode(p1.x, p1.y);
      // from === to is a self-loop; still a valid anchor pair
      if (from && to) return [from, to];
    } catch (_) {}
    return [null, null];
  }

  const edges = [];
  // In mermaid v11 the edge labels are sibling groups with no id linking them
  // to their path; document order pairs each label with its path (a label with
  // no text sits at translate(0,0) and is simply not repositioned).
  const allPaths = [...svg.querySelectorAll('g.edgePaths path')];
  const allLabels = [...svg.querySelectorAll('g.edgeLabels > g.edgeLabel')];
  allPaths.forEach((path, i) => {
    if (!path.getAttribute('d')) {
      edges.push({ path, from: null, to: null, label: null });
      return;
    }
    const [from, to] = resolveEndpoints(path);
    edges.push({
      path, from, to,
      label: (from && to) ? allLabels[i] || null : null,
    });
  });

  // --- geometry --------------------------------------------------------------
  function box(n) {
    const b = localBox(n);
    return { w: b.w, h: b.h, cx: center(n).x, cy: center(n).y };
  }
  // Point on the border of box `b` along the line from b.center towards (tx, ty),
  // plus the outward normal (nx, ny) of the edge that was hit.
  function borderPoint(b, tx, ty) {
    const dx = tx - b.cx, dy = ty - b.cy;
    const txm = Math.abs(dx) > 1e-9 ? (b.w / 2) / Math.abs(dx) : Infinity;
    const tym = Math.abs(dy) > 1e-9 ? (b.h / 2) / Math.abs(dy) : Infinity;
    const t = Math.min(txm, tym);
    let nx = 0, ny = 0;
    if (t === txm) { nx = Math.sign(dx) || 1; } else { ny = Math.sign(dy) || 1; }
    return { x: b.cx + dx * t, y: b.cy + dy * t, nx, ny };
  }
  function updateAllEdges() {
    for (const e of edges) updateEdge(e);
  }
  function placeLabel(e) {
    if (!e.label) return;
    try {
      const mid = e.path.getPointAtLength(e.path.getTotalLength() / 2);
      e.label.setAttribute('transform', `translate(${mid.x},${mid.y})`);
    } catch (_) {}
  }
  function updateEdge(e) {
    if (!e.from || !e.to) return;
    // self-loop: leave the right border twice, arc out, come back in
    if (e.from === e.to) {
      const a = box(e.from);
      const w2 = a.w / 2, h2 = a.h / 2;
      const y1 = a.cy - h2 / 3, y2 = a.cy + h2 / 3;
      const r = 48;
      e.path.setAttribute('d',
        `M ${a.cx + w2} ${y1} C ${a.cx + w2 + r} ${y1}, ${a.cx + w2 + r} ${y2}, ${a.cx + w2} ${y2}`);
      placeLabel(e);
      return;
    }
    const a = box(e.from), b = box(e.to);
    const p1 = borderPoint(a, b.cx, b.cy);
    const p2 = borderPoint(b, a.cx, a.cy);
    // cubic whose tangents at both ends follow the border normals, so the path
    // leaves box A and arrives at box B perpendicular to the edge it touches
    const k = Math.max(40, Math.hypot(p2.x - p1.x, p2.y - p1.y) / 2);
    e.path.setAttribute('d',
      `M ${p1.x} ${p1.y} C ${p1.x + p1.nx * k} ${p1.y + p1.ny * k}, ` +
      `${p2.x + p2.nx * k} ${p2.y + p2.ny * k}, ${p2.x} ${p2.y}`);
    placeLabel(e);
  }
  function applyNode(n) {
    n.el.setAttribute('transform', `translate(${n.x + n.dx},${n.y + n.dy})`);
  }

  // --- dragging --------------------------------------------------------------
  let drag = null;
  for (const n of nodeById.values()) {
    n.el.addEventListener('pointerdown', (ev) => {
      if (ev.target.closest && ev.target.closest('button')) return;
      ev.preventDefault();
      drag = { n, sx: ev.clientX, sy: ev.clientY, dx0: n.dx, dy0: n.dy };
      n.el.setPointerCapture(ev.pointerId);
    });
  }
  svg.addEventListener('pointermove', (ev) => {
    if (!drag) return;
    const scale = svg.getBoundingClientRect().width / svg.viewBox.baseVal.width || 1;
    drag.n.dx = drag.dx0 + (ev.clientX - drag.sx) / scale;
    drag.n.dy = drag.dy0 + (ev.clientY - drag.sy) / scale;
    applyNode(drag.n);
    for (const e of edges) if (e.from === drag.n || e.to === drag.n) updateEdge(e);
  });
  svg.addEventListener('pointerup', () => { drag = null; });

  document.getElementById('reset').addEventListener('click', () => {
    for (const n of nodeById.values()) { n.dx = 0; n.dy = 0; applyNode(n); }
    updateAllEdges();
  });

  // Re-anchor every edge once at the end, so all arrowheads sit exactly on
  // the box borders (mermaid's own routing leaves them slightly off).
  updateAllEdges();
}
</script>
</body>
</html>
"""


def ensure_mermaid_js() -> None:
    if MERMAID_JS.exists() and MERMAID_JS.stat().st_size > 100_000:
        return
    print(f"downloading {MERMAID_URL} -> {MERMAID_JS.name} ...")
    req = urllib.request.Request(MERMAID_URL, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as resp, open(MERMAID_JS, "wb") as out:
        out.write(resp.read())


def main() -> None:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else SCRIPT_DIR / "api-class-diagram.mmd"
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else src.with_suffix(".html")
    if not src.exists():
        sys.exit(f"diagram not found: {src}")
    ensure_mermaid_js()
    html = (
        HTML_TEMPLATE.replace("__DIAGRAM__", src.read_text(encoding="utf-8"))
        .replace("__TITLE__", src.stem.replace("-", " "))
    )
    # mermaid.min.js must sit next to the HTML (relative <script src>)
    out.write_text(html, encoding="utf-8")
    if out.resolve().parent != MERMAID_JS.parent:
        out.write_text(
            html.replace('src="mermaid.min.js"',
                         f'src="{(out.parent / ".." / MERMAID_JS.name).as_posix()}"'),
            encoding="utf-8")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
