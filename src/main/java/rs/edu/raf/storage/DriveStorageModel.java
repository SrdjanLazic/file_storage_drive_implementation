package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DriveStorageModel {

    private String usersJSON;
    private String configJSON;
    private String downloadFolder;
    private String currentStorageName;
    private String currentStorageID;
    private User superuser;
    private User currentUser;
    private long storageSizeLimit;
    private int currNumberOfFiles;
    private boolean maxNumberOfFilesInDirectorySet = false;
    private Map<String, Integer> maxNumberOfFilesInDirectory = new HashMap<>();
    private List<User> userList = new ArrayList<>();
    private List<String> unsupportedExtensions = new ArrayList<>();
    private transient ObjectMapper mapper = new ObjectMapper();

    public DriveStorageModel(){

    }

    public DriveStorageModel(User user, String storageName,String storageID, String downloadFolder, String usersPath, String configPath) {

        // Inicijalizacija parametara:
        this.currNumberOfFiles = 0;
        this.currentStorageName = storageName;
        this.currentStorageID = storageID;
        this.downloadFolder = downloadFolder;
        this.superuser = user;
        this.currentUser = user;
        this.userList.add(user);
        this.usersJSON = usersPath;
        this.configJSON = configPath;

        try {
            mapper.writeValue(new File(usersJSON), user);
            mapper.writeValue(new File(configJSON), this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getCurrentStorageName() {
        return currentStorageName;
    }

    public void setCurrentStorageName(String currentStorageName) {
        this.currentStorageName = currentStorageName;
    }

    public long getStorageSizeLimit() {
        return storageSizeLimit;
    }

    public List<String> getUnsupportedExtensions() {
        return unsupportedExtensions;
    }

    public void setStorageSizeLimit(long storageSizeLimit) {
        this.storageSizeLimit = storageSizeLimit;
    }

    public void setUnsupportedExtensions(List<String> unsupportedExtensions) {
        this.unsupportedExtensions = unsupportedExtensions;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public Map<String, Integer> getMaxNumberOfFilesInDirectory() {
        return maxNumberOfFilesInDirectory;
    }

    public void setMaxNumberOfFilesInDirectory(Map<String, Integer> maxNumberOfFilesInDirectory) {
        this.maxNumberOfFilesInDirectory = maxNumberOfFilesInDirectory;
    }

    public int getCurrNumberOfFiles() {
        return currNumberOfFiles;
    }

    public void setCurrNumberOfFiles(int currNumberOfFiles) {
        this.currNumberOfFiles = currNumberOfFiles;
    }

    public void incrementCounter(){
        this.currNumberOfFiles++;
        updateConfig();
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public String getCurrentStorageID() {
        return currentStorageID;
    }

    public void setCurrentStorageID(String currentStorageID) {
        this.currentStorageID = currentStorageID;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public User getSuperuser() {
        return superuser;
    }

    public void setSuperuser(User superuser) {
        this.superuser = superuser;
    }

    void updateConfig(){
        try {
            mapper.writeValue(new File(configJSON), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void updateUsers(){
        try {
            mapper.writeValue(new File(usersJSON), userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "StorageModel{" +
                "usersJSON=" + usersJSON +
                ", configJSON=" + configJSON +
                ", downloadFolder='" + downloadFolder + '\'' +
                ", currentStorageName='" + currentStorageName + '\'' +
                ", superuser=" + superuser +
                ", currentUser=" + currentUser +
                ", storageSize=" + storageSizeLimit +
                ", userList=" + userList +
                ", unsupportedExtensions=" + unsupportedExtensions +
                ", maxNumberOfFilesInDirectory=" + maxNumberOfFilesInDirectory +
                ", mapper=" + mapper +
                '}';
    }

    public boolean isMaxNumberOfFilesInDirectorySet() {
        return maxNumberOfFilesInDirectorySet;
    }

    public void setMaxNumberOfFilesInDirectorySet(boolean maxNumberOfFilesInDirectorySet) {
        this.maxNumberOfFilesInDirectorySet = maxNumberOfFilesInDirectorySet;
    }

    public String getUsersJSON() {
        return usersJSON;
    }

    public void setUsersJSON(String usersJSON) {
        this.usersJSON = usersJSON;
    }

    public String getConfigJSON() {
        return configJSON;
    }

    public void setConfigJSON(String configJSON) {
        this.configJSON = configJSON;
    }
}
