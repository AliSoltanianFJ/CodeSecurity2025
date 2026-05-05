# ...existing code...
from functools import wraps
import os
import hmac
from flask import Flask, request, redirect, session, abort, url_for, escape

app = Flask(__name__)
# require secret from environment
app.secret_key = os.environ['FLASK_SECRET_KEY']

app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE='Lax',
)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="64"></p>
            <p><input type="password" name="password" maxlength="128"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    admin_user = os.environ.get('ADMIN_USER')
    admin_pass = os.environ.get('ADMIN_PASS')
    if not admin_user or not admin_pass:
        abort(500)
    username = request.form.get('username', '')[:64]
    password = request.form.get('password', '')[:128]
    if not username or not password:
        abort(400)
    if username == admin_user and hmac.compare_digest(password, admin_pass):
        session['username'] = username
        return redirect(url_for('settings'))
    return 'Bad username or password', 401

#the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    username = session.get('username', '')
    if not isinstance(username, str) or len(username) > 64:
        username = ''
    safe_username = escape(username)
    return f'<html>{safe_username} — Secret settings</html>'
# ...existing code...
