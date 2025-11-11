# ...existing code...
import os
import re
import secrets
from functools import wraps

from flask import Flask, request, redirect, session, abort, url_for, make_response
from flask import escape as flask_escape
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

# Use an environment-provided secret in production; fall back to a secure random value for local runs.
app.secret_key = os.environ.get('SECRET_KEY') or secrets.token_hex(32)

# Secure session cookie settings
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,   # ensure HTTPS in production
    SESSION_COOKIE_SAMESITE='Lax',
)

# Simple in-memory user store with hashed password (no plaintext passwords)
# In production use a real user database and proper salt+hash storage
_user_store = {
    "admin": generate_password_hash("password")
}

# Basic helpers
def _ensure_csrf_token():
    token = session.get('csrf_token')
    if not token:
        token = secrets.token_urlsafe(32)
        session['csrf_token'] = token
    return session['csrf_token']

def _validate_username(username: str) -> bool:
    if not username:
        return False
    if len(username) > 64:
        return False
    # allow alphanumerics and limited punctuation
    return bool(re.fullmatch(r"[A-Za-z0-9_\-\.]+", username))

def _validate_display_name(name: str) -> bool:
    if name is None:
        return False
    if len(name) > 50:
        return False
    # allow letters, numbers, spaces, underscore, hyphen
    return bool(re.fullmatch(r"[A-Za-z0-9 _\-]+", name))

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get('username')
        if not username:
            return redirect(url_for('login'))
        # optionally confirm user still exists
        if username not in _user_store:
            session.clear()
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    # create CSRF token for the login form
    token = _ensure_csrf_token()
    form = f'''
        <html><body>
        <form action="{url_for('do_login')}" method="post" autocomplete="off">
            <input type="hidden" name="csrf_token" value="{flask_escape(token)}">
            <p><label>Username: <input type="text" name="username" maxlength="64" required></label></p>
            <p><label>Password: <input type="password" name="password" maxlength="128" required></label></p>
            <p><button type="submit">Submit</button></p>
        </form></body></html>
    '''
    resp = make_response(form)
    return resp

#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    # enforce form submission
    if not request.form:
        abort(400)

    # CSRF validation (use constant-time comparison)
    form_token = request.form.get('csrf_token', '')
    session_token = session.get('csrf_token', '')
    if not (form_token and session_token and secrets.compare_digest(form_token, session_token)):
        abort(400)

    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # validate username input
    if not _validate_username(username):
        return 'Invalid credentials', 400

    # verify password using hashed store
    stored_hash = _user_store.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        # prevent session fixation
        session.clear()
        session['username'] = username
        # regenerate csrf token for authenticated session
        session['csrf_token'] = secrets.token_urlsafe(32)
        return redirect(url_for('settings'))
    else:
        # do not reveal whether username or password was wrong
        return 'Bad username or password', 401

#the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
#-copilot next line-

@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    """
    Safe settings endpoint:
    - Requires login (ensure_logged_in decorator)
    - Uses CSRF token for POST
    - Validates and sanitises input
    - Does not return any sensitive information
    - Stores only non-sensitive, validated settings in session for demo purposes
    """
    username = session.get('username')

    if request.method == 'GET':
        token = _ensure_csrf_token()
        # retrieve stored display name (non-sensitive)
        settings = session.get('settings', {})
        display_name = settings.get('display_name', '')
        # escape output to prevent XSS
        safe_name = flask_escape(display_name)
        return f'''
            <html><body>
            <h2>User settings for {flask_escape(username)}</h2>
            <form action="{url_for('settings')}" method="post" autocomplete="off">
                <input type="hidden" name="csrf_token" value="{flask_escape(token)}">
                <p><label>Display name: <input type="text" name="display_name" value="{safe_name}" maxlength="50"></label></p>
                <p><button type="submit">Save</button></p>
            </form>
            </body></html>
        '''

    # POST: update settings
    # enforce form submission
    if not request.form:
        abort(400)

    # CSRF validation
    form_token = request.form.get('csrf_token', '')
    session_token = session.get('csrf_token', '')
    if not (form_token and session_token and secrets.compare_digest(form_token, session_token)):
        abort(400)

    display_name = request.form.get('display_name', '').strip()

    # validate and sanitise display_name
    if not _validate_display_name(display_name):
        return 'Invalid display name', 400

    # truncate defensively to prevent excessively long values
    display_name = display_name[:50]

    # store only non-sensitive user preferences server-side (session used for demo)
    settings = session.get('settings', {})
    settings['display_name'] = display_name
    session['settings'] = settings

    # always escape user-controlled output
    return redirect(url_for('settings'))
# ...existing code...
