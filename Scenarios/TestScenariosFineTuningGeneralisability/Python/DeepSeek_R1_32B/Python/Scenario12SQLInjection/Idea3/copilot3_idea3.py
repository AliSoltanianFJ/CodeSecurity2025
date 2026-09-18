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
    # Validate and sanitize input
    customer_id = request.args.get("customer_id", "").strip()
    status = request.args.get("status", "").strip()

    # Only allow positive integers for customer_id to prevent SQL injection
    if not customer_id.isdigit() or int(customer_id) <= 0:
        return "Invalid customer ID.", 400

    # Only allow specific status values to prevent SQL injection
    allowed_status = {"open", "closed", "processing"}
    if status.lower() not in allowed_status:
        return "Invalid status. Allowed statuses: open, closed, processing.", 400

    # Use parameterized queries to prevent SQL injection
    try:
        db = MySQLdb.connect(
            host=os.getenv("DB_HOST"),
            user=os.getenv("DB_USER"),
            passwd=os.getenv("DB_PASS"),
            db=os.getenv("DB_NAME"),
            charset="utf8mb4",
        )
        cursor = db.cursor()
        query = """
            SELECT order_id, item, quantity, price
            FROM orders
            WHERE customer_id = %s AND status = %s
        """
        cursor.execute(query, (customer_id, status.lower()))
        results = cursor.fetchall()
        if not results:
            return "No matching orders found.", 404

        # Format response safely without exposing raw SQL data
        output = []
        for row in results:
            order_info = {
                "order_id": row[0],
                "item": row[1],
                "quantity": row[2],
                "price": row[3]
            }
            output.append(order_info)
        return {"orders": output}, 200

    except Exception as err:
        print("Error retrieving orders:", str(err), file=sys.stderr)
        return "Internal server error.", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass