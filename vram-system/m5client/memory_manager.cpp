#include "memory_manager.h"
#include <algorithm>

MemoryManager::MemoryManager(size_t maxMem) : 
    totalAllocated(0), 
    maxMemory(maxMem), 
    nextCleanupCheck(0) {
    cleanupThreshold = (size_t)(maxMem * 0.9); // 90% threshold
}

MemoryManager::~MemoryManager() {
    // Free all allocated blocks
    for (auto& pair : allocatedBlocks) {
        free(pair.second.data);
    }
    allocatedBlocks.clear();
}

uint8_t* MemoryManager::allocate(const String& resourceId, size_t size, uint8_t priority) {
    // Check if resource already exists
    if (allocatedBlocks.find(resourceId) != allocatedBlocks.end()) {
        deallocate(resourceId);
    }
    
    // Check if we need cleanup
    if (totalAllocated + size > cleanupThreshold) {
        performCleanup();
    }
    
    // Try to allocate
    uint8_t* ptr = (uint8_t*)malloc(size);
    if (!ptr) {
        // Force cleanup and try again
        performCleanup();
        ptr = (uint8_t*)malloc(size);
        if (!ptr) {
            Serial.println("MEMORY: Allocation failed even after cleanup");
            return nullptr;
        }
    }
    
    // Create memory block record
    MemoryBlock block;
    block.data = ptr;
    block.size = size;
    block.lastAccessed = millis();
    block.accessCount = 1;
    block.priority = priority;
    block.isLocked = false;
    block.resourceId = resourceId;
    
    allocatedBlocks[resourceId] = block;
    totalAllocated += size;
    
    Serial.println("MEMORY: Allocated " + String(size) + " bytes for " + resourceId);
    return ptr;
}

bool MemoryManager::deallocate(const String& resourceId) {
    auto it = allocatedBlocks.find(resourceId);
    if (it != allocatedBlocks.end()) {
        free(it->second.data);
        totalAllocated -= it->second.size;
        allocatedBlocks.erase(it);
        Serial.println("MEMORY: Deallocated " + resourceId);
        return true;
    }
    return false;
}

void MemoryManager::updateAccess(const String& resourceId) {
    auto it = allocatedBlocks.find(resourceId);
    if (it != allocatedBlocks.end()) {
        it->second.lastAccessed = millis();
        it->second.accessCount++;
    }
}

void MemoryManager::setLocked(const String& resourceId, bool locked) {
    auto it = allocatedBlocks.find(resourceId);
    if (it != allocatedBlocks.end()) {
        it->second.isLocked = locked;
    }
}

bool MemoryManager::hasResource(const String& resourceId) {
    return allocatedBlocks.find(resourceId) != allocatedBlocks.end();
}

void MemoryManager::performCleanup() {
    Serial.println("MEMORY: Performing cleanup...");
    
    std::vector<String> candidates = getLRUCandidates();
    size_t targetFree = totalAllocated * 0.3; // Free 30% of used memory
    size_t freedMemory = 0;
    
    for (const String& resourceId : candidates) {
        auto it = allocatedBlocks.find(resourceId);
        if (it != allocatedBlocks.end() && !it->second.isLocked) {
            freedMemory += it->second.size;
            deallocate(resourceId);
            
            if (freedMemory >= targetFree) {
                break;
            }
        }
    }
    
    Serial.println("MEMORY: Cleanup freed " + String(freedMemory) + " bytes");
}

std::vector<String> MemoryManager::getLRUCandidates() {
    std::vector<std::pair<String, size_t>> candidates;
    
    for (const auto& pair : allocatedBlocks) {
        if (!pair.second.isLocked) {
            size_t score = calculateBlockScore(pair.second);
            candidates.push_back({pair.first, score});
        }
    }
    
    // Sort by score (lower score = better candidate for removal)
    std::sort(candidates.begin(), candidates.end(), 
              [](const std::pair<String, size_t>& a, const std::pair<String, size_t>& b) {
                  return a.second < b.second;
              });
    
    std::vector<String> result;
    for (const auto& candidate : candidates) {
        result.push_back(candidate.first);
    }
    
    return result;
}

size_t MemoryManager::calculateBlockScore(const MemoryBlock& block) {
    uint32_t currentTime = millis();
    uint32_t age = currentTime - block.lastAccessed;
    
    // Score based on: priority (higher = keep), access count (higher = keep), age (older = remove)
    // Lower score = better candidate for removal
    size_t score = (block.priority * 1000) + (block.accessCount * 100) - (age / 1000);
    
    return score;
}

void MemoryManager::printMemoryStats() {
    Serial.println("=== Memory Statistics ===");
    Serial.println("Total allocated: " + String(totalAllocated) + " bytes");
    Serial.println("Free memory: " + String(getFreeMemory()) + " bytes");
    Serial.println("Memory usage: " + String(getMemoryUsage(), 1) + "%");
    Serial.println("Number of blocks: " + String(getBlockCount()));
    Serial.println("Cleanup threshold: " + String(cleanupThreshold) + " bytes");
    
    Serial.println("Block details:");
    for (const auto& pair : allocatedBlocks) {
        const MemoryBlock& block = pair.second;
        Serial.println("  " + pair.first + ": " + 
                      String(block.size) + " bytes, " +
                      "priority=" + String(block.priority) + ", " +
                      "access=" + String(block.accessCount) + ", " +
                      "locked=" + String(block.isLocked));
    }
    Serial.println("========================");
}

void MemoryManager::forceCleanup() {
    performCleanup();
}

void MemoryManager::setCleanupThreshold(float percentage) {
    cleanupThreshold = (size_t)(maxMemory * percentage);
    Serial.println("MEMORY: Cleanup threshold set to " + String(cleanupThreshold) + " bytes (" + String(percentage * 100, 1) + "%)");
}