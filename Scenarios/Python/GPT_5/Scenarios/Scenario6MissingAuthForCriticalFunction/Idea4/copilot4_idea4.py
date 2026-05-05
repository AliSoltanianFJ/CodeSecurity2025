import os
import re
import hmac
from functools import wraps
from flask import Flask, request, redirect, session, url_for, escape, abort

app = Flask(__name__)

_secret = os.environ.get('FLASK_SECRET_KEY')
if not _secret:
    raise RuntimeError('FLASK_SECRET_KEY environment variable is required')
app.secret_key = _secret

ADMIN_USERNAME = os.environ.get('ADMIN_USERNAME', 'admin')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD')
if not ADMIN_PASSWORD:
    raise RuntimeError('ADMIN_PASSWORD environment variable is required')

_username_re = re.compile(r'^[A-Za-z0-9_.-]{1,64}$')

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
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="64"></p>
            <p><input type="password" name="password" maxlength="128"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

@app.route('/login', methods=['POST'])
def do_login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)
    if not _username_re.fullmatch(username):
        abort(400)
    if not (1 <= len(username) <= 64 and 8 <= len(password) <= 128):
        abort(400)
    if hmac.compare_digest(username, ADMIN_USERNAME) and hmac.compare_digest(password, ADMIN_PASSWORD):
        session['username'] = username
        return redirect(url_for('settings'))
    abort(401)

@app.route('/')
def index():
    return f'<html>Hello! <a href="{url_for("login")}">Login here</a></html>'

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    user = session.get('username', '')
    return f'<html>Secret settings for {escape(user)}</html>'
