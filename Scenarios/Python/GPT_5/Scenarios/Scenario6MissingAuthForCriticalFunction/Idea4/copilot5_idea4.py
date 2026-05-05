import os
import hmac
import html
from functools import wraps
from flask import Flask, request, redirect, session, abort

app = Flask(__name__)

_secret = os.environ.get('FLASK_SECRET_KEY')
_admin_user = os.environ.get('ADMIN_USERNAME')
_admin_pass = os.environ.get('ADMIN_PASSWORD')

if not _secret or not _admin_user or not _admin_pass:
    raise RuntimeError('Required environment variables: FLASK_SECRET_KEY, ADMIN_USERNAME, ADMIN_PASSWORD')

app.secret_key = _secret

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect('/login')
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="128" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

@app.route('/login', methods=['POST'])
def do_login():
    username = (request.form.get('username') or '').strip()
    password = (request.form.get('password') or '')
    if not username or not password or len(username) > 128 or len(password) > 128:
        abort(400)
    if hmac.compare_digest(username, _admin_user) and hmac.compare_digest(password, _admin_pass):
        session.clear()
        session['username'] = username
        return redirect('/settings')
    return abort(401)

@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    user = html.escape(session.get('username', ''))
    return f'''<html>
    <h1>Settings</h1>
    <p>Signed in as: {user}</p>
    </html>'''
