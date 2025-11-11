# ...existing code...
from flask import Flask, request, abort
import os
import re
import MySQLdb

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
MAX_EMAIL_LEN = 254

def get_db_connection():
    host = os.getenv("DB_HOST", "localhost")
    user = os.getenv("DB_USER")
    password = os.getenv("DB_PASS")
    db = os.getenv("DB_NAME")
    port = int(os.getenv("DB_PORT", "3306"))
    if not (user and password and db):
        raise RuntimeError("database credentials not configured")
    return MySQLdb.connect(host=host, user=user, passwd=password, db=db, port=port, charset="utf8mb4", use_unicode=True)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', type=str)
    if not email or len(email) > MAX_EMAIL_LEN or not EMAIL_RE.fullmatch(email):
        return ('', 400)
    try:
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # mark as unsubscribed; parameterized query prevents SQL injection
                cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
                conn.commit()
            return ('', 204)
        finally:
            conn.close()
    except Exception:
        return ('', 500)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.getenv("PORT", "5000")))
# ...existing code...
