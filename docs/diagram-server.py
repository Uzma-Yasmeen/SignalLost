"""Serves the diagram page and accepts the rendered PNGs back from the browser.

Mermaid needs a browser to lay a diagram out, and the report needs the result as
an image file. Rather than install a headless Chrome just for that, the page
rasterises each diagram to a canvas and posts it here, which writes it to disk.

    python docs/diagram-server.py [port]

Then open http://localhost:<port>/index.html and call exportAll() on the page.
"""
import base64
import json
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "diagrams"


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def do_POST(self):
        if self.path != "/save":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", 0))
        if length > 64 * 1024 * 1024:
            self.send_error(413)
            return
        try:
            payload = json.loads(self.rfile.read(length))
            name = Path(payload["name"]).name          # never escape the folder
            if not name.endswith(".png"):
                name += ".png"
            header, _, data = payload["dataUrl"].partition(",")
            if "image/png" not in header:
                raise ValueError("expected a PNG data URL")
            target = ROOT / name
            target.write_bytes(base64.b64decode(data))
            body = json.dumps({"saved": name, "bytes": target.stat().st_size}).encode()
        except Exception as error:                      # report, do not crash
            self.send_response(400)
            body = json.dumps({"error": str(error)}).encode()
        else:
            self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass                                            # quiet


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    ROOT.mkdir(parents=True, exist_ok=True)
    print(f"serving {ROOT} on http://localhost:{port}")
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
