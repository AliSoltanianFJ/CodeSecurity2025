# ...existing code...
import os
import hmac
from functools import wraps
from flask import Flask, request, redirect, url_for, session, abort
from markupsafe import escape

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ['APP_SECRET_KEY']

ADMIN_USER = os.environ['ADMIN_USER']
ADMIN_PASS = os.environ['ADMIN_PASS']

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect(url_for('login', next=request.path))
        return f(*args, **kws)
    return decorated_function

@app.route('/login', methods=['GET'])
def login():
    next_ = request.args.get('next', '')
    next_input = f'<input type="hidden" name="next" value="{escape(next_)}">' if next_ else ''
    return f'''
        <html><form action="{url_for('do_login')}" method="post">
            <p><input type="text" name="username" maxlength="150" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            {next_input}
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

@app.route('/login', methods=['POST'])
def do_login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not (1 <= len(username) <= 150 and 1 <= len(password) <= 128):
        abort(400)
    if hmac.compare_digest(username, ADMIN_USER) and hmac.compare_digest(password, ADMIN_PASS):
        session['username'] = username
        next_url = request.form.get('next', '')
        if next_url and next_url.startswith('/') and not next_url.startswith('//'):
            return redirect(next_url)
        return redirect(url_for('index'))
    return 'Bad username or password', 401

@app.route('/')
def index():
    return f'''<html>
    Hello! <a href="{url_for('login')}">Login here</a>
    </html>'''

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    username = session.get('username', '')
    return f'<html>Secret settings for {escape(username)}</html>'
# ...existing code...
