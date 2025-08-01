import os
import json
import gzip
import hashlib
from datetime import datetime
import logging
from typing import Dict, List, Optional, Tuple

class ResourceManager:
    """Advanced resource management system for VRAM server"""
    
    def __init__(self, resources_dir: str = "resources"):
        self.resources_dir = resources_dir
        self.metadata_file = os.path.join(resources_dir, "metadata.json")
        self.usage_stats = {}
        self.resource_cache = {}
        
        # Ensure resources directory exists
        os.makedirs(resources_dir, exist_ok=True)
        
        # Setup logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
        # Load existing metadata
        self.metadata = self._load_metadata()
        
    def _load_metadata(self) -> Dict:
        """Load resource metadata from file"""
        if os.path.exists(self.metadata_file):
            try:
                with open(self.metadata_file, 'r') as f:
                    return json.load(f)
            except Exception as e:
                self.logger.error(f"Error loading metadata: {e}")
        return {"resources": {}, "versions": {}}
    
    def _save_metadata(self):
        """Save resource metadata to file"""
        try:
            with open(self.metadata_file, 'w') as f:
                json.dump(self.metadata, f, indent=2)
        except Exception as e:
            self.logger.error(f"Error saving metadata: {e}")
    
    def _calculate_hash(self, data: bytes) -> str:
        """Calculate SHA256 hash of data"""
        return hashlib.sha256(data).hexdigest()
    
    def _compress_data(self, data: bytes) -> bytes:
        """Compress data using gzip"""
        return gzip.compress(data)
    
    def _decompress_data(self, compressed_data: bytes) -> bytes:
        """Decompress gzip data"""
        return gzip.decompress(compressed_data)
    
    def store_resource(self, resource_id: str, data: bytes, 
                      resource_type: str = "generic", 
                      priority: int = 1,
                      compress: bool = True) -> Dict:
        """Store a resource with metadata"""
        try:
            # Calculate hash for integrity checking
            data_hash = self._calculate_hash(data)
            
            # Compress if requested
            storage_data = self._compress_data(data) if compress else data
            
            # Create resource file path
            file_path = os.path.join(self.resources_dir, f"{resource_id}.bin")
            
            # Store the resource
            with open(file_path, 'wb') as f:
                f.write(storage_data)
            
            # Update metadata
            self.metadata["resources"][resource_id] = {
                "type": resource_type,
                "size": len(data),
                "compressed_size": len(storage_data),
                "hash": data_hash,
                "compressed": compress,
                "priority": priority,
                "created": datetime.now().isoformat(),
                "last_accessed": datetime.now().isoformat(),
                "access_count": 0
            }
            
            # Update version
            self.metadata["versions"][resource_id] = self.metadata["versions"].get(resource_id, 0) + 1
            
            self._save_metadata()
            
            self.logger.info(f"Stored resource {resource_id} ({len(data)} bytes)")
            
            return {
                "status": "success",
                "resource_id": resource_id,
                "size": len(data),
                "compressed_size": len(storage_data),
                "version": self.metadata["versions"][resource_id],
                "hash": data_hash
            }
            
        except Exception as e:
            self.logger.error(f"Error storing resource {resource_id}: {e}")
            return {"status": "error", "message": str(e)}
    
    def get_resource(self, resource_id: str) -> Optional[Tuple[bytes, Dict]]:
        """Retrieve a resource with its metadata"""
        try:
            if resource_id not in self.metadata["resources"]:
                return None
            
            file_path = os.path.join(self.resources_dir, f"{resource_id}.bin")
            
            if not os.path.exists(file_path):
                return None
            
            # Read the resource
            with open(file_path, 'rb') as f:
                storage_data = f.read()
            
            # Get metadata
            resource_meta = self.metadata["resources"][resource_id]
            
            # Decompress if needed
            if resource_meta["compressed"]:
                data = self._decompress_data(storage_data)
            else:
                data = storage_data
            
            # Verify integrity
            if self._calculate_hash(data) != resource_meta["hash"]:
                self.logger.error(f"Hash mismatch for resource {resource_id}")
                return None
            
            # Update access statistics
            resource_meta["last_accessed"] = datetime.now().isoformat()
            resource_meta["access_count"] += 1
            self._save_metadata()
            
            self.logger.info(f"Retrieved resource {resource_id}")
            
            return data, resource_meta
            
        except Exception as e:
            self.logger.error(f"Error retrieving resource {resource_id}: {e}")
            return None
    
    def list_resources(self, resource_type: Optional[str] = None) -> List[Dict]:
        """List all available resources"""
        resources = []
        
        for resource_id, meta in self.metadata["resources"].items():
            if resource_type is None or meta["type"] == resource_type:
                resources.append({
                    "id": resource_id,
                    "type": meta["type"],
                    "size": meta["size"],
                    "compressed_size": meta["compressed_size"],
                    "priority": meta["priority"],
                    "version": self.metadata["versions"].get(resource_id, 1),
                    "last_accessed": meta["last_accessed"],
                    "access_count": meta["access_count"]
                })
        
        return sorted(resources, key=lambda x: x["last_accessed"], reverse=True)
    
    def check_version(self, resource_id: str) -> Optional[int]:
        """Check the current version of a resource"""
        return self.metadata["versions"].get(resource_id)
    
    def delete_resource(self, resource_id: str) -> bool:
        """Delete a resource"""
        try:
            if resource_id not in self.metadata["resources"]:
                return False
            
            file_path = os.path.join(self.resources_dir, f"{resource_id}.bin")
            
            if os.path.exists(file_path):
                os.remove(file_path)
            
            del self.metadata["resources"][resource_id]
            if resource_id in self.metadata["versions"]:
                del self.metadata["versions"][resource_id]
            
            self._save_metadata()
            
            self.logger.info(f"Deleted resource {resource_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"Error deleting resource {resource_id}: {e}")
            return False
    
    def get_storage_stats(self) -> Dict:
        """Get storage statistics"""
        total_size = 0
        total_compressed = 0
        resource_count = len(self.metadata["resources"])
        
        for meta in self.metadata["resources"].values():
            total_size += meta["size"]
            total_compressed += meta["compressed_size"]
        
        return {
            "resource_count": resource_count,
            "total_size": total_size,
            "total_compressed_size": total_compressed,
            "compression_ratio": total_compressed / total_size if total_size > 0 else 0,
            "disk_usage_mb": total_compressed / (1024 * 1024)
        }