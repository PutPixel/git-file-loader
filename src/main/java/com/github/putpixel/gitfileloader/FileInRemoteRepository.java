package com.github.putpixel.gitfileloader;

import com.google.common.base.Preconditions;

public class FileInRemoteRepository {

    private final String pathInRepo;

    private final String objectId;

    public FileInRemoteRepository(String objectId, String pathInRepo) {
        this.objectId = Preconditions.checkNotNull(objectId);
        this.pathInRepo = Preconditions.checkNotNull(pathInRepo);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
        result = prime * result + ((pathInRepo == null) ? 0 : pathInRepo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileInRemoteRepository other = (FileInRemoteRepository) obj;
        if (objectId == null) {
            if (other.objectId != null) {
                return false;
            }
        }
        else if (!objectId.equals(other.objectId)) {
            return false;
        }
        if (pathInRepo == null) {
            if (other.pathInRepo != null) {
                return false;
            }
        }
        else if (!pathInRepo.equals(other.pathInRepo)) {
            return false;
        }
        return true;
    }

    public String getPathInRepo() {
        return pathInRepo;
    }

    public String getObjectId() {
        return objectId;
    }

}
