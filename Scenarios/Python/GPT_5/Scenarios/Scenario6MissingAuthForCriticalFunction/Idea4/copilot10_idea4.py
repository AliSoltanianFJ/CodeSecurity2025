import os
from functools import wraps
from flask import Flask, request, redirect, url_for, session, abort, escape

app = Flask(__name__)

# require explicit secret in environment
_secret = os.environ.get('FLASK_SECRET_KEY')
if not _secret:
    raise RuntimeError('FLASK_SECRET_KEY environment variable is required')
app.secret_key = _secret

# admin credentials must come from environment
_ADMIN_USER = os.environ.get('ADMIN_USERNAME')
_ADMIN_PASS = os.environ.get('ADMIN_PASSWORD')
if not (_ADMIN_USER and _ADMIN_PASS):
    raise RuntimeError('ADMIN_USERNAME and ADMIN_PASSWORD environment variables are required')

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get('username')
        if not username:
            return redirect(url_for('login', next=request.path))
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="150" required></p>
            <p><input type="password" name="password" maxlength="150" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

@app.route('/login', methods=['POST'])
def do_login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)
    if not (1 <= len(username) <= 150 and 1 <= len(password) <= 150):
        abort(400)
    if username == _ADMIN_USER and password == _ADMIN_PASS:
        session['username'] = username
        return redirect(url_for('settings'))
    return 'Bad username or password', 401

@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    username = session.get('username')
    if not isinstance(username, str) or len(username) > 150:
        abort(400)
    safe_username = escape(username)
    return f'<html><body><h1>Account settings</h1><p>User: {safe_username}</p></body></html>'
