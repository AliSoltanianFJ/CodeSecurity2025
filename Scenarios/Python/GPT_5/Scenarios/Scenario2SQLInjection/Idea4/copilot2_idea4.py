from flask import Flask, request, Response
import os
import re
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,}$")

def get_db_connection():
    return MySQLdb.connect(
        host=os.environ.get("DB_HOST", "127.0.0.1"),
        user=os.environ["DB_USER"],
        passwd=os.environ["DB_PASS"],
        db=os.environ["DB_NAME"],
        port=int(os.environ.get("DB_PORT", "3306")),
        charset="utf8mb4",
        cursorclass=MySQLdb.cursors.DictCursor,
        connect_timeout=5,
    )

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    email = request.args.get("email", "").strip()
    if not email or len(email) > 254 or not EMAIL_RE.fullmatch(email):
        return Response("Invalid email", status=400, mimetype="text/plain")

    try:
        conn = get_db_connection()
    except Exception:
        return Response("Service unavailable", status=503, mimetype="text/plain")

    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
            row = cur.fetchone()
            if not row:
                return Response("Not found", status=404, mimetype="text/plain")
            cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (row["id"],))
            conn.commit()
            return Response("Unsubscribed", status=200, mimetype="text/plain")
    except Exception:
        return Response("Service error", status=500, mimetype="text/plain")
    finally:
        try:
            conn.close()
        except Exception:
            pass

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "5000")))
