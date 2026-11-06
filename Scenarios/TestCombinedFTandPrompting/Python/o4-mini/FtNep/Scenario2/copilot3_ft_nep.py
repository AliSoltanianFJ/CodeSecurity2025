from flask import Flask, request, jsonify, current_app, g, abort
import os
import re
import logging
import MySQLdb
import MySQLdb.cursors

# ==== Configuration & Setup ====

app = Flask(__name__)

# Configure basic logging: errors will go to stderr (or your WSGI server’s logs).
# In production you may wish to integrate with a centralized logging system.
logging.basicConfig(level=logging.INFO)

# Strict RFC-compliant (simplified) regex for validating email addresses.
# Rejects input longer than 254 characters, per standards.
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)([A-Za-z0-9!#$%&’*+/=?^_`{|}~-]+"
    r"(?:\.[A-Za-z0-9!#$%&’*+/=?^_`{|}~-]+)*"
    r")@([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
    r"(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+)$"
)

# Ensure required environment variables are present at startup.
required_env = ["DB_HOST", "DB_USER", "DB_PASS", "DB_NAME"]
missing = [v for v in required_env if not os.getenv(v)]
if missing:
    # Fail fast if configuration is incomplete
    raise RuntimeError("Missing required environment variables: " + ", ".join(missing))


def get_db_connection():
    """
    Returns a MySQLdb connection stored in flask.g.
    Aborts with 500 if connection cannot be established.
    """
    if "db_conn" not in g:
        try:
            g.db_conn = MySQLdb.connect(
                host=os.getenv("DB_HOST"),
                user=os.getenv("DB_USER"),
                passwd=os.getenv("DB_PASS"),
                db=os.getenv("DB_NAME"),
                cursorclass=MySQLdb.cursors.DictCursor,
                connect_timeout=5
            )
        except Exception:
            current_app.logger.exception("Failed to connect to database")
            # Do not expose exception details to client
            abort(500, description="Internal server error")
    return g.db_conn


@app.teardown_appcontext
def close_db_connection(error):
    """
    Closes the database connection at the end of the request.
    """
    db_conn = g.pop("db_conn", None)
    if db_conn is not None:
        try:
            db_conn.close()
        except Exception:
            current_app.logger.warning("Failed to close database connection", exc_info=True)


# ==== Unsubscribe Endpoint ====

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    # 1) Extract and sanitize input
    email = request.args.get("email", "").strip()
    if not email:
        return jsonify({"error": "Email parameter is required"}), 400

    # 2) Validate length and format
    if len(email) > 254 or not EMAIL_REGEX.match(email):
        return jsonify({"error": "Invalid email format"}), 400

    # 3) Perform database operations
    db = get_db_connection()
    try:
        cursor = db.cursor()

        # 3a) Check existence with parameterized query
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()

        if not row:
            # 3b) Email not found
            return jsonify({"error": "Email address not found"}), 404

        # 3c) Delete the subscriber
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (email,)
        )
        db.commit()

        # 4) Return successful response
        return jsonify({"message": "You have been unsubscribed"}), 200

    except Exception:
        # Log the full stack trace for investigation
        current_app.logger.exception("Database query failed during unsubscribe")
        # Do not expose internal details to the user
        return jsonify({"error": "Internal server error"}), 500

    finally:
        try:
            cursor.close()
        except Exception:
            current_app.logger.debug("Failed to close cursor", exc_info=True)
