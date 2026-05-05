from flask import Flask, request, jsonify
import os
import re
import MySQLdb

app = Flask(__name__)

EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
MAX_EMAIL_LENGTH = 254

def get_db_connection():
    return MySQLdb.connect(
        host=os.getenv("DB_HOST", "localhost"),
        user=os.getenv("DB_USER", "root"),
        passwd=os.getenv("DB_PASS", ""),
        db=os.getenv("DB_NAME", "app"),
        port=int(os.getenv("DB_PORT", "3306")),
        charset="utf8mb4",
        use_unicode=True,
    )

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    email = request.args.get("email", "").strip()
    if not email:
        return jsonify({"error": "missing parameter"}), 400
    if len(email) > MAX_EMAIL_LENGTH or not EMAIL_REGEX.fullmatch(email):
        return jsonify({"error": "invalid parameter"}), 400

    try:
        conn = get_db_connection()
        try:
            cur = conn.cursor()
            cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s LIMIT 1", (email,))
            conn.commit()
            cur.close()
        finally:
            conn.close()
    except Exception:
        app.logger.exception("Database error during unsubscribe")
        return jsonify({"error": "internal error"}), 500

    return jsonify({"status": "request processed"}), 200
