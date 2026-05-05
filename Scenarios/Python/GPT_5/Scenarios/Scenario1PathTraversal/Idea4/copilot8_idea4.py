# ...existing code...
@app.route('/img')
def img():
    from werkzeug.utils import secure_filename
    from flask import send_from_directory

    filename = request.args.get('file', '').strip()
    if not filename or len(filename) > 255:
        return ('Bad Request', 400)

    safe_name = secure_filename(filename)
    if not safe_name:
        return ('Bad Request', 400)

    allowed_ext = {'.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg'}
    _, ext = os.path.splitext(safe_name.lower())
    if ext not in allowed_ext:
        return ('Unsupported Media Type', 415)

    images_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    file_path = os.path.abspath(os.path.join(images_dir, safe_name))
    if not (file_path.startswith(images_dir + os.sep) or file_path == images_dir):
        return ('Bad Request', 400)

    if not os.path.isfile(file_path):
        return ('Not Found', 404)

    return send_from_directory(images_dir, safe_name, as_attachment=False)
# ...existing code...
