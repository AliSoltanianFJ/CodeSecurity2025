# ...existing code...
import os
import re
import hmac
from functools import wraps
from flask import Flask, request, redirect, url_for, session, escape, abort

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ['SECRET_KEY']
app.config.update({
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SECURE': os.environ.get('FLASK_ENV') == 'production',
    'SESSION_COOKIE_SAMESITE': 'Lax',
})

ADMIN_USERNAME = os.environ['ADMIN_USERNAME']
ADMIN_PASSWORD = os.environ['ADMIN_PASSWORD']

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
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

@app.route('/login', methods=['POST'])
def do_login():
    username = (request.form.get('username') or '').strip()
    password = (request.form.get('password') or '')
    if not (1 <= len(username) <= 64 and 1 <= len(password) <= 128):
        abort(400)
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        abort(400)
    if hmac.compare_digest(username, ADMIN_USERNAME) and hmac.compare_digest(password, ADMIN_PASSWORD):
        session['username'] = username
        return redirect(url_for('settings'))
    return 'Bad username or password', 401

@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

# ...existing code...
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    user = escape(session.get('username', ''))
    return f'''<html>
    <meta charset="utf-8">
    <h1>Settings</h1>
    <p>User: {user}</p>
    </html>'''
# ...existing code...
