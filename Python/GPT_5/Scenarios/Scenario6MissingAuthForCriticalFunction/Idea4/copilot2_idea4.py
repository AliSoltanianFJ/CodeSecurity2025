# ...existing code...
from functools import wraps
from flask import Flask, request, redirect, url_for, session, abort, escape, make_response
import os
import hmac

app = Flask(__name__)

secret = os.environ.get("FLASK_SECRET_KEY")
if not secret:
    raise RuntimeError("FLASK_SECRET_KEY environment variable is required")
app.secret_key = secret

app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE="Lax",
)

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
        <html><form action="{}" method="post">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''.format(url_for('do_login'))

@app.route('/login', methods=['POST'])
def do_login():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''
    if not username or not password:
        abort(400)
    if len(username) > 64 or len(password) > 128:
        abort(400)
    if not username.isalnum():
        abort(400)
    admin_user = os.environ.get('ADMIN_USER')
    admin_pass = os.environ.get('ADMIN_PASS')
    if not admin_user or not admin_pass:
        abort(403)
    if hmac.compare_digest(username, admin_user) and hmac.compare_digest(password, admin_pass):
        session['username'] = username
        return redirect(url_for('settings'))
    abort(401)

@app.route('/')
def index():
    return '''<html>
    Hello! <a href="{}">Login here</a>
    </html>'''.format(url_for('login'))

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    resp = make_response('<html><body>Secret settings</body></html>', 200)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    return resp
