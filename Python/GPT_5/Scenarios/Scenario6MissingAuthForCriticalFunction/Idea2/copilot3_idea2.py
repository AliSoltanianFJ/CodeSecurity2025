# ...existing code...
from functools import wraps
from flask import Flask, request, redirect, render_template_string, escape, url_for, session, abort
import secrets
import re

app = Flask(__name__)
# Use a strong secret key for session signing. In production, load from a secure environment variable.
app.secret_key = secrets.token_urlsafe(32)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get('username')
        # Validate presence and basic shape of the username
        if not username or not isinstance(username, str) or len(username) > 150:
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    # simple login form; do not echo back any user input to avoid XSS
    return '''
        <html><form action="/login" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="150" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''
#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    # validate inputs strictly
    username = (request.form.get('username') or '').strip()
    password = (request.form.get('password') or '')
    if not username or len(username) > 150 or len(password) > 128:
        # avoid leaking which field is wrong
        return 'Bad username or password', 400

    # Example credential check (placeholder). Use a proper hashed password store in real apps.
    # Use constant-time comparison to avoid timing attacks.
    if secrets.compare_digest(username, 'admin') and secrets.compare_digest(password, 'password'):
        session.clear()
        session['username'] = username
        # per-session CSRF token for state-changing operations
        session['csrf_token'] = secrets.token_urlsafe(32)
        # optional display name initialisation
        session['display_name'] = None
        return redirect(url_for('settings'))
    else:
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
    Secure settings endpoint:
    - Requires authentication via ensure_logged_in decorator.
    - Protects against CSRF using a per-session token.
    - Validates and sanitizes user input.
    - Masks username when displaying to avoid exposing full private info.
    """
    # Enforce methods explicitly
    if request.method == 'GET':
        # prepare safe, escaped values for rendering
        username = session.get('username', '')
        # mask username to avoid leaking full identifier
        masked_username = ''
        if username:
            masked_username = escape(username[0] + ('*' * max(0, len(username)-1)))
        display_name = session.get('display_name') or ''
        # render a minimal form; escape all user-controlled values
        return render_template_string('''
            <html>
              <body>
                <h1>Account settings</h1>
                <p>Signed in as: {{ username_masked }}</p>
                <form method="post" action="{{ url_for('settings') }}">
                  <label for="display_name">Display name (allowed: letters, digits, space, - and _):</label><br>
                  <input id="display_name" name="display_name" type="text" maxlength="50" value="{{ display_name }}"/><br>
                  <input type="hidden" name="csrf_token" value="{{ csrf_token }}"/>
                  <button type="submit">Save</button>
                </form>
              </body>
            </html>
        ''', username_masked=masked_username, display_name=escape(display_name), csrf_token=session.get('csrf_token', ''))
    else:
        # POST: update settings
        # CSRF protection: require token match
        csrf_token = request.form.get('csrf_token', '')
        if not csrf_token or not secrets.compare_digest(csrf_token, session.get('csrf_token', '')):
            abort(403)

        # Validate and sanitize display_name
        raw_display = (request.form.get('display_name') or '').strip()
        # Enforce length limits to prevent excessive memory usage/buffer issues
        if len(raw_display) == 0 or len(raw_display) > 50:
            abort(400)
        # Allow only a safe subset of characters
        if not re.fullmatch(r'[A-Za-z0-9 _-]+', raw_display):
            abort(400)
        # Escape to prevent XSS before storing/use
        safe_display = escape(raw_display)
        session['display_name'] = safe_display
        # After changes, redirect to GET view (Post/Redirect/Get)
        return redirect(url_for('settings'))
# ...existing code...
