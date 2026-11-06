import os
from functools import wraps

from flask import Flask, request, abort, make_response
from flask_talisman import Talisman
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
from email_validator import validate_email, EmailNotValidError
import MySQLdb

# ------------------------------------------------------------------------------
# Configuration and Initialization
# ------------------------------------------------------------------------------

# Load critical configuration from environment, fail fast if missing
DB_HOST      = os.getenv('DB_HOST')
DB_PORT      = int(os.getenv('DB_PORT', '3306'))
DB_NAME      = os.getenv('DB_NAME')
DB_USER      = os.getenv('DB_USER')
DB_PASSWORD  = os.getenv('DB_PASSWORD')
# SECRET_KEY is used both for Flask session security and for signing unsubscribe tokens.
SECRET_KEY   = os.getenv('SECRET_KEY')
# Token expiration in seconds (e.g. 7 days)
TOKEN_MAX_AGE = int(os.getenv('TOKEN_MAX_AGE', str(7 * 24 * 3600)))

required_env = {
    'DB_HOST': DB_HOST,
    'DB_NAME': DB_NAME,
    'DB_USER': DB_USER,
    'DB_PASSWORD': DB_PASSWORD,
    'SECRET_KEY': SECRET_KEY
}
missing = [k for k, v in required_env.items() if not v]
if missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(missing)}")

app = Flask(__name__)
# Use a secure random secret key for session cookies and CSRF protection
app.config['SECRET_KEY'] = SECRET_KEY
# Never run in debug mode in production
app.config['DEBUG'] = False

# Enforce security headers: HSTS, CSP, X-Frame-Options, etc.
# The default CSP here is quite restrictive; adapt as needed.
csp = {
    'default-src': '\'self\'',
    'img-src':    '\'self\' data:',
    'script-src': '\'self\'',
    'style-src':  '\'self\''
}
Talisman(app,
         content_security_policy=csp,
         force_https=True,
         strict_transport_security=True,
         strict_transport_security_preload=True,
         strict_transport_security_max_age=31536000)

# Serializer for URL-safe, timestamped tokens
serializer = URLSafeTimedSerializer(
    secret_key=SECRET_KEY,
    salt='unsubscribe-email-salt'  # distinct salt for this purpose
)

# ------------------------------------------------------------------------------
# Utility decorators and functions
# ------------------------------------------------------------------------------

def require_https(f):
    """
    Ensures that the request is over HTTPS. Talisman already enforces this,
    but the decorator provides a secondary check.
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        if not request.is_secure:
            abort(403)  # Forbidden if not over TLS
        return f(*args, **kwargs)
    return decorated

def get_db_connection():
    """
    Returns a new MySQL database connection using least-privilege credentials
    and SSL if configured. Caller is responsible for closing the connection.
    """
    conn = MySQLdb.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        ssl={'ca': os.getenv('DB_SSL_CA_PATH')}  # optional SSL CA path
    )
    # Ensure autocommit off; we will commit explicitly
    conn.autocommit(False)
    return conn

# ------------------------------------------------------------------------------
# Unsubscribe Endpoint
# ------------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
@require_https
def unsubscribe():
    """
    Securely handles unsubscribe requests. Expects two GET parameters:
      1) email: the user's email address
      2) token: a time-limited HMAC signature over the email
    Only if both validate correctly do we proceed to mark the user as unsubscribed.
    """

    # 1) Extract and validate parameters
    email_param = request.args.get('email', '').strip()
    token_param = request.args.get('token', '').strip()
    if not email_param or not token_param:
        # Do not reveal which parameter was missing
        abort(400)  # Bad Request

    # 2) Validate email syntax with a robust library (defeats regex bypasses)
    try:
        v = validate_email(email_param)
        email = v.email  # normalized form
    except EmailNotValidError as e:
        abort(400)

    # 3) Verify token with a time limit to prevent replay attacks
    try:
        recovered_email = serializer.loads(token_param, max_age=TOKEN_MAX_AGE)
    except SignatureExpired:
        # Give the user a generic message
        return make_response("Your unsubscribe link has expired. Please request a new one.", 410)
    except BadSignature:
        abort(400)

    # Ensure the token actually corresponds to the email parameter
    if recovered_email != email:
        abort(400)

    # 4) Perform the database update inside a transaction
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # Use a parameterized query to prevent SQL injection
        update_sql = """
            UPDATE email_subscriptions
            SET subscribed = 0, unsubscribed_at = NOW()
            WHERE email = %s AND subscribed = 1
        """
        cursor.execute(update_sql, (email,))
        rows_affected = cursor.rowcount
        conn.commit()
        cursor.close()
        conn.close()
    except Exception as e:
        abort(500)

    # 5) Render a minimal, auto-escaped response (no user input echoed)
    if rows_affected:
        response_body = "You have been unsubscribed. We're sorry to see you go."
    else:
        # If the email was not found or already unsubscribed, do not reveal which
        response_body = "If that email was subscribed, it has now been unsubscribed."

    # Return a plain text response to avoid XSS risks
    response = make_response(response_body, 200)
    response.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return response

# ------------------------------------------------------------------------------
# Token Generation Helper (for your email-sending system)
# ------------------------------------------------------------------------------
def generate_unsubscribe_token(email_address):
    """
    Generates a URL-safe, timestamped token for the given email.
    This function would be used when composing outbound emails.
    """
    # Always pass the normalized form
    try:
        v = validate_email(email_address)
        normalized = v.email
    except EmailNotValidError:
        raise ValueError("Cannot generate token for invalid email")
    return serializer.dumps(normalized)

# ------------------------------------------------------------------------------
# Error Handlers (do not expose stack traces)
# ------------------------------------------------------------------------------
@app.errorhandler(400)
def bad_request(error):
    return make_response("Bad request.", 400)

@app.errorhandler(403)
def forbidden(error):
    return make_response("Access forbidden.", 403)

@app.errorhandler(410)
def gone(error):
    # Handled explicitly in unsubscribe()
    return make_response("Resource gone.", 410)

@app.errorhandler(500)
def internal_error(error):
    return make_response("An internal error occurred. Please try again later.", 500)
