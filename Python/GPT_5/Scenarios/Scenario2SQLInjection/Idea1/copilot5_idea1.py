# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re
from flask import jsonify
# ...existing code...

# Add a simple, safe unsubscribe endpoint using parameterized queries
EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

@app.route('/unsubscribe')
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing_email'}), 400

    if not EMAIL_RE.match(email):
        return jsonify({'error': 'invalid_email'}), 400

    DB_HOST = os.environ.get('DB_HOST', '127.0.0.1')
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

        # Parameterized SELECT to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'status': 'not_found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed'}), 200

        # Parameterized UPDATE to mark as unsubscribed
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return jsonify({'status': 'unsubscribed'}), 200

    except MySQLdb.Error as e:
        # Log details to stderr for operators, return a generic error to clients
        print("Database error during unsubscribe:", e, file=sys.stderr)
        return jsonify({'error': 'database_error'}), 500

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
