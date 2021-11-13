package rs.edu.raf.storage;

import com.google.api.services.drive.model.File;

public class FileNameComparator implements java.util.Comparator<com.google.api.services.drive.model.File> {
    @Override
    public int compare(File o1, File o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
}
