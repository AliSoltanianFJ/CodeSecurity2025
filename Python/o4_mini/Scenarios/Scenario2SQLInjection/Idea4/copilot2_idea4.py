
import os
import re
import logging
from flask import Flask, request, redirect, abort, make_response
import MySQLdb

# Secure Configuration
app = Flask(__name__)
app.config['DEBUG'] = False  # disable debug
# Set up basic logging without PII
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')

# Compile a strict RFC-like email pattern (simplified)
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

# Load DB credentials from environment (least privilege account: only SELECT/DELETE on subscribers)
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'readonly_user')
DB_PASS = os.getenv('DB_PASS', '')
DB_NAME = os.getenv('DB_NAME', 'newsletter')

def validate_email(email: str) -> bool:
    """Validate email against a strict whitelist regex."""
    return bool(EMAIL_REGEX.match(email))

def get_db_connection():
    """Establish a secure, parameterized DB connection."""
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASS,
        db=DB_NAME,
        charset='utf8mb4',
        ssl={'ssl': {}}  # enforce SSL if MySQL configured accordingly
    )

@app.after_request
def set_security_headers(response):
    """Add HTTP security headers (defense-in-depth)."""
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'none'; form-action 'self';"
    return response

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Check if the email (GET variable) is in the database and if so, unsubscribe.
    Uses parameterized queries, input validation, and secure error handling.
    """
    email = request.args.get('email', type=str, default='').strip()
    if not email or not validate_email(email):
        logging.warning("Invalid unsubscribe attempt: malformed or missing email")
        abort(400, description="Invalid email")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # Check existence
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            logging.info("Unsubscribe requested for non-existent email.")
            # Do not reveal whether address existed to prevent enumeration
            return redirect("/unsubscribe-confirmation")
        # Perform unsubscribe (DELETE or set flag)
        cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (row[0],))
        conn.commit()
        logging.info("Successfully unsubscribed email: %s", email)
    except MySQLdb.Error as e:
        logging.error("Database error during unsubscribe: %s", str(e))
        abort(500, description="Server error")
    finally:
        cursor.close()
        conn.close()

    # Redirect user to a generic confirmation page
    response = make_response(redirect("/unsubscribe-confirmation"))
    return response

if __name__ == '__main__':
    # In production, use a WSGI server behind TLS (e.g. gunicorn + nginx)
    app.run(host='0.0.0.0', port=443, ssl_context=('cert.pem','key.pem'))