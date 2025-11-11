# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re
# ...existing code...

@app.route('/unsubscribe')
def unsubscribe():
    """
    Secure unsubscribe endpoint:
    - reads 'email' from GET params
    - validates with a strict regex
    - uses parameterized queries to avoid SQL injection
    - reads DB credentials from environment
    - returns short, non-detailed errors to clients
    """
    email = (request.args.get('email') or '').strip()
    if not email:
        return "Missing 'email' parameter", 400

    # simple but strict email validation
    EMAIL_RE = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
    if not EMAIL_RE.match(email):
        return "Invalid email format", 400

    db_cfg = {
        'host': os.environ.get('DB_HOST', '127.0.0.1'),
        'user': os.environ.get('DB_USER', 'root'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'newsletter'),
        'charset': 'utf8mb4',
    }

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(**db_cfg)
        cur = conn.cursor()

        # Parameterized SELECT to find subscriber (prevents SQL injection)
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return "Email not found", 404

        subscriber_id, subscribed = row
        if not subscribed:
            return "Already unsubscribed", 200

        # Parameterized UPDATE to mark as unsubscribed
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        # Redirect or return a short confirmation
        return redirect('/unsubscribed?email=' + email, code=302)

    except MySQLdb.Error:
        # Log internal details to stderr only; do not return DB internals to client
        print("Database error during unsubscribe", file=sys.stderr)
        return "Internal server error", 500

    finally:
        try:
            if cur:
                cur.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass
# ...existing code...
