package com.filesystem.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_metadata")
public class UserMetadata {
    private String userId;
    private String username;
    private String company;
    private String role;

}


