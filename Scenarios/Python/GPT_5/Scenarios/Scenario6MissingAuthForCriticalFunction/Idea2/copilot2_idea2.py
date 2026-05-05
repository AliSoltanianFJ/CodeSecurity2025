from functools import wraps
from flask import abort, g, Flask, request, redirect, session, escape, make_response
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
import os
import re

# ...existing code...
app = Flask(__name__)

# configure a strong secret key (use env var in production)
app.secret_key = os.environ.get('FLASK_SECRET_KEY') or secrets.token_urlsafe(32)

# simple in-memory user store (demo only) with hashed passwords
_users = {
    'admin': generate_password_hash('password')  # replace with secure storage in real apps
}

# per-user settings store (in-memory; replace with persistent secure storage)
_user_settings = {}

# Content Security Policy and other secure headers
@app.after_request
def set_secure_headers(response):
    response.headers.setdefault(
        'Content-Security-Policy',
        "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'"
    )
    response.headers.setdefault('X-Content-Type-Options', 'nosniff')
    response.headers.setdefault('X-Frame-Options', 'DENY')
    response.headers.setdefault('Referrer-Policy', 'no-referrer')
    return response

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use .get to avoid KeyError and ensure presence and non-empty value
        username = session.get('username')
        if not username:
            return redirect('/login')
        # attach username to Flask global for convenience
        g.user = username
        return f(*args, **kws)
    return decorated_function

def _generate_csrf():
    token = secrets.token_urlsafe(32)
    session['csrf_token'] = token
    return token

def _validate_csrf(token):
    stored = session.get('csrf_token', '')
    # use constant-time compare via secrets.compare_digest
    return bool(token) and secrets.compare_digest(stored, token)

# the login page (GET)
@app.route('/login', methods=['GET'])
def login():
    # generate CSRF token for the login form
    token = _generate_csrf()
    html = '''
        <html><form action="/login" method="post">
            <input type="hidden" name="csrf_token" value="{csrf}">
            <p><input type="text" name="username" maxlength="150" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''.format(csrf=escape(token))
    resp = make_response(html)
    return resp

# the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    # Validate CSRF
    token = request.form.get('csrf_token', '')
    if not _validate_csrf(token):
        abort(400, description='Invalid CSRF token')

    # Validate input lengths and allowed characters (basic)
    username = request.form.get('username', '')[:150]
    password = request.form.get('password', '')[:128]

    if not username or not password:
        abort(400, description='Missing credentials')

    # simple username format check: allow letters, numbers, underscore, dash
    if not re.fullmatch(r'[A-Za-z0-9_\-]{1,150}', username):
        abort(400, description='Invalid username format')

    # check password using hashed value
    stored_hash = _users.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        # create a new session identifier safely and set username
        session.clear()
        session['username'] = username
        # rotate CSRF token post-login
        session['csrf_token'] = secrets.token_urlsafe(32)
        return redirect('/settings')
    else:
        # do not reveal whether username exists
        return 'Bad username or password', 401

# the home page
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
    Secure settings endpoint:
    - Requires authentication (ensure_logged_in)
    - Uses CSRF protection
    - Validates and sanitises all input
    - Limits input length to prevent buffer-style issues
    - Escapes all output to prevent XSS
    - Avoids insecure deserialization and does not expose sensitive info
    """
    username = g.user

    if request.method == 'GET':
        # produce a form with current settings (escaped)
        token = _generate_csrf()
        user_settings = _user_settings.get(username, {})
        display_name = escape(user_settings.get('display_name', ''))
        html = f'''
            <html>
              <body>
                <h1>Settings for {escape(username)}</h1>
                <form method="post" action="/settings">
                  <input type="hidden" name="csrf_token" value="{escape(token)}">
                  <label>Display name (max 64 chars):</label>
                  <input type="text" name="display_name" value="{display_name}" maxlength="64" required>
                  <button type="submit">Save</button>
                </form>
                <p><a href="/">Home</a></p>
              </body>
            </html>
        '''
        return make_response(html)

    # POST: validate CSRF first
    token = request.form.get('csrf_token', '')
    if not _validate_csrf(token):
        abort(400, description='Invalid CSRF token')

    # Extract and sanitize input
    raw_display_name = request.form.get('display_name', '')
    # enforce max length to prevent large payloads
    if len(raw_display_name) > 64:
        abort(400, description='Display name too long')

    # allow only a safe subset of characters (letters, numbers, spaces, - _ .)
    if not re.fullmatch(r"[A-Za-z0-9 \-_.]{1,64}", raw_display_name):
        abort(400, description='Invalid characters in display name')

    # final escape when storing/returning to avoid XSS on rendering
    safe_display_name = escape(raw_display_name)

    # store settings in a safe in-memory mapping (avoid insecure deserialization)
    _user_settings[username] = {'display_name': safe_display_name}

    # success response: do not leak session details or internal state
    html = f'''
        <html>
          <body>
            <p>Settings updated.</p>
            <p>Display name is now: {safe_display_name}</p>
            <p><a href="/settings">Back to settings</a></p>
          </body>
        </html>
    '''
    return make_response(html)
