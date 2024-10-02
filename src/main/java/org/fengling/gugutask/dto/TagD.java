package org.fengling.gugutask.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagD {
    private Long Id;
    private String tagName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public TagD() {
    }

    public TagD(Long Id, String tagName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.Id = Id;
        this.tagName = tagName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
