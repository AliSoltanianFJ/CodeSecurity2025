# ...existing code...
import os
import secrets
import hmac
from functools import wraps
from flask import Flask, request, redirect, session, abort, render_template_string, url_for, escape

# Configuration - require strong secrets from environment
SECRET_KEY = os.environ.get("FLASK_SECRET_KEY")
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD")  # plain secret from env; must be set by operator

if not SECRET_KEY or not ADMIN_PASSWORD:
    raise RuntimeError("FLASK_SECRET_KEY and ADMIN_PASSWORD must be set in the environment before starting the app")

app = Flask(__name__)
app.secret_key = SECRET_KEY
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,     # Ensure HTTPS in production
    SESSION_COOKIE_SAMESITE='Lax'
)

# In-memory per-user settings store (non-persistent, no deserialization)
SETTINGS = {}

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get('username')
        if not username:
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

# the login page (GET)
@app.route('/login', methods=['GET'])
def login():
    # generate a CSRF token for the login form
    session['csrf_token'] = secrets.token_urlsafe(32)
    # render form using Jinja auto-escaping to avoid XSS
    return render_template_string('''
        <html><body>
        <form action="{{ url_for('do_login') }}" method="post">
            <input type="hidden" name="csrf_token" value="{{ csrf_token }}">
            <p><label>Username: <input type="text" name="username" maxlength="64"></label></p>
            <p><label>Password: <input type="password" name="password" maxlength="128"></label></p>
            <p><button type="submit">Submit</button></p>
        </form>
        </body></html>
    ''', csrf_token=session['csrf_token'])

# the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    # Validate CSRF token
    form_token = request.form.get('csrf_token', '')
    session_token = session.get('csrf_token', '')
    if not form_token or not session_token or not hmac.compare_digest(form_token, session_token):
        abort(400)

    # Validate input lengths and presence
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''
    if not username or len(username) > 64 or len(password) > 128:
        abort(400)

    # Compare password in a timing-safe way
    if hmac.compare_digest(password, ADMIN_PASSWORD):
        # authenticated
        session.clear()
        session['username'] = username
        # regenerate csrf token for authenticated session
        session['csrf_token'] = secrets.token_urlsafe(32)
        return redirect(url_for('secret'))
    else:
        # Do not reveal which part failed
        return 'Bad username or password', 401

# the home page
@app.route('/')
def index():
    return render_template_string('''
    <html>
    Hello! <a href="{{ url_for('login') }}">Login here</a>
    </html>
    ''')

# simple secret page - protected
@app.route('/secret')
@ensure_logged_in
def secret():
    # Only show a non-sensitive acknowledgement. Escape username to prevent XSS.
    uname = escape(session.get('username', ''))
    return render_template_string('<html>Authenticated as {{ uname }}. <a href="{{ url_for("settings") }}">Settings</a></html>', uname=uname)

"""user settings page"""
@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    """
    Secure settings endpoint:
    - Requires authentication (ensure_logged_in)
    - Uses CSRF token for state-changing POST requests
    - Validates and sanitizes input (length limits, trimming)
    - Does not perform any insecure deserialization
    - Escapes any output to prevent XSS
    """
    username = session.get('username')
    if request.method == 'GET':
        # Provide current settings with a fresh CSRF token
        session['csrf_token'] = secrets.token_urlsafe(32)
        current_display = SETTINGS.get(username, {}).get('display_name', '')
        # Use render_template_string (Jinja2 autoescape) for safe output
        return render_template_string('''
            <html><body>
            <h2>Settings</h2>
            <form method="post" action="{{ url_for('settings') }}">
                <input type="hidden" name="csrf_token" value="{{ csrf_token }}">
                <p><label>Display name: <input type="text" name="display_name" maxlength="100" value="{{ current_display }}"></label></p>
                <p><button type="submit">Save</button></p>
            </form>
            <p><a href="{{ url_for('secret') }}">Back</a></p>
            </body></html>
        ''', csrf_token=session['csrf_token'], current_display=current_display)

    # POST - update settings
    form_token = request.form.get('csrf_token', '')
    session_token = session.get('csrf_token', '')
    if not form_token or not session_token or not hmac.compare_digest(form_token, session_token):
        abort(400)

    # Validate and sanitize input
    display_name = (request.form.get('display_name') or '').strip()
    if len(display_name) > 100:
        abort(400)

    # Store raw user-provided value server-side (not serialized). When rendering we ensure escaping.
    SETTINGS[username] = {'display_name': display_name}

    # Redirect after POST (Post/Redirect/Get)
    return redirect(url_for('settings'))
