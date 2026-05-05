from flask import Flask, request, make_response
import os
import re
import MySQLdb

DB_HOST = os.environ.get("DB_HOST")
DB_USER = os.environ.get("DB_USER")
DB_PASS = os.environ.get("DB_PASS")
DB_NAME = os.environ.get("DB_NAME")

if not all([DB_HOST, DB_USER, DB_PASS, DB_NAME]):
    raise RuntimeError("Database credentials are not fully configured in environment variables")

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")

app = Flask(__name__)

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    email = request.args.get("email", type=str)
    if not email:
        return make_response(("Missing required parameter", 400))
    email = email.strip()
    if len(email) > 254 or not EMAIL_RE.fullmatch(email):
        return make_response(("Invalid parameter", 400))

    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset="utf8mb4")
        try:
            cur = conn.cursor()
            cur.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
            row = cur.fetchone()
            if not row:
                return make_response(("Not found", 404))
            cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (row[0],))
            conn.commit()
            return make_response(("Unsubscribed", 200))
        finally:
            conn.close()
    except MySQLdb.Error:
        return make_response(("Server error", 500))
