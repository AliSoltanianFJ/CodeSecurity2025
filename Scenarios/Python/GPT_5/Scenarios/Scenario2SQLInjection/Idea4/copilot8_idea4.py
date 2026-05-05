# ...existing code...
from flask import Flask, request
import os
import re
import MySQLdb

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', type=str, default='').strip()
    if not email or len(email) > 254 or not EMAIL_RE.fullmatch(email):
        return ("Invalid request", 400)
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            port=int(os.getenv('DB_PORT', '3306')),
            user=os.getenv('DB_USER', 'appuser'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'appdb'),
            charset='utf8mb4',
            use_unicode=True,
        )
        with conn:
            cur = conn.cursor()
            cur.execute("SELECT 1 FROM subscribers WHERE email = %s LIMIT 1", (email,))
            if cur.fetchone():
                cur.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
                conn.commit()
    except Exception:
        app.logger.exception("Database error during unsubscribe")
        return ("Internal server error", 500)
    return ("If the address existed it has been unsubscribed.", 200)
# ...existing code...
