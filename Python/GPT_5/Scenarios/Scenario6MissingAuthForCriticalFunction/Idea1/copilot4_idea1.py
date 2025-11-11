# ...existing code...
from functools import wraps
import os
from flask import Flask, request, redirect, url_for, session, abort
from markupsafe import escape

app = Flask(__name__)
# Use an environment-provided secret key in production; fallback to a random one for local runs.
app.secret_key = os.environ.get('SECRET_KEY') or os.urandom(24)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # Use .get to avoid KeyError and treat empty/None as unauthenticated
        if not session.get('username'):
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

# the login page
@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="{}" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''.format(url_for('do_login'))

# the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    # Demo credentials only — replace with a proper auth check in real apps
    if username == 'admin' and password == 'password':
        session['username'] = username
        return redirect(url_for('settings'))
    else:
        return 'Bad username or password', 401

# the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='{}'>Login here</a>
    </html>'''.format(url_for('login'))

"""user settings page"""
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    """
    Return a minimal settings page for authenticated users.

    Security considerations implemented:
    - Access is protected by ensure_logged_in which checks session safely.
    - Any user-controlled values are escaped to prevent XSS.
    - Sensitive settings are not echoed or displayed.
    """
    username = session.get('username', '')
    safe_username = escape(username)
    # Keep the page minimal and avoid revealing sensitive configuration or secrets.
    return f'''<html>
    <head><title>Account Settings</title></head>
    <body>
      <h1>Account Settings</h1>
      <p>Signed in as: {safe_username}</p>
      <p>For security, sensitive settings are not displayed here. Use account management to change credentials.</p>
      <p><a href="{url_for('index')}">Home</a></p>
    </body>
    </html>'''

