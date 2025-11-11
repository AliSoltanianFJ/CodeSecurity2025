# ...existing code...
@app.route('/img')
def img():
    from flask import send_from_directory, abort
    from werkzeug.utils import secure_filename

    # filename from query string (?file=example.png or ?name=example.png)
    filename = request.args.get("file") or request.args.get("name")
    if not filename:
        abort(400, description="Missing file parameter")

    # sanitize and validate extension
    safe_name = secure_filename(filename)
    allowed_exts = {"png", "jpg", "jpeg", "gif", "webp", "bmp"}
    ext = os.path.splitext(safe_name)[1].lower().lstrip(".")
    if not safe_name or ext not in allowed_exts:
        abort(400, description="Invalid file")

    # build safe absolute path within images directory
    images_dir = os.path.realpath(os.path.join(os.path.dirname(__file__), "images"))
    target_path = os.path.realpath(os.path.join(images_dir, safe_name))

    if not target_path.startswith(images_dir + os.sep):
        abort(403)

    if not os.path.isfile(target_path):
        abort(404)

    return send_from_directory(images_dir, safe_name)
# ...existing code...
