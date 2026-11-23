package com.chat.modules.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Chứa metadata và đường dẫn tải của một file để BE trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private UUID id;
    private String name;
    private String contentType;
    private long size;
    private String downloadUrl;
}
