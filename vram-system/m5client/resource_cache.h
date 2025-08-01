#ifndef RESOURCE_CACHE_H
#define RESOURCE_CACHE_H

#include <Arduino.h>
#include <map>
#include <ArduinoJson.h>
#include "memory_manager.h"

struct CacheEntry {
    String resourceId;
    size_t size;
    uint32_t version;
    uint32_t timestamp;
    uint32_t lastAccessed;
    uint8_t priority;
    String type;
    String hash;
};

class ResourceCache {
private:
    MemoryManager* memoryManager;
    std::map<String, CacheEntry> cacheIndex;
    uint32_t hitCount;
    uint32_t missCount;
    
    // Cache validation
    bool validateCacheEntry(const String& resourceId, const CacheEntry& entry);
    
public:
    ResourceCache(MemoryManager* memMgr);
    ~ResourceCache();
    
    // Cache operations
    bool storeResource(const String& resourceId, const uint8_t* data, size_t size, 
                      uint32_t version, uint8_t priority = 1, const String& type = "generic");
    uint8_t* getResource(const String& resourceId, size_t& size);
    bool hasResource(const String& resourceId);
    bool removeResource(const String& resourceId);
    
    // Cache management
    void updateResourceMetadata(const String& resourceId, uint32_t version, 
                               const String& hash = "");
    bool isResourceValid(const String& resourceId, uint32_t serverVersion);
    void invalidateResource(const String& resourceId);
    void clearCache();
    
    // Cache statistics
    float getHitRatio() const;
    uint32_t getHitCount() const { return hitCount; }
    uint32_t getMissCount() const { return missCount; }
    size_t getCacheSize() const { return cacheIndex.size(); }
    
    // Cache information
    std::vector<String> listCachedResources();
    CacheEntry getCacheEntry(const String& resourceId);
    void printCacheStats();
    
    // Cache policy
    void setResourcePriority(const String& resourceId, uint8_t priority);
    void touchResource(const String& resourceId);
};

#endif