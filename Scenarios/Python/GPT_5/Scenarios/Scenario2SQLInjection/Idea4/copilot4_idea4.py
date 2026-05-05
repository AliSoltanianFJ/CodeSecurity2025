from flask import Flask, request, jsonify, abort
import os
import re
import MySQLdb

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_config():
    try:
        return {
            "host": os.environ["DB_HOST"],
            "user": os.environ["DB_USER"],
            "passwd": os.environ["DB_PASSWORD"],
            "db": os.environ["DB_NAME"],
            "port": int(os.environ.get("DB_PORT", "3306")),
            "charset": "utf8mb4",
            "use_unicode": True,
        }
    except KeyError:
        return None

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email or len(email) > 254 or not EMAIL_RE.fullmatch(email):
        abort(400)

    cfg = get_db_config()
    if not cfg:
        app.logger.error("Database configuration missing")
        abort(500)

    conn = None
    try:
        conn = MySQLdb.connect(**cfg)
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify({"status": "not_found"}), 404
        subscriber_id = row[0]
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({"status": "unsubscribed"}), 200
    except Exception:
        app.logger.exception("Database error while processing unsubscribe")
        abort(500)
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass
