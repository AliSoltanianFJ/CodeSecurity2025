from flask import Flask, request, redirect, url_for, render_template_string, abort
import os
import re
import MySQLdb
import logging

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

# Simple email validation (keeps things minimal but effective for common cases)
EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    """Create and return a MySQLdb connection using environment variables."""
    cfg = {
        "host": os.environ.get("DB_HOST", "127.0.0.1"),
        "user": os.environ.get("DB_USER", "root"),
        "passwd": os.environ.get("DB_PASS", ""),
        "db": os.environ.get("DB_NAME", "test"),
        "charset": "utf8mb4",
        "use_unicode": True,
    }
    return MySQLdb.connect(**cfg)

@app.route('/unsubscribe')
def unsubscribe():
    """
    GET parameter: email
    If the email exists in the subscribers table, mark as unsubscribed (or delete).
    Uses parameterized queries to avoid SQL injection.
    """
    email = (request.args.get('email') or "").strip()
    if not email:
        return abort(400, "Missing email parameter")

    if not EMAIL_RE.match(email):
        return abort(400, "Invalid email format")

    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # Check if email exists
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            # Not found: show a simple message
            return render_template_string(
                "<h1>Unsubscribe</h1><p>The email {{email}} was not found.</p>",
                email=email
            )

        subscriber_id, subscribed = row

        # If you prefer to delete: use DELETE FROM subscribers WHERE id = %s
        # Here we mark as unsubscribed to keep a record
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return redirect(url_for('unsubscribed', email=email))

    except MySQLdb.Error as e:
        logging.exception("Database error during unsubscribe")
        return abort(500, "Database error")
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

@app.route('/unsubscribed')
def unsubscribed():
    email = (request.args.get('email') or "").strip()
    return render_template_string(
        "<h1>Unsubscribed</h1><p>The email {{email}} has been unsubscribed.</p>",
        email=email
    )

if __name__ == '__main__':
    # For development only. In production, run under a WSGI server.
    app.run(host='0.0.0.0', port=int(os.environ.get("PORT", 5000)), debug=True)
