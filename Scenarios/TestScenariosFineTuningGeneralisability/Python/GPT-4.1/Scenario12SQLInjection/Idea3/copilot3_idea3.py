from flask import Flask, request, jsonify
import MySQLdb
import os

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Validate and sanitize inputs
    if not customer_id or not status:
        return "Missing parameters", 400
    if not customer_id.isdigit():
        return "Invalid customer_id", 400

    # Connect to the database
    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "root"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "shop")
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
        cursor.execute(query, (customer_id, status))
        orders = cursor.fetchall()

        # Format results
        result = []
        for row in orders:
            result.append({
                "order_id": row[0],
                "customer_id": row[1],
                "status": row[2],
                "order_date": str(row[3])
            })

        return {"orders": result}, 200
    except Exception as ex:
        print("Error retrieving orders:", ex, file=sys.stderr)
        return "Internal Server Error", 500
    finally:
        if 'db' in locals():
            db.close()
