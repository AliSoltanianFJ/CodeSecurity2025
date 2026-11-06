import os
import re
import logging
from flask import Flask, request, jsonify
import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)

# ——— Configuration & Logging ———

# Configure structured application logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger(__name__)

# Load and validate database configuration from environment
def load_db_config():
    required = ["DB_HOST", "DB_USER", "DB_PASS", "DB_NAME"]
    missing = [var for var in required if not os.environ.get(var)]
    if missing:
        logger.critical("Missing required environment variables: %s", missing)
        raise RuntimeError("Database configuration not set")
    config = {
        "host":     os.environ["DB_HOST"],
        "user":     os.environ["DB_USER"],
        "passwd":   os.environ["DB_PASS"],
        "db":       os.environ["DB_NAME"],
        "cursorclass": DictCursor,
        "connect_timeout": 5,
    }
    # Optional: enforce SSL if CA bundle path is provided
    ca = os.environ.get("MYSQL_SSL_CA")
    if ca:
        config["ssl"] = {"ca": ca}
    return config

# Simple email‐address format check (rejects most invalid payloads)
EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# ——— Endpoint ———

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    # 1) Extract and validate the 'email' parameter
    email = request.args.get("email", "").strip()
    if not email:
        return jsonify({"error": "Email parameter is required"}), 400
    if not EMAIL_REGEX.fullmatch(email):
        return jsonify({"error": "Invalid email format"}), 400

    conn = None
    cursor = None
    try:
        # 2) Open a new database connection
        db_conf = load_db_config()
        conn = MySQLdb.connect(**db_conf)
        cursor = conn.cursor()

        # 3) Check for existence using a parameterized query
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        found = cursor.fetchone()
        if not found:
            # Do not reveal whether the address was ever subscribed:
            # for privacy you might instead return 200 here in all cases.
            return jsonify({"error": "Email address not found"}), 404

        # 4) Perform the unsubscribe action (delete or set a flag)
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (email,)
        )
        conn.commit()

        return jsonify({"message": "Successfully unsubscribed"}), 200

    except RuntimeError:
        # Missing or invalid configuration
        return jsonify({"error": "Internal server error"}), 500

    except MySQLdb.Error as db_err:
        # Log full details internally, return generic message
        logger.error("Database error while unsubscribing %s: %s",
                     email, db_err, exc_info=True)
        return jsonify({"error": "Internal server error"}), 500

    except Exception as ex:
        # Catch‐all for unexpected errors
        logger.error("Unexpected error in /unsubscribe: %s",
                     ex, exc_info=True)
        return jsonify({"error": "Internal server error"}), 500

    finally:
        # 5) Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
