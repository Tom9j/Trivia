from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import json
import logging
import os
from datetime import datetime
from resource_manager import ResourceManager

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize resource manager
resource_manager = ResourceManager("resources")

@app.route('/')
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "ok",
        "service": "VRAM Resource Server",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    })

@app.route('/api/resources', methods=['GET'])
def list_resources():
    """List all available resources"""
    try:
        resource_type = request.args.get('type')
        resources = resource_manager.list_resources(resource_type)
        
        return jsonify({
            "status": "success",
            "resources": resources,
            "count": len(resources)
        })
    
    except Exception as e:
        logger.error(f"Error listing resources: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources/<resource_id>', methods=['GET'])
def get_resource(resource_id):
    """Get a specific resource"""
    try:
        result = resource_manager.get_resource(resource_id)
        
        if result is None:
            return jsonify({"status": "error", "message": "Resource not found"}), 404
        
        data, metadata = result
        
        # Return JSON response with base64 encoded data for small resources
        if len(data) <= 1024 * 1024:  # 1MB limit for JSON response
            import base64
            return jsonify({
                "status": "success",
                "resource_id": resource_id,
                "data": base64.b64encode(data).decode('utf-8'),
                "encoding": "base64",
                "metadata": metadata
            })
        else:
            # For large resources, return download link
            return jsonify({
                "status": "success",
                "resource_id": resource_id,
                "download_url": f"/api/resources/{resource_id}/download",
                "metadata": metadata
            })
    
    except Exception as e:
        logger.error(f"Error getting resource {resource_id}: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources/<resource_id>/download', methods=['GET'])
def download_resource(resource_id):
    """Download a resource as binary file"""
    try:
        result = resource_manager.get_resource(resource_id)
        
        if result is None:
            return jsonify({"status": "error", "message": "Resource not found"}), 404
        
        data, metadata = result
        
        # Create temporary file for download
        temp_file = f"/tmp/{resource_id}.bin"
        with open(temp_file, 'wb') as f:
            f.write(data)
        
        return send_file(temp_file, as_attachment=True, download_name=f"{resource_id}.bin")
    
    except Exception as e:
        logger.error(f"Error downloading resource {resource_id}: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources/<resource_id>/version', methods=['GET'])
def check_resource_version(resource_id):
    """Check the version of a resource"""
    try:
        version = resource_manager.check_version(resource_id)
        
        if version is None:
            return jsonify({"status": "error", "message": "Resource not found"}), 404
        
        return jsonify({
            "status": "success",
            "resource_id": resource_id,
            "version": version
        })
    
    except Exception as e:
        logger.error(f"Error checking version for {resource_id}: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources', methods=['POST'])
def store_resource():
    """Store a new resource"""
    try:
        # Get parameters from request
        data = request.get_json()
        
        if not data or 'resource_id' not in data or 'data' not in data:
            return jsonify({"status": "error", "message": "Missing required fields"}), 400
        
        resource_id = data['resource_id']
        resource_type = data.get('type', 'generic')
        priority = data.get('priority', 1)
        compress = data.get('compress', True)
        
        # Decode base64 data
        import base64
        try:
            resource_data = base64.b64decode(data['data'])
        except Exception:
            return jsonify({"status": "error", "message": "Invalid base64 data"}), 400
        
        # Store the resource
        result = resource_manager.store_resource(
            resource_id, resource_data, resource_type, priority, compress
        )
        
        return jsonify(result)
    
    except Exception as e:
        logger.error(f"Error storing resource: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources/<resource_id>', methods=['DELETE'])
def delete_resource(resource_id):
    """Delete a resource"""
    try:
        success = resource_manager.delete_resource(resource_id)
        
        if not success:
            return jsonify({"status": "error", "message": "Resource not found"}), 404
        
        return jsonify({
            "status": "success",
            "message": f"Resource {resource_id} deleted"
        })
    
    except Exception as e:
        logger.error(f"Error deleting resource {resource_id}: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/stats', methods=['GET'])
def get_stats():
    """Get server statistics"""
    try:
        stats = resource_manager.get_storage_stats()
        
        return jsonify({
            "status": "success",
            "stats": stats,
            "timestamp": datetime.now().isoformat()
        })
    
    except Exception as e:
        logger.error(f"Error getting stats: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/resources/<resource_id>/info', methods=['GET'])
def get_resource_info(resource_id):
    """Get detailed information about a resource without downloading it"""
    try:
        if resource_id not in resource_manager.metadata["resources"]:
            return jsonify({"status": "error", "message": "Resource not found"}), 404
        
        metadata = resource_manager.metadata["resources"][resource_id]
        version = resource_manager.metadata["versions"].get(resource_id, 1)
        
        return jsonify({
            "status": "success",
            "resource_id": resource_id,
            "metadata": metadata,
            "version": version
        })
    
    except Exception as e:
        logger.error(f"Error getting resource info for {resource_id}: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

# Error handlers
@app.errorhandler(404)
def not_found(error):
    return jsonify({"status": "error", "message": "Endpoint not found"}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({"status": "error", "message": "Internal server error"}), 500

if __name__ == '__main__':
    # Create some demo resources
    logger.info("Starting VRAM Resource Server...")
    
    # Add demo resources for testing
    demo_text = b"This is a demo text resource for VRAM testing."
    demo_json = json.dumps({"type": "demo", "message": "Hello from VRAM!", "data": [1, 2, 3, 4, 5]}).encode()
    demo_binary = bytes(range(256))  # Binary data
    
    resource_manager.store_resource("demo_text", demo_text, "text", priority=2)
    resource_manager.store_resource("demo_json", demo_json, "json", priority=1)
    resource_manager.store_resource("demo_binary", demo_binary, "binary", priority=3)
    
    logger.info("Demo resources created")
    
    # Start the server
    app.run(host='0.0.0.0', port=5000, debug=True)