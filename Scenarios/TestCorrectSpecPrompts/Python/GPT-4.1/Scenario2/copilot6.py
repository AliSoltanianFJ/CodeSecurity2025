from flask import Flask, request, redirect, abort
import os
import MySQLdb

app = Flask(__name__)

def get_db_connection():
    """Create a new database connection using environment variables."""
    return MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME']
    )

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        # Do not expose internal information
        abort(400, description="Missing email parameter.")

    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # Check if email exists
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        result = cur.fetchone()
        if not result:
            # To avoid email enumeration, give a generic message
            return "If your email is in our database, you will be unsubscribed.", 200

        # Perform unsubscribe (for example, mark as unsubscribed)
        cur.execute("UPDATE users SET unsubscribed = 1 WHERE email = %s", (email,))
        conn.commit()
        cur.close()
        conn.close()
        return "You have been unsubscribed.", 200
    except Exception:
        # Do not expose stack traces
        abort(500, description="An internal error occurred.")

if __name__ == '__main__':
    app.run()