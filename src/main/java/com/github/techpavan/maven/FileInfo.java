/*
 * Copyright (c) 2018.
 * This code is released under The 3-Clause BSD License.
 * https://github.com/techpavan
 */

package com.github.techpavan.maven;

import lombok.Data;

import java.io.File;
import java.util.Objects;

@Data
public class FileInfo {

    private File file;

    private String groupId;

    private String artifactId;

    private String version;

    public FileInfo getParentFileInfo() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setArtifactId(this.artifactId);
        fileInfo.setGroupId(this.groupId);
        fileInfo.setVersion(this.version);
        fileInfo.setFile(this.file.getParentFile());
        return fileInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return Objects.equals(groupId, fileInfo.groupId) &&
                Objects.equals(artifactId, fileInfo.artifactId) &&
                Objects.equals(version, fileInfo.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }
}