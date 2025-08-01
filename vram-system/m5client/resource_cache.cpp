#include "resource_cache.h"
#include <algorithm>

ResourceCache::ResourceCache(MemoryManager* memMgr) : 
    memoryManager(memMgr), 
    hitCount(0), 
    missCount(0) {
}

ResourceCache::~ResourceCache() {
    clearCache();
}

bool ResourceCache::storeResource(const String& resourceId, const uint8_t* data, size_t size, 
                                 uint32_t version, uint8_t priority, const String& type) {
    // Allocate memory for the resource
    uint8_t* ptr = memoryManager->allocate(resourceId, size, priority);
    if (!ptr) {
        Serial.println("CACHE: Failed to allocate memory for " + resourceId);
        return false;
    }
    
    // Copy data
    memcpy(ptr, data, size);
    
    // Create cache entry
    CacheEntry entry;
    entry.resourceId = resourceId;
    entry.size = size;
    entry.version = version;
    entry.timestamp = millis();
    entry.lastAccessed = millis();
    entry.priority = priority;
    entry.type = type;
    entry.hash = ""; // Will be set if needed
    
    cacheIndex[resourceId] = entry;
    
    Serial.println("CACHE: Stored " + resourceId + " (" + String(size) + " bytes)");
    return true;
}

uint8_t* ResourceCache::getResource(const String& resourceId, size_t& size) {
    auto it = cacheIndex.find(resourceId);
    if (it == cacheIndex.end()) {
        missCount++;
        Serial.println("CACHE: Miss for " + resourceId);
        return nullptr;
    }
    
    // Check if memory manager still has the resource
    if (!memoryManager->hasResource(resourceId)) {
        // Resource was evicted, remove from cache index
        cacheIndex.erase(it);
        missCount++;
        Serial.println("CACHE: Resource " + resourceId + " was evicted");
        return nullptr;
    }
    
    // Update access statistics
    hitCount++;
    it->second.lastAccessed = millis();
    memoryManager->updateAccess(resourceId);
    
    // Get the data pointer from memory manager
    auto memBlocks = memoryManager->allocatedBlocks;
    auto memIt = memBlocks.find(resourceId);
    if (memIt != memBlocks.end()) {
        size = it->second.size;
        Serial.println("CACHE: Hit for " + resourceId);
        return memIt->second.data;
    }
    
    missCount++;
    return nullptr;
}

bool ResourceCache::hasResource(const String& resourceId) {
    auto it = cacheIndex.find(resourceId);
    if (it == cacheIndex.end()) {
        return false;
    }
    
    // Verify memory manager still has it
    return memoryManager->hasResource(resourceId);
}

bool ResourceCache::removeResource(const String& resourceId) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        cacheIndex.erase(it);
        bool result = memoryManager->deallocate(resourceId);
        Serial.println("CACHE: Removed " + resourceId);
        return result;
    }
    return false;
}

void ResourceCache::updateResourceMetadata(const String& resourceId, uint32_t version, const String& hash) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        it->second.version = version;
        if (hash.length() > 0) {
            it->second.hash = hash;
        }
    }
}

bool ResourceCache::isResourceValid(const String& resourceId, uint32_t serverVersion) {
    auto it = cacheIndex.find(resourceId);
    if (it == cacheIndex.end()) {
        return false;
    }
    
    return it->second.version >= serverVersion;
}

void ResourceCache::invalidateResource(const String& resourceId) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        it->second.version = 0; // Mark as invalid
    }
}

void ResourceCache::clearCache() {
    for (const auto& pair : cacheIndex) {
        memoryManager->deallocate(pair.first);
    }
    cacheIndex.clear();
    hitCount = 0;
    missCount = 0;
    Serial.println("CACHE: Cleared all cached resources");
}

float ResourceCache::getHitRatio() const {
    uint32_t total = hitCount + missCount;
    if (total == 0) return 0.0;
    return (float)hitCount / total * 100.0;
}

std::vector<String> ResourceCache::listCachedResources() {
    std::vector<std::pair<String, uint32_t>> resources;
    
    for (const auto& pair : cacheIndex) {
        if (memoryManager->hasResource(pair.first)) {
            resources.push_back({pair.first, pair.second.lastAccessed});
        }
    }
    
    // Sort by last accessed (most recent first)
    std::sort(resources.begin(), resources.end(),
              [](const std::pair<String, uint32_t>& a, const std::pair<String, uint32_t>& b) {
                  return a.second > b.second;
              });
    
    std::vector<String> result;
    for (const auto& resource : resources) {
        result.push_back(resource.first);
    }
    
    return result;
}

CacheEntry ResourceCache::getCacheEntry(const String& resourceId) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        return it->second;
    }
    
    // Return empty entry if not found
    CacheEntry empty;
    empty.resourceId = "";
    empty.size = 0;
    empty.version = 0;
    empty.timestamp = 0;
    empty.lastAccessed = 0;
    empty.priority = 0;
    empty.type = "";
    empty.hash = "";
    
    return empty;
}

void ResourceCache::printCacheStats() {
    Serial.println("=== Cache Statistics ===");
    Serial.println("Hit count: " + String(hitCount));
    Serial.println("Miss count: " + String(missCount));
    Serial.println("Hit ratio: " + String(getHitRatio(), 2) + "%");
    Serial.println("Cached resources: " + String(getCacheSize()));
    
    Serial.println("Cache contents:");
    for (const auto& pair : cacheIndex) {
        if (memoryManager->hasResource(pair.first)) {
            const CacheEntry& entry = pair.second;
            uint32_t age = millis() - entry.lastAccessed;
            Serial.println("  " + pair.first + ": " + 
                          String(entry.size) + " bytes, " +
                          "v" + String(entry.version) + ", " +
                          "priority=" + String(entry.priority) + ", " +
                          "age=" + String(age / 1000) + "s");
        }
    }
    Serial.println("========================");
}

void ResourceCache::setResourcePriority(const String& resourceId, uint8_t priority) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        it->second.priority = priority;
        // Also update in memory manager if resource exists
        if (memoryManager->hasResource(resourceId)) {
            auto memBlocks = memoryManager->allocatedBlocks;
            auto memIt = memBlocks.find(resourceId);
            if (memIt != memBlocks.end()) {
                memIt->second.priority = priority;
            }
        }
    }
}

void ResourceCache::touchResource(const String& resourceId) {
    auto it = cacheIndex.find(resourceId);
    if (it != cacheIndex.end()) {
        it->second.lastAccessed = millis();
        memoryManager->updateAccess(resourceId);
    }
}

bool ResourceCache::validateCacheEntry(const String& resourceId, const CacheEntry& entry) {
    // Check if the resource still exists in memory
    if (!memoryManager->hasResource(resourceId)) {
        return false;
    }
    
    // Additional validation could include hash checking
    // For now, just check memory existence
    return true;
}