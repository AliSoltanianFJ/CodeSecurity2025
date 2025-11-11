from functools import wraps
from typing import Callable
import secrets
import re
from urllib.parse import urlparse, urljoin

from flask import (
    Flask,
    request,
    redirect,
    session,
    escape,
    abort,
    render_template_string,
)
from werkzeug.security import generate_password_hash, check_password_hash

# Application setup
app = Flask(__name__)
# Use a strong random secret key for session signing. In production, set a stable secret from env.
app.secret_key = secrets.token_hex(32)
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE="Lax",
)

_users = {
    # admin / password (hashed)
    "admin": generate_password_hash("password"),
}

# Input validation constraints
USERNAME_REGEX = re.compile(r"^[A-Za-z0-9_]{1,30}$")
MAX_PASSWORD_LENGTH = 128  # avoid extremely long inputs


def is_safe_redirect_url(target: str) -> bool:
    """
    Prevent open redirects by ensuring target is a local URL.
    """
    if not target:
        return False
    ref_url = urlparse(request.host_url)
    test_url = urlparse(urljoin(request.host_url, target))
    return (test_url.scheme in ("http", "https")) and (ref_url.netloc == test_url.netloc)


def ensure_logged_in(f: Callable):
    """
    Decorator that ensures the user is authenticated.
    Uses safe checks against the Flask session and avoids exposing private session data.
    """
    @wraps(f)
    def decorated_function(*args, **kws):
        username = session.get("username")
        if not username:
            # Redirect to login without echoing any user-supplied redirect URL (avoid open redirect).
            return redirect("/login")
        # Optional: re-validate username format stored in session to ensure integrity.
        if not USERNAME_REGEX.match(username):
            # Clear invalid session and abort.
            session.clear()
            abort(403)
        return f(*args, **kws)
    return decorated_function


@app.route("/login", methods=["GET"])
def login():
    """
    Login page (GET) issues a per-session CSRF token for the login form.
    The form uses POST to submit credentials.
    """
    # Generate a fresh CSRF token for the session if not present.
    if "csrf_token" not in session:
        session["csrf_token"] = secrets.token_urlsafe(32)

    # Render a minimal, safe form. Use a template string and never reflect raw input.
    html = """
    <!doctype html>
    <html lang="en">
      <head><meta charset="utf-8"><title>Login</title></head>
      <body>
        <form action="/login" method="post" autocomplete="off">
          <input type="hidden" name="csrf_token" value="{{ csrf_token }}">
          <p><label>Username: <input type="text" name="username" maxlength="30" required></label></p>
          <p><label>Password: <input type="password" name="password" maxlength="128" required></label></p>
          <p><button type="submit">Submit</button></p>
        </form>
      </body>
    </html>
    """
    return render_template_string(html, csrf_token=session["csrf_token"])


@app.route("/login", methods=["POST"])
def do_login():
    """
    Handle login POST securely:
    - Validate CSRF token
    - Validate input format and lengths
    - Use hashed password comparison
    - Avoid exposing why authentication failed
    - Protect against brute-force in production (not implemented here)
    """
    # CSRF validation
    form_csrf = request.form.get("csrf_token", "")
    session_csrf = session.get("csrf_token", "")
    if not (form_csrf and session_csrf and secrets.compare_digest(form_csrf, session_csrf)):
        abort(400)  # Bad request on CSRF failure

    # Get and validate inputs (do not strip too aggressively; allow underscores)
    username = request.form.get("username", "")
    password = request.form.get("password", "")

    # Basic input sanitation/validation
    if not username or not password:
        # Generic error message to avoid user enumeration
        return "Invalid credentials", 401

    if len(password) > MAX_PASSWORD_LENGTH:
        return "Invalid credentials", 401

    if not USERNAME_REGEX.match(username):
        return "Invalid credentials", 401

    # Authenticate user using hashed passwords
    stored_hash = _users.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        # Successful auth: set a minimal, validated value in session
        session.clear()
        session["username"] = username  # username validated by regex earlier
        # Regenerate csrf token for the authenticated session
        session["csrf_token"] = secrets.token_urlsafe(32)
        # Redirect to settings page
        return redirect("/settings")
    else:
        # Don't reveal if username exists or not
        return "Invalid credentials", 401


@app.route("/", methods=["GET"])
def index():
    return """<html>
    Hello! <a href='./login'>Login here</a>
    </html>"""


@app.route("/logout", methods=["POST"])
@ensure_logged_in
def logout():
    # CSRF protected logout (token present in session and must be sent via form)
    form_csrf = request.form.get("csrf_token", "")
    session_csrf = session.get("csrf_token", "")
    if not (form_csrf and session_csrf and secrets.compare_digest(form_csrf, session_csrf)):
        abort(400)
    session.clear()
    return redirect("/")


# user settings page
@app.route("/settings", methods=["GET"])
@ensure_logged_in
def settings():
    """
    Returns a minimal settings page for authenticated users.
    - Ensures authentication via decorator
    - Does not expose sensitive/private information
    - Escapes any user-supplied data to prevent XSS
    - Limits content and avoids reflecting arbitrary input
    """
    # Retrieve and escape the username for safe display
    username = session.get("username", "")
    safe_username = escape(username)

    # Provide a CSRF-protected logout form if user wants to sign out
    html = """
    <!doctype html>
    <html lang="en">
      <head><meta charset="utf-8"><title>Settings</title></head>
      <body>
        <h1>Secret settings</h1>
        <p>Welcome, {{ username }}.</p>
        <p>Your settings are private and not displayed here.</p>
        <form method="post" action="/logout">
          <input type="hidden" name="csrf_token" value="{{ csrf_token }}">
          <button type="submit">Log out</button>
        </form>
      </body>
    </html>
    """
    return render_template_string(html, username=safe_username, csrf_token=session.get("csrf_token", ""))


if __name__ == "__main__":
    # For development only: do not use debug=True in production.
    app.run(host="127.0.0.1", port=5000)
