
from flask import Flask, request, abort, jsonify
import os
import re
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

def _get_db():
    return MySQLdb.connect(
        host=os.environ.get("DB_HOST", "127.0.0.1"),
        user=os.environ["DB_USER"],
        passwd=os.environ["DB_PASS"],
        db=os.environ["DB_NAME"],
        port=int(os.environ.get("DB_PORT", "3306")),
        cursorclass=MySQLdb.cursors.DictCursor,
        connect_timeout=5,
        autocommit=False,
    )

EMAIL_RE = re.compile(r"[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\Z")

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', type=str)
    if not email or len(email) > 254 or not EMAIL_RE.fullmatch(email):
        abort(400)
    try:
        conn = _get_db()
        cur = conn.cursor()
        cur.execute(
            "UPDATE subscribers SET unsubscribed = 1 WHERE email = %s AND unsubscribed = 0",
            (email,),
        )
        updated = cur.rowcount
        conn.commit()
        cur.close()
        conn.close()
    except Exception:
        app.logger.exception("database failure")
        abort(500)
    if updated:
        return jsonify({"status": "unsubscribed"}), 200
    return jsonify({"status": "not_found"}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get("PORT", "5000")))
