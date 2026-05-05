# ...existing code...
import re
import secrets
from functools import wraps
from html import escape
from urllib.parse import urlparse

from flask import Flask, request, redirect, session, abort, make_response, url_for

# ...existing code...
app = Flask(__name__)
# Use a strong random secret key (do not check this into source control in real apps)
app.secret_key = secrets.token_urlsafe(32)

# Limit request body size to mitigate large payload attacks
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024  # 10 KB

# Simple CSP and other safety headers helper
def secure_response(body, status=200):
    resp = make_response(body, status)
    resp.headers['Content-Security-Policy'] = "default-src 'self'; object-src 'none'; frame-ancestors 'none';"
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['X-Frame-Options'] = 'DENY'
    return resp

def is_safe_redirect(target):
    # Only allow relative redirects (no scheme or netloc)
    parsed = urlparse(target)
    return (not parsed.scheme) and (not parsed.netloc)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get('username')
        if not username:
            # preserve next only if it's a safe relative URL
            next_url = request.path
            login_url = url_for('login')
            if is_safe_redirect(next_url):
                return redirect(login_url + '?next=' + next_url)
            return redirect(login_url)
        return f(*args, **kws)
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    # generate a short-lived csrf token for the login form
    csrf = secrets.token_urlsafe(32)
    session['csrf_token'] = csrf
    html = f'''
        <html><body>
        <form action="/login" method="post" autocomplete="off">
            <input type="hidden" name="csrf_token" value="{escape(csrf)}">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></body></html>
    '''
    return secure_response(html)

#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    # Basic CSRF protection
    form_csrf = request.form.get('csrf_token', '')
    session_csrf = session.get('csrf_token', '')
    if not (form_csrf and session_csrf and secrets.compare_digest(form_csrf, session_csrf)):
        abort(400)

    username = (request.form.get('username') or '').strip()
    password = (request.form.get('password') or '')

    # validate lengths to prevent large inputs
    if len(username) > 64 or len(password) > 128:
        abort(400)

    # For demo only: use a constant-time comparison; DO NOT use plaintext passwords in production.
    # Replace with proper password hashing and user store in real applications.
    if secrets.compare_digest(username, 'admin') and secrets.compare_digest(password, 'password'):
        session['username'] = username
        # safe redirect to next if provided and safe
        next_url = request.args.get('next') or request.form.get('next') or url_for('index')
        if is_safe_redirect(next_url):
            return redirect(next_url)
        return redirect(url_for('index'))
    else:
        return secure_response('Bad username or password', 401)

#the home page
@app.route('/')
def index():
    return secure_response('''<html>
    Hello! <a href='./login'>Login here</a>
    </html>''')

"""user settings page"""
#-copilot next line-

@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    """
    Secure settings page that allows a logged-in user to update a simple "display_name".
    - Validates and sanitizes all input.
    - Uses a CSRF token stored in session.
    - Limits input length and allowed characters to prevent XSS and other injection attacks.
    - Never deserializes user data.
    """
    # Generate or validate CSRF token
    if request.method == 'GET':
        csrf = secrets.token_urlsafe(32)
        session['settings_csrf'] = csrf
        # Show current stored display name (sanitised)
        current = session.get('display_name', '')
        safe_current = escape(current)
        html = f'''
            <html><body>
            <h1>Settings</h1>
            <form method="post" action="/settings" autocomplete="off">
                <input type="hidden" name="csrf_token" value="{escape(csrf)}">
                <label>Display name (max 50 chars, letters, numbers, space, - _ .):</label><br>
                <input type="text" name="display_name" value="{safe_current}" maxlength="50" required>
                <p><button type="submit">Save</button></p>
            </form>
            </body></html>
        '''
        return secure_response(html)

    # POST: process update
    form_csrf = request.form.get('csrf_token', '')
    session_csrf = session.get('settings_csrf', '')
    if not (form_csrf and session_csrf and secrets.compare_digest(form_csrf, session_csrf)):
        abort(400)

    # Get and sanitise input
    display_name = (request.form.get('display_name') or '').strip()

    # Enforce length limits
    if len(display_name) == 0 or len(display_name) > 50:
        return secure_response('Invalid display name length', 400)

    # Allow only a safe subset of characters to prevent XSS and injection
    if not re.match(r'^[A-Za-z0-9 _\-.]+$', display_name):
        return secure_response('Display name contains invalid characters', 400)

    # Final sanitisation before storage/display
    safe_name = escape(display_name)

    # Store only the safe, validated value in session (avoid storing sensitive data)
    session['display_name'] = safe_name

    html = f'''
        <html><body>
        <h1>Settings updated</h1>
        <p>Your display name has been updated.</p>
        <p><a href="/settings">Back to settings</a></p>
        </body></html>
    '''
    return secure_response(html)

# ...existing code...
if __name__ == '__main__':
    # For local development only. In production, run under a proper WSGI server and configure keys securely.
    app.run(debug=False, host='127.0.0.1', port=5000)
