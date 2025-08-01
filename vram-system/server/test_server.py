#!/usr/bin/env python3
"""
VRAM System Test Suite
Tests the Flask server API endpoints and resource management
"""

import requests
import json
import base64
import time
import sys
import os

# Test configuration
SERVER_URL = "http://localhost:5000"
TEST_TIMEOUT = 5

def test_health_check():
    """Test server health check endpoint"""
    print("Testing health check...")
    try:
        response = requests.get(f"{SERVER_URL}/", timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "service" in data
        print("✓ Health check passed")
        return True
    except Exception as e:
        print(f"✗ Health check failed: {e}")
        return False

def test_list_resources():
    """Test resource listing endpoint"""
    print("Testing resource listing...")
    try:
        response = requests.get(f"{SERVER_URL}/api/resources", timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
        assert "resources" in data
        assert "count" in data
        print(f"✓ Resource listing passed ({data['count']} resources)")
        return True
    except Exception as e:
        print(f"✗ Resource listing failed: {e}")
        return False

def test_get_resource():
    """Test getting a specific resource"""
    print("Testing resource retrieval...")
    try:
        # First get the list to find a resource
        response = requests.get(f"{SERVER_URL}/api/resources", timeout=TEST_TIMEOUT)
        data = response.json()
        
        if data["count"] > 0:
            resource_id = data["resources"][0]["id"]
            
            # Get the resource
            response = requests.get(f"{SERVER_URL}/api/resources/{resource_id}", timeout=TEST_TIMEOUT)
            assert response.status_code == 200
            resource_data = response.json()
            assert resource_data["status"] == "success"
            assert "data" in resource_data or "download_url" in resource_data
            print(f"✓ Resource retrieval passed for {resource_id}")
            return True
        else:
            print("✓ No resources to test retrieval with")
            return True
    except Exception as e:
        print(f"✗ Resource retrieval failed: {e}")
        return False

def test_resource_info():
    """Test getting resource metadata"""
    print("Testing resource info...")
    try:
        # Get resource list first
        response = requests.get(f"{SERVER_URL}/api/resources", timeout=TEST_TIMEOUT)
        data = response.json()
        
        if data["count"] > 0:
            resource_id = data["resources"][0]["id"]
            
            # Get resource info
            response = requests.get(f"{SERVER_URL}/api/resources/{resource_id}/info", timeout=TEST_TIMEOUT)
            assert response.status_code == 200
            info_data = response.json()
            assert info_data["status"] == "success"
            assert "metadata" in info_data
            assert "version" in info_data
            print(f"✓ Resource info passed for {resource_id}")
            return True
        else:
            print("✓ No resources to test info with")
            return True
    except Exception as e:
        print(f"✗ Resource info failed: {e}")
        return False

def test_version_check():
    """Test resource version checking"""
    print("Testing version check...")
    try:
        # Get resource list first
        response = requests.get(f"{SERVER_URL}/api/resources", timeout=TEST_TIMEOUT)
        data = response.json()
        
        if data["count"] > 0:
            resource_id = data["resources"][0]["id"]
            
            # Check version
            response = requests.get(f"{SERVER_URL}/api/resources/{resource_id}/version", timeout=TEST_TIMEOUT)
            assert response.status_code == 200
            version_data = response.json()
            assert version_data["status"] == "success"
            assert "version" in version_data
            print(f"✓ Version check passed for {resource_id}")
            return True
        else:
            print("✓ No resources to test version with")
            return True
    except Exception as e:
        print(f"✗ Version check failed: {e}")
        return False

def test_store_resource():
    """Test storing a new resource"""
    print("Testing resource storage...")
    try:
        # Create test data
        test_data = "This is a test resource for VRAM system validation."
        encoded_data = base64.b64encode(test_data.encode()).decode()
        
        payload = {
            "resource_id": "test_resource",
            "data": encoded_data,
            "type": "text",
            "priority": 2,
            "compress": True
        }
        
        response = requests.post(f"{SERVER_URL}/api/resources", 
                               json=payload, 
                               timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        result = response.json()
        assert result["status"] == "success"
        assert result["resource_id"] == "test_resource"
        print("✓ Resource storage passed")
        return True
    except Exception as e:
        print(f"✗ Resource storage failed: {e}")
        return False

def test_delete_resource():
    """Test deleting a resource"""
    print("Testing resource deletion...")
    try:
        # Delete the test resource we created
        response = requests.delete(f"{SERVER_URL}/api/resources/test_resource", timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        result = response.json()
        assert result["status"] == "success"
        print("✓ Resource deletion passed")
        return True
    except Exception as e:
        print(f"✗ Resource deletion failed: {e}")
        return False

def test_stats():
    """Test server statistics endpoint"""
    print("Testing server statistics...")
    try:
        response = requests.get(f"{SERVER_URL}/api/stats", timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
        assert "stats" in data
        stats = data["stats"]
        assert "resource_count" in stats
        assert "total_size" in stats
        print("✓ Server statistics passed")
        return True
    except Exception as e:
        print(f"✗ Server statistics failed: {e}")
        return False

def test_404_handling():
    """Test 404 error handling"""
    print("Testing 404 error handling...")
    try:
        response = requests.get(f"{SERVER_URL}/api/resources/nonexistent", timeout=TEST_TIMEOUT)
        assert response.status_code == 404
        data = response.json()
        assert data["status"] == "error"
        print("✓ 404 handling passed")
        return True
    except Exception as e:
        print(f"✗ 404 handling failed: {e}")
        return False

def run_all_tests():
    """Run all tests and report results"""
    print("=== VRAM Server Test Suite ===")
    print(f"Testing server at: {SERVER_URL}")
    print()
    
    tests = [
        test_health_check,
        test_list_resources,
        test_get_resource,
        test_resource_info,
        test_version_check,
        test_store_resource,
        test_delete_resource,
        test_stats,
        test_404_handling
    ]
    
    passed = 0
    failed = 0
    
    for test in tests:
        try:
            if test():
                passed += 1
            else:
                failed += 1
        except Exception as e:
            print(f"✗ Test {test.__name__} crashed: {e}")
            failed += 1
        print()
    
    print("=== Test Results ===")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")
    print(f"Total: {passed + failed}")
    
    if failed == 0:
        print("🎉 All tests passed!")
        return True
    else:
        print("❌ Some tests failed!")
        return False

if __name__ == "__main__":
    if len(sys.argv) > 1:
        SERVER_URL = sys.argv[1]
    
    success = run_all_tests()
    sys.exit(0 if success else 1)