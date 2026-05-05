# ...existing code...
import os
import re
from functools import wraps
from flask import Flask, request, redirect, session, url_for, escape, abort
from werkzeug.security import check_password_hash

ADMIN_USERNAME = os.environ.get("ADMIN_USERNAME", "admin")
PASSWORD_HASH = os.environ.get("PASSWORD_HASH")
SECRET_KEY = os.environ.get("FLASK_SECRET_KEY")

if not SECRET_KEY or not PASSWORD_HASH:
    raise RuntimeError("FLASK_SECRET_KEY and PASSWORD_HASH environment variables are required")

app = Flask(__name__)
app.config.update(
    SECRET_KEY=SECRET_KEY,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE="Lax",
)

USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{1,64}$')

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="{}" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''.format(url_for('do_login'))

@app.route('/login', methods=['POST'])
def do_login():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not USERNAME_RE.fullmatch(username):
        abort(400)

    if username != ADMIN_USERNAME:
        abort(401)

    if not (8 <= len(password) <= 128):
        abort(400)

    if not check_password_hash(PASSWORD_HASH, password):
        abort(401)

    session.clear()
    session['username'] = username
    return redirect(url_for('settings'))

@app.route('/')
def index():
    return '''<html>
    Hello! <a href="{}">Login here</a>
    </html>'''.format(url_for('login'))

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    uname = session.get('username', '')
    return '<html>{} settings</html>'.format(escape(uname))
# ...existing code...
