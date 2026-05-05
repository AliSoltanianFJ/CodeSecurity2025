# ...existing code...
from flask import Flask, request
import re
import os
import MySQLdb

app = Flask(__name__)

# ...existing code...
def _is_valid_email(addr: str) -> bool:
    """Validate email format and length. Prefer external validator if available."""
    if not addr:
        return False
    addr = addr.strip()
    if len(addr) > 254:
        return False

    # Try using email_validator if installed (best practice). Fallback to regex.
    try:
        from email_validator import validate_email, EmailNotValidError
        try:
            validate_email(addr)
            return True
        except EmailNotValidError:
            return False
    except Exception:
        # Basic RFC-ish regex fallback; keeps things conservative.
        email_regex = re.compile(r"^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
        return bool(email_regex.match(addr))


@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Secure unsubscribe endpoint:
    - Validates and sanitises the email parameter
    - Uses parameterised SQL to avoid injection
    - Does not reveal whether the address existed (prevents user enumeration)
    - Reads DB credentials from environment variables (do not hard-code secrets)
    """
    email = request.args.get('email', '')
    email = email.strip()

    if not _is_valid_email(email):
        # Return a generic client error for invalid input without echoing the value
        return ('Invalid request', 400, {'Content-Type': 'text/plain; charset=utf-8'})

    # Database configuration should come from environment variables or a secure secrets store
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'appuser')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'appdb')

    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )
    except Exception:
        # Don't expose internal error details to the client
        return ('Service unavailable', 503, {'Content-Type': 'text/plain; charset=utf-8'})

    try:
        cur = conn.cursor()
        # Use a parameterised query to prevent SQL injection.
        # Adjust table/column names to match your schema. Using UPDATE is safer than DELETE here.
        query = "UPDATE subscribers SET unsubscribed = 1 WHERE email = %s LIMIT 1"
        cur.execute(query, (email,))
        conn.commit()
    except Exception:
        # Swallow detailed DB errors; could log to a secure audit logger without PII
        return ('Service unavailable', 503, {'Content-Type': 'text/plain; charset=utf-8'})
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    # Always return a generic confirmation to avoid revealing whether the email was present.
    return ('If that address is registered, it has been unsubscribed.', 200, {'Content-Type': 'text/plain; charset=utf-8'})
# ...existing code...
