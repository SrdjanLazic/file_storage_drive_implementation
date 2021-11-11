package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DriveStorageModel {

    //TODO: DODAJ POLJE storageID !!!!!

    private java.io.File usersJSON;
    private java.io.File configJSON;
    private String downloadFolder;
    private String rootDirectory;
    private User superuser;
    private User currentUser;
    private int currNumberOfFiles;
    private List<User> userList = new ArrayList<>();
    private transient ObjectMapper mapper = new ObjectMapper();

    public DriveStorageModel(){

    }

    public DriveStorageModel(User user, String storageName, String downloadFolder, java.io.File usersPath, java.io.File configPath) {

        // Inicijalizacija parametara:
        this.currNumberOfFiles = 1;  //1 zbog download foldera
        this.rootDirectory = storageName;
        this.downloadFolder = downloadFolder;
        this.superuser = user;
        this.currentUser = user;
        this.userList.add(user);
        this.usersJSON = usersPath;  // TODO: pisanje svih juzera
        this.configJSON = configPath;

        try {
            mapper.writeValue(usersJSON, user);
            mapper.writeValue(configJSON, this);
            currNumberOfFiles += 2; // inkrementiramo trenutni broj fajlova u skladistu
            updateConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
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

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
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
            mapper.writeValue(configJSON, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void updateUsers(){
        try {
            mapper.writeValue(usersJSON, userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
