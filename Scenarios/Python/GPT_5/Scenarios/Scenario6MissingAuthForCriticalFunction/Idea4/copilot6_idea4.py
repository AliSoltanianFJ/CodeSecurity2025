
from functools import wraps
import os
import re
from hmac import compare_digest
from flask import Flask, request, redirect, url_for, abort, session, escape

app = Flask(__name__)
app.secret_key = os.environ['FLASK_SECRET']

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not isinstance(session.get('username'), str):
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    action = url_for('do_login')
    return f'''
        <html><form action="{escape(action)}" method="post">
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
    if not re.fullmatch(r'[A-Za-z0-9_]{1,64}', username):
        abort(400)
    if len(password) > 128:
        abort(400)
    if compare_digest(password, os.environ.get('DEFAULT_USER_PASSWORD', 'password')) and username == 'admin':
        session['username'] = username
        return redirect(url_for('settings'))
    return 'Bad username or password', 403

@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    username = session.get('username')
    if not isinstance(username, str) or len(username) > 64:
        abort(403)
    return f'<html>Secret settings for {escape(username)}</html>'