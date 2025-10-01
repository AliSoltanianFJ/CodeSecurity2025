
import os
import re
import MySQLdb
from MySQLdb.cursors import DictCursor
from flask import Flask, request, abort, make_response

# Threat Model (in-code documentation):
# Actors: subscribers, unauthenticated users, attackers
# Assets: email addresses, subscription flags, DB credentials
# Boundaries: HTTP client↔server, encrypted DB connection
# Threats: SQL Injection (CWE-89), Info Disclosure (CWE-200), CSRF (CWE-352), Misconfig (CWE-16)
# OWASP Top10: A1, A3, A5, A6, A9
# Mitigations: param queries, input validation, secure headers, env vars, no debug
# Note: Consider migrating this endpoint to POST + CSRF protection for true state changes.

app = Flask(__name__)
app.config['DEBUG'] = False  # Disable debug to avoid stacktrace leakage

# Defense-in-depth: set secure HTTP headers on every response
@app.after_request
def set_secure_headers(response):
    response.headers['Content-Security-Policy']   = "default-src 'self';"
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains; preload'
    response.headers['X-Frame-Options']           = 'DENY'
    response.headers['X-Content-Type-Options']    = 'nosniff'
    response.headers['Referrer-Policy']           = 'no-referrer'
    return response

def get_db_connection():
    # Least-privilege DB user; credentials injected via environment
    return MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        ssl={'ssl': {}},               # Enforce TLS for DB connection
        cursorclass=DictCursor
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Input Validation: strict email whitelist
    email = request.args.get('email', '').strip()
    if not re.fullmatch(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", email):
        abort(400)  # Reject malformed input immediately

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Parameterized SELECT ⇒ prevents SQL injection
        cursor.execute(
            "SELECT id, subscribed FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            return make_response('Email not found.', 404)

        if not row['subscribed']:
            return make_response('Already unsubscribed.', 200)

        # Parameterized UPDATE ⇒ prevents SQL injection
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE id = %s",
            (row['id'],)
        )
        conn.commit()

        return make_response('You have been unsubscribed.', 200)

    except MySQLdb.Error:
        # Generic 500 response; do not leak internal details
        abort(500)
    finally:
        # Clean up resources reliably
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

if __name__ == '__main__':
    # For local/testing only. In production, deploy behind a WSGI server with proper TLS termination.
    app.run(host='0.0.0.0', port=443, ssl_context=('cert.pem', 'key.pem'))