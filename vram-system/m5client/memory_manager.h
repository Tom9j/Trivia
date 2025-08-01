#ifndef MEMORY_MANAGER_H
#define MEMORY_MANAGER_H

#include <Arduino.h>
#include <map>
#include <vector>
#include <string>

struct MemoryBlock {
    uint8_t* data;
    size_t size;
    uint32_t lastAccessed;
    uint32_t accessCount;
    uint8_t priority;
    bool isLocked;
    String resourceId;
};

class MemoryManager {
private:
    std::map<String, MemoryBlock> allocatedBlocks;
    size_t totalAllocated;
    size_t maxMemory;
    size_t cleanupThreshold;
    uint32_t nextCleanupCheck;
    
    // LRU and priority-based cleanup
    void performCleanup();
    std::vector<String> getLRUCandidates();
    size_t calculateBlockScore(const MemoryBlock& block);
    
public:
    MemoryManager(size_t maxMem = 200 * 1024); // Default 200KB
    ~MemoryManager();
    
    // Memory allocation
    uint8_t* allocate(const String& resourceId, size_t size, uint8_t priority = 1);
    bool deallocate(const String& resourceId);
    
    // Memory management
    void updateAccess(const String& resourceId);
    void setLocked(const String& resourceId, bool locked);
    bool hasResource(const String& resourceId);
    
    // Memory monitoring
    size_t getTotalAllocated() const { return totalAllocated; }
    size_t getFreeMemory() const { return maxMemory - totalAllocated; }
    float getMemoryUsage() const { return (float)totalAllocated / maxMemory * 100.0f; }
    size_t getBlockCount() const { return allocatedBlocks.size(); }
    
    // Memory statistics
    void printMemoryStats();
    bool isMemoryLow() const { return totalAllocated >= cleanupThreshold; }
    
    // Cleanup control
    void forceCleanup();
    void setCleanupThreshold(float percentage);
};

#endif