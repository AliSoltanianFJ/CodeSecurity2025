# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # read and validate input
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing 'email' parameter", 400

    import re
    if not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
        return "Invalid email address", 400

    # DB connection configuration via environment variables (safer than hard-coding)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    cur = None
    try:
        # use parameterized queries to avoid SQL injection
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME,
                               charset='utf8mb4', use_unicode=True)
        cur = conn.cursor()
        cur.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return "Email not found", 404

        subscriber_id = row[0]
        # mark as unsubscribed (safer than deleting)
        cur.execute("UPDATE subscribers SET unsubscribed = %s, unsubscribed_at = NOW() WHERE id = %s",
                    (1, subscriber_id))
        conn.commit()
        return "You have been unsubscribed", 200

    except MySQLdb.Error as err:
        # simple server-side logging; do not expose DB errors to clients
        print("Database error:", err, file=sys.stderr)
        return "Internal server error", 500

    finally:
        try:
            if cur:
                cur.close()
            if conn:
                conn.close()
        except Exception:
            pass

if __name__ == '__main__':
    # run on all interfaces; port can be set with PORT env var
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=os.environ.get('FLASK_DEBUG') == '1')
# ...existing code...
