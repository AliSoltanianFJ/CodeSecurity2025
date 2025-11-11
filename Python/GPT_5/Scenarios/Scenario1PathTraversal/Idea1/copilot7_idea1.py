# ...existing code...
@app.route('/img')
def img():
    from flask import abort, send_file
    from werkzeug.utils import safe_join
    import mimetypes
    from pathlib import Path

    name = request.args.get('name', type=str)
    if not name:
        return abort(400)

    images_dir = Path(app.root_path) / 'images'
    try:
        safe_path = safe_join(str(images_dir), name)
    except Exception:
        return abort(400)

    p = Path(safe_path)
    if not p.is_file():
        return abort(404)

    mime, _ = mimetypes.guess_type(p.name)
    if not (mime and mime.startswith('image/')):
        return abort(415)

    return send_file(str(p), mimetype=mime)
# ...existing code...
