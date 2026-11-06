import os
import logging
from flask import Flask, request, render_template, abort
from flask_wtf import FlaskForm, CSRFProtect
from wtforms import HiddenField
from wtforms.validators import DataRequired, Email
from email_validator import validate_email, EmailNotValidError
import MySQLdb
from flask_talisman import Talisman

# -----------------------------------------------------------------------------
# Configuration and Initialization
# -----------------------------------------------------------------------------

# Ensure that all required environment variables are set
required_env = ['SECRET_KEY', 'DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME']
missing = [var for var in required_env if not os.getenv(var)]
if missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(missing)}")

# Create Flask application
app = Flask(__name__, static_folder=None)  # no automatic static serving
# Secret key for session cookies and CSRF tokens—must be strong and unpredictable
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY')

# Enforce HTTPS, set HSTS, enable a strict Content Security Policy
Talisman(
    app,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,  # 1 year
    content_security_policy={
        'default-src': ["'self'"],
        'script-src': ["'self'"],
        'style-src': ["'self'"],
        'object-src': ["'none'"],
        'form-action': ["'self'"],
    }
)

# Enable CSRF protection for all POST endpoints
csrf = CSRFProtect(app)

# Configure logging
# Only warning and above are emitted in production
logging.basicConfig(level=logging.WARNING,
                    format='%(asctime)s %(levelname)s %(message)s',
                    handlers=[logging.StreamHandler()])

# Database connection parameters (least privilege user)
DB_CONFIG = {
    'host':     os.getenv('DB_HOST'),
    'user':     os.getenv('DB_USER'),
    'passwd':   os.getenv('DB_PASSWORD'),
    'db':       os.getenv('DB_NAME'),
    'charset':  'utf8mb4',
    'use_unicode': True,
    'connect_timeout': 5,   # fail fast
}

# -----------------------------------------------------------------------------
# Forms
# -----------------------------------------------------------------------------

class UnsubscribeForm(FlaskForm):
    # We include only the normalized email in a hidden field;
    # CSRF token is injected automatically by FlaskForm
    email = HiddenField(validators=[DataRequired(), Email()])

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

def get_db_connection():
    """
    Create and return a new database connection using least-privilege credentials.
    Caller is responsible for closing connection and cursor.
    """
    return MySQLdb.connect(**DB_CONFIG)

def is_valid_email(addr):
    """
    Validate and normalize email address.
    Returns normalized address on success, raises EmailNotValidError on failure.
    """
    v = validate_email(addr)  # uses RFC-compliant checks and DNS lookups by default
    return v.email  # normalized (lowercased, unicode-normalized)

# -----------------------------------------------------------------------------
# Routes
# -----------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe_confirm():
    """
    Step 1: Present the user with an unsubscribe confirmation form.
    This prevents CSRF since the actual unsubscribe is only performed via POST.
    """
    raw_email = request.args.get('email', '')
    try:
        email = is_valid_email(raw_email)
    except EmailNotValidError:
        # Do not reveal validation details
        return render_template('unsubscribe_invalid.html'), 400

    form = UnsubscribeForm()
    # Embed normalized, validated email in the form
    form.email.data = email

    # Render a template that auto-escapes all user-provided content
    return render_template(
        'unsubscribe_confirm.html',
        form=form
    ), 200

@app.route('/unsubscribe', methods=['POST'])
def unsubscribe_action():
    """
    Step 2: Process the unsubscribe request.
    Must be POST with valid CSRF token and a normalized email address.
    """
    form = UnsubscribeForm()
    if not form.validate_on_submit():
        # Either missing CSRF token, invalid email format, or missing field
        abort(400)

    raw_email = form.email.data
    try:
        email = is_valid_email(raw_email)
    except EmailNotValidError:
        abort(400)

    # Perform the unsubscribe in the database
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Parameterized UPDATE to avoid SQL injection
        # The user account behind DB_USER must only have UPDATE permissions on subscribers table
        update_sql = """
            UPDATE subscribers
            SET unsubscribed = 1,
                unsubscribed_at = UTC_TIMESTAMP()
            WHERE email = %s
              AND unsubscribed = 0
        """
        cursor.execute(update_sql, (email,))
        conn.commit()
    except MySQLdb.MySQLError:
        # Log the exception internally without exposing details to the client
        logging.exception("Database error when updating unsubscribe status")
        abort(500)
    finally:
        # Clean up resources; ignore errors on close
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # Render a success page confirming unsubscribe
    return render_template('unsubscribe_success.html'), 200

# -----------------------------------------------------------------------------
# Error Handlers
# -----------------------------------------------------------------------------

@app.errorhandler(400)
def handle_bad_request(e):
    # Generic message; no debug info leaked
    return render_template('error.html', code=400, message="Bad request"), 400

@app.errorhandler(404)
def handle_not_found(e):
    return render_template('error.html', code=404, message="Not found"), 404

@app.errorhandler(500)
def handle_server_error(e):
    # Log exception if not already logged
    logging.exception("Internal server error")
    return render_template('error.html', code=500,
                           message="An unexpected error occurred. Please try again later."), 500

# -----------------------------------------------------------------------------
# Templates (to be placed in your templates/ directory)
# -----------------------------------------------------------------------------
"""
unsubscribe_confirm.html
------------------------
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Confirm Unsubscribe</title>
</head>
<body>
  <h1>Unsubscribe</h1>
  <p>You are about to unsubscribe: {{ form.email.data }}.</p>
  <form method="post" action="/unsubscribe">
    {{ form.csrf_token }}
    {{ form.email }}
    <button type="submit">Confirm Unsubscribe</button>
  </form>
</body>
</html>

unsubscribe_success.html
------------------------
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Unsubscribed</title></head>
<body>
  <h1>You have been unsubscribed</h1>
  <p>If this was a mistake, please contact support.</p>
</body>
</html>

unsubscribe_invalid.html
------------------------
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Invalid Request</title></head>
<body>
  <h1>Invalid unsubscribe link</h1>
  <p>The link you used is invalid. Please check your email or contact support.</p>
</body>
</html>

error.html
----------
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Error {{ code }}</title></head>
<body>
  <h1>Error {{ code }}</h1>
  <p>{{ message }}</p>
</body>
</html>
"""