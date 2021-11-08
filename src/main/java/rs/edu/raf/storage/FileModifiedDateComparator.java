package rs.edu.raf.storage;

import com.google.api.services.drive.model.File;

public class FileModifiedDateComparator implements java.util.Comparator<com.google.api.services.drive.model.File> {

    @Override
    public int compare(File o1, File o2) {
        return Long.compare(o1.getModifiedTime().getValue(), o2.getModifiedTime().getValue());
    }
}
