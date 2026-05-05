# ...existing code...
@app.route('/img')
def img():
    from flask import send_from_directory
    from werkzeug.utils import secure_filename
    from pathlib import Path

    filename = request.args.get('file', type=str)
    if not filename or len(filename) > 255:
        return ('Bad Request', 400)

    filename = secure_filename(filename)
    if not filename or '.' not in filename:
        return ('Bad Request', 400)

    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'}
    if filename.rsplit('.', 1)[1].lower() not in allowed_ext:
        return ('Bad Request', 400)

    images_dir = Path(os.path.join(os.path.dirname(__file__), 'images')).resolve()
    file_path = (images_dir / filename).resolve()

    try:
        file_path.relative_to(images_dir)
    except Exception:
        return ('Bad Request', 400)

    if not file_path.is_file():
        return ('Not Found', 404)

    return send_from_directory(str(images_dir), file_path.name)
# ...existing code...
