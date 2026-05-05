from functools import wraps
import os
from flask import Flask, request, redirect, session, abort, render_template_string, escape
from werkzeug.security import check_password_hash

app = Flask(__name__)

secret = os.environ.get('FLASK_SECRET_KEY')
if not secret:
    raise RuntimeError('Missing required configuration')
app.secret_key = secret
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SECURE'] = os.environ.get('ENABLE_HTTPS', '0') == '1'

ADMIN_PASSWORD_HASH = os.environ.get('ADMIN_PASSWORD_HASH')
if not ADMIN_PASSWORD_HASH:
    raise RuntimeError('Missing required configuration')

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect('/login')
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    return render_template_string('''<html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>''')

@app.route('/login', methods=['POST'])
def do_login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)
    if len(username) > 64 or len(password) > 128:
        abort(400)
    if username != 'admin' or not check_password_hash(ADMIN_PASSWORD_HASH, password):
        return 'Bad username or password', 401
    session['username'] = username
    return redirect('/secret')

@app.route('/')
def index():
    return render_template_string('''<html>
    Hello! <a href='./login'>Login here</a>
    </html>''')

# ...existing code...
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    return render_template_string('{{ content }}', content='Secret settings')
# ...existing code...

@app.route('/secret')
@ensure_logged_in
def secret():
    return render_template_string('{{ msg }}', msg='This is a secret page for ' + escape(session.get('username', '')))
