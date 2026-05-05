# ...existing code...
import re
import hmac
import hashlib
import logging

# ...existing code...

@app.route('/unsubscribe')
def unsubscribe():
    """
    Secure unsubscribe:
    - Expects GET params: email, token
    - token = HMAC_SHA256(secret, email_lower)
    - Responds with a generic message to avoid enumeration
    """
    # Configure logger
    logger = logging.getLogger('unsubscribe')
    try:
        # Read and validate inputs
        email = request.args.get('email', '', type=str).strip()
        token = request.args.get('token', '', type=str).strip()

        # Basic limits to prevent oversized input/buffer issues
        if not email or len(email) > 254 or not token or len(token) > 256:
            return ("If the email was subscribed, it has been removed."), 200

        # Conservative email validation (avoid complex regex that can be exploited)
        if not re.match(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$', email):
            return ("If the email was subscribed, it has been removed."), 200

        email_lower = email.lower()

        # Verify HMAC token to prevent CSRF and arbitrary removals.
        secret = os.environ.get('UNSUBSCRIBE_SECRET')
        if not secret:
            logger.error('UNSUBSCRIBE_SECRET not set in environment')
            return ("Service temporarily unavailable."), 503

        expected = hmac.new(secret.encode('utf-8'), email_lower.encode('utf-8'), hashlib.sha256).hexdigest()
        # constant-time comparison
        if not hmac.compare_digest(expected, token):
            # Do not reveal which part failed
            return ("If the email was subscribed, it has been removed."), 200

        # Database operation: parameterised query to prevent SQL injection
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASSWORD')
        db_name = os.environ.get('DB_NAME')
        if not (db_user and db_pass and db_name):
            logger.error('Database credentials not set in environment')
            return ("Service temporarily unavailable."), 503

        conn = None
        cursor = None
        try:
            conn = MySQLdb.connect(host=db_host,
                                   user=db_user,
                                   passwd=db_pass,
                                   db=db_name,
                                   charset='utf8mb4',
                                   use_unicode=True,
                                   connect_timeout=5)
            cursor = conn.cursor()
            # Example: mark unsubscribed = 0 or delete; use parameterised queries
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email_lower,))
            conn.commit()
            # Do not reveal whether any row was changed; generic response prevents enumeration
            return ("If the email was subscribed, it has been removed."), 200
        except Exception as e:
            logger.exception("Database error during unsubscribe")
            # Do not leak internal errors to the client
            return ("Service temporarily unavailable."), 503
        finally:
            if cursor:
                try:
                    cursor.close()
                except Exception:
                    pass
            if conn:
                try:
                    conn.close()
                except Exception:
                    pass
    except Exception:
        # Catch-all to ensure no sensitive trace leaks
        logger.exception("Unexpected error in unsubscribe endpoint")
        return ("Service temporarily unavailable."), 503
# ...existing code...
