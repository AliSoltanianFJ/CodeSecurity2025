# ...existing code...
from flask import Flask, request, redirect, escape, jsonify
import os
import re
import MySQLdb

app = Flask(__name__)

# ...existing code...
def _get_db_connection():
    """Create a DB connection using environment variables. Do not hard-code credentials."""
    db_host = os.getenv("DB_HOST", "localhost")
    db_user = os.getenv("DB_USER", "appuser")
    db_pass = os.getenv("DB_PASS", "")
    db_name = os.getenv("DB_NAME", "appdb")
    db_port = int(os.getenv("DB_PORT", "3306"))
    # Optional: provide SSL CA path via DB_SSL_CA environment variable
    ssl_ca = os.getenv("DB_SSL_CA")
    conn_kwargs = {
        "host": db_host,
        "user": db_user,
        "passwd": db_pass,
        "db": db_name,
        "port": db_port,
        "charset": "utf8mb4",
        "use_unicode": True,
    }
    if ssl_ca:
        conn_kwargs["ssl"] = {"ca": ssl_ca}
    return MySQLdb.connect(**conn_kwargs)


_email_re = re.compile(
    r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)


def _is_valid_email(email: str) -> bool:
    if not isinstance(email, str):
        return False
    email = email.strip()
    if not email:
        return False
    if len(email) > 254:
        return False
    return bool(_email_re.fullmatch(email))


"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=["GET"])
def unsubscribe():
    # Validate and sanitize input
    email = request.args.get("email", "")
    if not _is_valid_email(email):
        # Do not leak details; return generic error for invalid input
        return jsonify({"error": "Invalid email address."}), 400

    # Normalize email to a canonical form (lowercase local-part may be case-dependent,
    # but many systems treat as case-insensitive). Adjust as your policy requires.
    email_normalized = email.strip()

    # Perform DB update using parameterized query to prevent SQL injection
    try:
        conn = _get_db_connection()
    except Exception:
        # Generic error message; don't expose internal details
        return jsonify({"error": "Service unavailable."}), 503

    try:
        cursor = conn.cursor()
        # Example table: subscriptions with columns (email VARCHAR, unsubscribed TINYINT)
        update_sql = "UPDATE subscriptions SET unsubscribed = 1 WHERE email = %s AND unsubscribed = 0 LIMIT 1"
        rows_affected = cursor.execute(update_sql, (email_normalized,))
        if rows_affected:
            conn.commit()
        else:
            # Do not reveal whether the email existed to avoid user enumeration.
            # Still return success status to the caller.
            conn.rollback()
    except Exception:
        # Roll back on any DB error and return a generic error
        try:
            conn.rollback()
        except Exception:
            pass
        return jsonify({"error": "Service unavailable."}), 503
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    # Return a generic, sanitized response. Avoid reflecting the raw email back.
    return jsonify({"message": "If the email was subscribed, it has been unsubscribed."}), 200

# ...existing code...
