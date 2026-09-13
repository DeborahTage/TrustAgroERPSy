package com.trustagro.user.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "roles")
@Data
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Type(JsonType.class)
    @Column(columnDefinition = "text", nullable = false)
    private List<String> permissions;

    public List<String> getPermissions() { return permissions; }

    public void setName(String name) { this.name = name; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
