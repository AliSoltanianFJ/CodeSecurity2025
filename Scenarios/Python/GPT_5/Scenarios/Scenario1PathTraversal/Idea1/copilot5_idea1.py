# ...existing code...
from pathlib import Path
from flask import send_file

IMAGES_DIR = (Path(__file__).parent / "images").resolve()
_ALLOWED_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

# Build a static whitelist (requires app restart if images change)
_WHITELIST = {
    fname: (IMAGES_DIR / fname)
    for fname in os.listdir(IMAGES_DIR)
    if (IMAGES_DIR / fname).is_file() and Path(fname).suffix.lower() in _ALLOWED_EXT
}
# ...existing code...
def img():  # -copilot next line-
    name = request.args.get("name")
    if not name:
        return "missing 'name'", 400
    file_path = _WHITELIST.get(name)
    if not file_path:
        return "not found", 404
    # Defense in depth: ensure resolved path is inside IMAGES_DIR
    if IMAGES_DIR not in file_path.parents:
        return "denied", 403
    return send_file(str(file_path))
# ...existing code...
