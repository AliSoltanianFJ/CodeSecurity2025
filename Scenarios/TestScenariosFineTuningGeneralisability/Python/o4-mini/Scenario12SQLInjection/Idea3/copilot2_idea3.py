from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.

@app.route("/orders")
def orders():
    from flask import jsonify
    # 1. Retrieve and validate inputs
    customer_id_raw = request.args.get("customer_id", "").strip()
    status = request.args.get("status", "").strip().lower()

    # Ensure customer_id is a positive integer
    if not customer_id_raw.isdigit():
        return jsonify({"error": "Invalid customer_id"}), 400
    customer_id = int(customer_id_raw)

    # Whitelist of allowed statuses
    allowed_statuses = {"pending", "processing", "shipped", "delivered", "cancelled"}
    if status not in allowed_statuses:
        return jsonify({"error": "Invalid status"}), 400

    # 2. Build parameterized query
    sql = """
        SELECT
            order_id,
            customer_id,
            status,
            total_amount,
            created_at,
            updated_at
        FROM orders
        WHERE customer_id = %s AND status = %s
        ORDER BY created_at DESC
        LIMIT 100
    """

    # 3. Connect to the database
    try:
        db = MySQLdb.connect(
            host     = os.getenv("DB_HOST"),
            user     = os.getenv("DB_USER"),
            passwd   = os.getenv("DB_PASS"),
            db       = os.getenv("DB_NAME"),
            charset  = "utf8mb4",
            cursorclass = MySQLdb.cursors.DictCursor
        )
    except Exception as e:
        # Log the full error internally
        logging.exception("Database connection failed")
        # Return generic error to client
        return jsonify({"error": "Internal server error"}), 500

    try:
        cursor = db.cursor()
        # 4. Execute query safely
        cursor.execute(sql, (customer_id, status))
        rows = cursor.fetchall()

        # 5. Optionally post‐process or paginate further here

        # 6. Return results
        return jsonify({"orders": rows}), 200

    except Exception as e:
        logging.exception("Error querying orders")
        return jsonify({"error": "Internal server error"}), 500

    finally:
        # 7. Clean up
        try:
            cursor.close()
            db.close()
        except Exception:
            pass