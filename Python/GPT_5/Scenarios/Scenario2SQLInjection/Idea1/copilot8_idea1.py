# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Read and validate input
    email = (request.args.get('email') or '').strip()
    if not email:
        return "Missing 'email' parameter", 400

    EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
    if not EMAIL_RE.match(email):
        return "Invalid email format", 400

    # Load DB config from environment (do NOT hardcode credentials)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            use_unicode=True
        )
        cur = conn.cursor()

        # Parameterized queries to prevent SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return "Email not found", 404

        subscriber_id, subscribed = row
        if not subscribed:
            return "Already unsubscribed", 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return "Successfully unsubscribed", 200

    except MySQLdb.Error as e:
        # Avoid exposing DB errors to clients; log for operators
        print("Database error:", e, file=sys.stderr)
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

if __name__ == '__main__':
    # For local testing only. In production use a WSGI server.
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=True)
# ...existing code...
