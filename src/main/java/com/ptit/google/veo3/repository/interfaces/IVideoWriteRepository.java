package com.ptit.google.veo3.repository.interfaces;

import com.ptit.google.veo3.entity.Video;

/**
 * Write-only repository interface for Video entity
 * Following Interface Segregation Principle - separating write operations
 */
public interface IVideoWriteRepository {
    
    /**
     * Save a video entity
     * @param video Video to save
     * @return Saved video entity
     */
    Video save(Video video);
    
    /**
     * Delete a video by ID
     * @param id Video ID to delete
     */
    void deleteById(Long id);
    
    /**
     * Delete a video entity
     * @param video Video to delete
     */
    void delete(Video video);
    
    /**
     * Flush pending changes to database
     */
    void flush();
}