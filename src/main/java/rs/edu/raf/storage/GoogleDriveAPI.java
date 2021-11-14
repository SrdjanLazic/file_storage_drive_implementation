package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import rs.edu.raf.storage.enums.Operations;
import rs.edu.raf.storage.enums.Privileges;
import rs.edu.raf.storage.exceptions.*;

import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoogleDriveAPI implements FileStorage {

    static{
        StorageManager.registerStorage(new GoogleDriveAPI());
    }

    //#TODO: Da li su ovo suvisni fieldovi?
    private String currentStorage;
    private String currentStorageID;
    private DriveStorageModel currentStorageModel;
    private List<DriveStorageModel> driveStorageModelList = new ArrayList<>();
    private Drive service;


    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "File Storage Manager Specification";

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials at
     * ~/.credentials/calendar-java-quickstart
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveAPI.class.getResourceAsStream("/client1_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized Calendar client service.
     *
     * @return an authorized Calendar client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }



    // IMPLEMENTACIJA METODA IZ FILE STORAGE-A
    //TODO: Dodati exceptione i rad sa njima u vezi sa metodama iz specifikacije

    @Override
    public void createFolder(String path, String ... folderNames) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException {
        //provera da li trenutni korisnik ima privilegiju za kreiranje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        String folderId = findID(path);
        if(folderId == null){
            System.out.println("Nije pronadjena lokacija gde zelite napraviti folder");
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }
        for(String i : folderNames) {
            String folderName = i;
            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            fileMetadata.setParents(Collections.singletonList(folderId));
            getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() + 1);

            File file = null;
            try {
                file = service.files().create(fileMetadata)
                        .setFields("id, name, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Folder " + file.getName() + " created in Folder " + file.getParents() + " !");
            System.out.println("Folder ID: " + file.getId());
        }
        reuploadJSONS();
    }

    @Override
    public void createFile(String path, String ... filenames) throws InvalidExtensionException, InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException, FileLimitExceededException {

        //provera da li trenutni korisnik ima privilegiju za kreiranje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }


        String folderId = findID(path);
        if(folderId == null){
            System.out.println("Nije pronadjena lokacija gde zelite napraviti fajl");
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }
        for(String i : filenames) {
            // Provera da li cemo prekoraciti broj fajlova u nekom folderu:
            // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
            // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
            if(currentStorageModel.getMaxNumberOfFilesInDirectory().containsKey(currentStorageModel.getCurrentStorageName())){
                int numberOfFiles = currentStorageModel.getCurrNumberOfFiles(); //TODO: samo radi za root, srediti da radi i za fajlove
                if(numberOfFiles + 1 > currentStorageModel.getMaxNumberOfFilesInDirectory().get(getCurrentStorageModel()))
                    throw new FileLimitExceededException();
            }

            if(checkExtensions(i)){
                throw new InvalidExtensionException();
            }

            String filename = i;
            File fileMetadata = new File();
            fileMetadata.setName(filename);
            fileMetadata.setParents(Collections.singletonList(folderId));
            //java.io.File filePath = new java.io.File();
            //FileContent mediaContent = new FileContent(null, filePath);
            getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() + 1);

            File file = null;
            try {
                file = service.files().create(fileMetadata)
                        .setFields("id, name, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File " + file.getName() + " created in Folder " + file.getParents() + " !");
            System.out.println("File ID: " + file.getId());
        }
        reuploadJSONS();
    }

    @Override
    public void createFolder(String folderName) throws InsufficientPrivilegesException{

        //provera da li trenutni korisnik ima privilegiju za kreiranje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setParents(Collections.singletonList(getCurrentStorageID()));

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                        .setFields("id, name, parents")
                        .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder " + file.getName() + " created!");
        System.out.println("Folder ID: " + file.getId());
        getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() + 1);
        reuploadJSONS();
    }

    @Override
    public void createFile(String filename) throws InvalidExtensionException, InsufficientPrivilegesException, FileLimitExceededException{

        //provera da li trenutni korisnik ima privilegiju za kreiranje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        if(checkExtensions(filename)){
            throw new InvalidExtensionException();
        }

        // Provera da li cemo prekoraciti broj fajlova u nekom folderu:
        // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
        // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
        if(currentStorageModel.getMaxNumberOfFilesInDirectory().containsKey(currentStorageModel.getCurrentStorageName())){
            int numberOfFiles = currentStorageModel.getCurrNumberOfFiles(); //TODO: samo radi za root, srediti da radi i za fajlove
            if(numberOfFiles + 1> currentStorageModel.getMaxNumberOfFilesInDirectory().get(getCurrentStorageModel()))
                throw new FileLimitExceededException();
        }

        File fileMetadata = new File();
        fileMetadata.setName(filename);
        fileMetadata.setParents(Collections.singletonList(getCurrentStorageID()));

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                        .setFields("id, name, parents")
                        .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File " + file.getName() + " created!");
        System.out.println("File ID: " + file.getId());
        getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() + 1);
        reuploadJSONS();
    }

    @Override
    public void delete(String ... paths) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException, FileDeleteFailedException{

        //provera da li trenutni korisnik ima privilegiju za brisanje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.DELETE)){
            throw new InsufficientPrivilegesException();
        }

        for(String i: paths) {
            String path = i;
            String fileId = findID(path);

            if(fileId == null)
                throw new rs.edu.raf.storage.exceptions.FileNotFoundException();

            //TODO: Exceptione za ova dva if-a
            if(!currentStorageModel.getCurrentUser().equals(currentStorageModel.getSuperuser()) && path.contentEquals("users.json") || path.contentEquals("config.json")){
                System.out.println("Nije moguce obrisati users.json i config.json fajlove");
                throw new InsufficientPrivilegesException();
            }
            else if (!currentStorageModel.getCurrentUser().equals(currentStorageModel.getSuperuser()) && getFile(path).getName().contentEquals(currentStorage)) {
                System.out.println("Nije moguce obrisati skladiste, vec samo fajlove i direktorijume unutar njega.");
                throw new InsufficientPrivilegesException();
            }
            else {
                try {
                    service.files().delete(fileId).execute();
                } catch (IOException e) {
                    System.out.println("An error occurred: " + e);
                    throw new FileDeleteFailedException();
                }
                getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() - 1);
                System.out.println("File " + path + " deleted!");
            }
        }
        reuploadJSONS();
    }

    @Override
    public void move(String destination, String ... sources) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException, FileLimitExceededException, StorageSizeExceededException, InvalidExtensionException {
        //provera da li trenutni korisnik ima privilegiju za kreiranje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        String folderId = findID(destination);
        if(folderId == null){
            System.out.println("Nije pronadjena lokacija gde zelite napraviti fajl");
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }


        for(String i : sources) {
            String source = i;
            String fileId = findID(source);

            //provera za maximalan broj fajlova
            if(currentStorageModel.getMaxNumberOfFilesInDirectory().containsKey(currentStorageModel.getCurrentStorageName())){
                int numberOfFiles = currentStorageModel.getCurrNumberOfFiles(); //TODO: samo radi za root, srediti da radi i za fajlove
                if(numberOfFiles + 1 > currentStorageModel.getMaxNumberOfFilesInDirectory().get(getCurrentStorageModel()))
                    throw new FileLimitExceededException();
            }

            if(fileId == null){
                System.out.println("Nije pronadjen fajl koji zelite da prebacite");
                throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
            }

            if(checkExtensions(source)){
                throw new InvalidExtensionException();
            }

            //provera za storage size nije moguca na driveu
            //TODO: saberi size svih fajlova i podfajlova u storageu za storage size

            // Retrieve the existing parents to remove
            File file = null;
            try {
                file = service.files().get(fileId)
                        .setFields("parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //TODO: Iskoristiti ovaj kod za prolazak kroz liste
            StringBuilder previousParents = new StringBuilder();
            for (String parent : file.getParents()) {
                previousParents.append(parent);
                previousParents.append(',');
            }
            // Move the file to the new folder
            try {
                file = service.files().update(fileId, null)
                        .setAddParents(folderId)
                        .setRemoveParents(previousParents.toString())
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void list(String path) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException {

        //provera da li trenutni korisnik ima privilegiju za citanje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        String folderID = findID(path);
        String type;
        FileList result = null;


        if(folderID == null){
            System.out.println("Nije pronadjena lokacija odakle zelite da izlistate fajlove");
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }

        try {
            result = service.files().list()
                    .setFields("files(id, parents, name, mimeType, size, description)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            f.getId();
        }
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                //ne radi subfoldere i subfajlove trenutno #TODO: Iskoristiti kod iz movea

                if(!(file.getParents()==null) && file.getParents().contains(folderID)) {
                    Long size;
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
                    type = (file.getMimeType().equals("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                    if (file.getSize() == null) {
                        size = Long.valueOf(0);
                    } else {
                        size = file.getSize();
                    }
                    System.out.println(" --- " + size / 1024 + " kB " + " --- " + type);
                }
            }
        }
    }

    @Override
    public void list(String path, String argument, Operations operation) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException {
        //provera da li trenutni korisnik ima privilegiju za citanje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        String folderID = findID(path);
        String type;
        FileList result = null;

        if(folderID == null){
            System.out.println("Nije pronadjena lokacija odakle zelite da izlistate fajlove");
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }

        try {
            result = service.files().list()
                    .setFields("files(id, parents, name, mimeType, size, modifiedTime, createdTime, description)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            f.getId();
        }
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        }
        if (operation == Operations.FILTER_EXTENSION) {
            String extension = argument;
            System.out.println("\nLista fajlova sa datom ekstenzijom u skladistu:");
            System.out.println("------------------------------------------------\n");
            for (File file : files) {
                if (file.getName().endsWith(extension) && !(file.getParents()==null) && file.getParents().contains(folderID))
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        } else if (operation == Operations.FILTER_FILENAME) {
            String filename = argument;
            System.out.println("\nLista fajlova ciji nazivi sadrze dati tekst:");
            System.out.println("----------------------------------------------\n");
            for (File file : files) {
                if (file.getName().contains(filename) && !(file.getParents()==null) && file.getParents().contains(folderID))
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        } else if (operation == Operations.SORT_BY_NAME_ASC || operation == Operations.SORT_BY_NAME_DESC) {
            String order;
            if(operation == Operations.SORT_BY_NAME_ASC) {
                files.sort(new FileNameComparator());
                order = " rastuce ";
            }
            else {
                files.sort(new FileNameComparator().reversed());
                order = " opadajuce ";
            }

            System.out.println("\nLista fajlova i foldera sortirana" + order + "po nazivu:");
            System.out.println("-----------------------------------------------------\n");
            for (File file : files) {
                String name;
                Long size;
                if(file.getMimeType() == null){
                    type = "FILE";
                } else{
                    type = (file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                }
                if(file.getName() == null){
                    name = "Untitled";
                } else{
                    name = file.getName();
                }
                if(file.getSize() == null){
                    size = Long.valueOf(0);
                } else{
                    size = file.getSize();
                }
                if(!(file.getParents()==null) && file.getParents().contains(getCurrentStorageID()))
                    System.out.println( name + " --- " + size / (1024) + " kB " + " --- " + type);
            }
        } else if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC || operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
            String order;
            if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC) {
                files.sort(new FileModifiedDateComparator());
                order = " rastuce ";
            } else {
                files.sort(new FileModifiedDateComparator().reversed());
                order = " opadajuce ";
            }
           // SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            System.out.println("\nLista fajlova i foldera sortirana" + order + "po datumu izmene:");
            System.out.println("-----------------------------------------------------\n");
            for (File file : files) {
                String name;
                Long size;
                DateTime modtime;
                if(file.getMimeType() == null){
                    type = "FILE";
                } else{
                    type = (file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                }
                if(file.getName() == null){
                    name = "Untitled";
                } else{
                    name = file.getName();
                }
                if(file.getSize() == null){
                    size = Long.valueOf(0);
                } else{
                    size = file.getSize();
                }
                if(file.getModifiedTime() == null){
                    modtime = file.getCreatedTime();
                } else{
                    modtime = file.getModifiedTime();
                }
                //type = (file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                if(!(file.getParents()==null) && file.getParents().contains(folderID))
                    System.out.println(name + " --- " + size / (1024) + " kB" + " --- " + type + " --- " + modtime.toStringRfc3339());
            }
        } else if (operation == Operations.SORT_BY_DATE_CREATED) {
            files.sort(new FileDateCreatedComparator());
            //SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            System.out.println("\nLista fajlova i foldera sortirana po datumu kreiranje:");
            System.out.println("-----------------------------------------------------\n");
            for (File file : files) {
                Long size;
                if(file.getSize() == null){
                    size = Long.valueOf(0);
                } else{
                    size = file.getSize();
                }
                type = (file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                if(!(file.getParents()==null) && file.getParents().contains(folderID))
                    System.out.println(file.getName() + " --- " + size / (1024) + " kB" + " --- " + type + " --- " + file.getCreatedTime().toStringRfc3339());
            }
        }
    }


    @Override
    public void put(String destination, String ... sources) throws InsufficientPrivilegesException, InvalidExtensionException, FileLimitExceededException, StorageSizeExceededException, FileAlreadyInStorageException{
        //provera da li trenutni korisnik ima privilegiju za upisivanje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }


        String folderId = findID(destination);
        for(String i : sources) {
            //provera za maximalan broj fajlova
            if(currentStorageModel.getMaxNumberOfFilesInDirectory().containsKey(currentStorageModel.getCurrentStorageName())){
                int numberOfFiles = currentStorageModel.getCurrNumberOfFiles(); //TODO: samo radi za root, srediti da radi i za fajlove
                if(numberOfFiles > currentStorageModel.getMaxNumberOfFilesInDirectory().get(getCurrentStorageModel()))
                    throw new FileLimitExceededException();
            }

            String source = i;
            Path original = Paths.get(source);
            if (checkExtensions(source)) {
                throw new InvalidExtensionException();
            }

            if(findFile(source))
                throw new FileAlreadyInStorageException();

            //TODO: provere za SIZE EXCEEDED
            try{
                long size = Files.size(original);
                //if(currentStorageModel.getCurrent)
            } catch (IOException e){

            }
            java.io.File temp = new java.io.File(String.valueOf(original));

            File fileMetadata = new File();
            fileMetadata.setName(temp.getName());
            if (temp.isDirectory()) {
                fileMetadata.setMimeType("application/vnd.google-apps.folder");
            }
            fileMetadata.setParents(Collections.singletonList(folderId));
            //java.io.File filePath = new java.io.File();
            FileContent mediaContent = new FileContent(null, temp);

            File file = null;
            try {
                file = service.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int filecount = getCurrentStorageModel().getCurrNumberOfFiles();
            getCurrentStorageModel().setCurrNumberOfFiles(filecount + 1);
            System.out.println("File uploaded into Google Drive Storage - File ID: " + file.getId());
        }
        reuploadJSONS();
    }

    @Override
    public void get(String ... paths) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException {

        //provera da li trenutni korisnik ima privilegiju za citanje
        if(!currentStorageModel.getCurrentUser().getPrivileges().contains(Privileges.DOWNLOAD)){
            throw new InsufficientPrivilegesException();
        }

        for(String i : paths) {
            String path = i;
            String fileId = findID(path);
            String fileName = getFile(path).getName();
            String fileMime = getFile(path).getMimeType();
            System.out.println(fileMime);

            if(fileId == null){
                System.out.println("Nije pronadjen fajl koji zelite da prebacite");
                throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
            }

            //print file metadata
            try {
                File file = service.files().get(fileId).execute();

                System.out.println("Name: " + file.getName());
                System.out.println("Description: " + file.getDescription());
                System.out.println("MIME type: " + file.getMimeType());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
            }


            //download the file
            OutputStream outputStream = null;
            boolean googleDocCheck = false;
            try {
                outputStream = new FileOutputStream("C:/" + currentStorage + "/download/" + fileName);
                //System.out.println("Filename  " + fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                if (fileMime.contains("vnd.google-apps.document")) {
                    System.out.println("File is a google doc");
                    service.files().export(fileId, "application/pdf")
                            .executeMediaAndDownloadTo(outputStream);
                    outputStream.flush();
                    outputStream.close();
                    System.out.println("File downloaded!");
                } else {
                    service.files().get(fileId)
                            .executeMediaAndDownloadTo(outputStream);
                    outputStream.flush();
                    outputStream.close();
                    System.out.println("File downloaded!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    //Inicijalizovanje remote storagea na google driveu
    @Override
    public void initializeStorage(String rootName) {
        try {
            this.service = getDriveService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Da li treba napraviti currentDisk kao promenljivu?
        //local storage path kreiranje
        String rootPath = "C:/" + rootName;
        java.io.File storageRoot = new java.io.File(rootPath);
        if(!storageRoot.exists())
            storageRoot.mkdir();

        //lokalni download folder
        java.io.File downloadFolder = new java.io.File(rootPath + "/download");
        String downloadPath = rootPath + "/download";
        if(!downloadFolder.exists())
            downloadFolder.mkdir();

        //putanje do json fajlova
        String configLocalPath = rootPath + "/config.json";
        String usersLocalPath = rootPath + "/users.json";

        boolean isStorage = false;
        Scanner scanner = new Scanner(System.in);

        // Proveravamo ako vec postoji folder sa ovim imenom koji je skladiste (uz pomoc description taga 'SKLADISTE')
        if(findFile(rootName)) {
            //TODO: popraviti security ili pristup ovoj stavci za SKLADISTE
            if (getFile(rootName).getDescription().equalsIgnoreCase("SKLADISTE") ||
                    (getFile("config.json").getParents().contains(rootName) &&
                            getFile("users.json").getParents().contains(rootName))){
                isStorage = true;
                currentStorage = rootName;
                currentStorageID = findID(rootName);
            }
        }

        // Ako jeste skladiste, procitaj user i config fajlove
        if(isStorage){
            System.out.println("Direktorijum je vec skladiste. Unesite username i password kako biste se konektovali na skladiste.");
            System.out.println("Username:");
            String username = scanner.nextLine();
            System.out.println("Password:");
            String password = scanner.nextLine();
            ObjectMapper objectMapper = new ObjectMapper();

            List<User> users = new ArrayList<>();
            try {
                users = Arrays.asList(objectMapper.readValue(new java.io.File(usersLocalPath),User[].class)) /*objectMapper.readValue(new java.io.File(usersLocalPath), User.class)*/;
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean found = false;
            // Provera kredencijala - uporedjivanje prosledjenih username i password-a i procitanih iz users.json fajla
            for(User user: users) {
                if (username.equalsIgnoreCase(user.getUsername()) && password.equalsIgnoreCase(user.getPassword())) {
                    found = true;
                    try {
                        // Ako se podaci User-a match-uju, procitaj config, setuj trenutni storage i dodaj skladiste u listu skladista
                        DriveStorageModel storageModel = objectMapper.readValue(new java.io.File(configLocalPath), DriveStorageModel.class);
                        this.driveStorageModelList.add(storageModel);
                        setCurrentStorageModel(storageModel);
                        currentStorageModel.setCurrentUser(user);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //ukoliko je boolean ostao false
            if(!found) {
                throw new UserNotFoundException();
            }
            // Pravimo novo skladiste, prilikom kreiranja User-u koji ga je kreirao dodeljujemo sve privilegije
        } else {
            System.out.println("Direktorijum nije skladiste. Da li zelite da kreirate novo skladiste? Unesite DA ili NE");
            String choice = scanner.nextLine();

            if(choice.equalsIgnoreCase("DA")){

                createStorage(rootName);
                String rootID = findID(rootName);

                System.out.println("Unesite username i password kako biste kreirali skladiste.");
                System.out.println("Username:");
                String username = scanner.nextLine();
                System.out.println("Password:");
                String password = scanner.nextLine();

                User user = new User(username, password);
                user.setPrivileges(Set.of(Privileges.values()));

//                //pravimo users.json i config.json U DRIVEU
//                File usersDrive = new File().setName("users.json");
//                File configDrive = new File().setName("config.json");

//                //pravimo users.json i config.json U LOKALNOM
//                java.io.File usersLocal = new java.io.File(usersLocalPath);
//                java.io.File configLocal = new java.io.File(configLocalPath);

                DriveStorageModel storageModel = new DriveStorageModel(user, rootName, rootID , downloadPath, usersLocalPath, configLocalPath);
                this.driveStorageModelList.add(storageModel);
                setCurrentStorageModel(storageModel);
                put(rootName, usersLocalPath, configLocalPath);

            } else {
                System.out.println("Skladiste nije kreirano.");
                return;
            }

        }

    }

    @Override
    public void limitNumberOfFiles(int fileNumberMax, String path) throws InsufficientPrivilegesException, rs.edu.raf.storage.exceptions.FileNotFoundException {

        // Provera da li superuser poziva metodu
        if(!currentStorageModel.getSuperuser().equals(currentStorageModel.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        java.io.File directory = new java.io.File(path);

        // Provera da li postoji prosledjeni folder
        if(!findFile(path)){
            throw new rs.edu.raf.storage.exceptions.FileNotFoundException();
        }

        // Dodavanje novog para direktorijum-brFajlova u HashMap trenutnog skladista
        currentStorageModel.getMaxNumberOfFilesInDirectory().put(path, Integer.valueOf(fileNumberMax));
        currentStorageModel.setMaxNumberOfFilesInDirectorySet(true);
        reuploadJSONS();
    }

    @Override
    public void limitStorageSize(long limit) throws InsufficientPrivilegesException{

        //Proveravamo da li superuser poziva metodu
        if(!currentStorageModel.getSuperuser().equals(currentStorageModel.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        currentStorageModel.setStorageSizeLimit(limit);
        currentStorageModel.setStorageSizeLimit(limit);
        reuploadJSONS();
    }

    @Override
    public void restrictExtension(String extension) throws InsufficientPrivilegesException{

        //Proveravamo da li superuser poziva metodu
        if(!currentStorageModel.getSuperuser().equals(currentStorageModel.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        currentStorageModel.getUnsupportedExtensions().add(extension);
        reuploadJSONS();

    }

    @Override
    public void addNewUser(User user, Set<Privileges> privileges) throws InsufficientPrivilegesException, UserAlreadyExistsException {
        //Proveravamo da li superuser poziva metodu
        if(!currentStorageModel.getSuperuser().equals(currentStorageModel.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        //proveravamo da li korisnik vec postoji
        if(currentStorageModel.getUserList().contains(user)){
            throw new UserAlreadyExistsException();
        }

        user.setPrivileges(privileges);
        currentStorageModel.getUserList().add(user);
        reuploadJSONS();
    }


    @Override
    public void removeUser(User user) throws UserNotFoundException, InsufficientPrivilegesException {
        //Proveravamo da li superuser poziva metodu
        if(!currentStorageModel.getSuperuser().equals(currentStorageModel.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        //Proveravamo da li postoji odabrani user za brisanje
        boolean userFound = false;
        User targetUser = null;
        for(User u : currentStorageModel.getUserList()){
            if(u.getUsername().equalsIgnoreCase(user.getUsername())){
                userFound = true;
            }
        }

        if(userFound){
            currentStorageModel.getUserList().remove(targetUser);
            reuploadJSONS();
        } else{
            throw new UserNotFoundException();
        }
    }

    //TODO: Adaptirano ali treba testirati
    @Override
    public void login(User user) throws UserAlreadyLoggedInException, UserNotFoundException{

        User connectingUser = null;
        boolean found = false;

        for(User u : currentStorageModel.getUserList()){
            if(u.equals(user)){
                found = true;
                connectingUser = u;
            }
        }
        if(!found)
            throw new UserNotFoundException();

        if(currentStorageModel.getCurrentUser().equals(connectingUser))
            throw new UserAlreadyLoggedInException();

        currentStorageModel.setCurrentUser(connectingUser);
        reuploadJSONS();
    }

    @Override
    public void logout(User user) throws UserNotFoundException, UserLogoutException {
        User disconnectingUser = null;

        for(User u : currentStorageModel.getUserList()){
            if(u.equals(disconnectingUser)){
                disconnectingUser = u;
            }
        }

        if(disconnectingUser==null)
            throw new UserNotFoundException();

        if(!currentStorageModel.getCurrentUser().equals(disconnectingUser))
            throw new UserLogoutException();

        currentStorageModel.setCurrentUser(null);
        reuploadJSONS();
    }


    //POMOCNE METODE
    private void createStorage(String folderName) {
        currentStorage = folderName;
        currentStorageID = findID(folderName);
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setDescription("SKLADISTE");

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Storage " + folderName + " created!");
        System.out.println("Storage ID: " + file.getId());
    }

    public String findID(String s){
        FileList result = null;
        String id = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size, description)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            if(f.getName().equalsIgnoreCase(s)) {
                id = f.getId();
                System.out.println("File/Folder " + f.getName() + " ID found!");
            }
        }
        return id;
    }

    protected boolean findFile(String s){
        FileList result = null;
        String id = null;
        boolean check = false;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size, description)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            if(f.getName().equalsIgnoreCase(s)) {
                id = f.getName();
                if(s.contentEquals(id))
                    check = true;
                System.out.println("File/Folder "+ id +" found!");
            }
        }
        return check;
    }

    public File getFile(String filename){
        FileList result = null;
        File targetFile = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size, description)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            if(f.getName().equalsIgnoreCase(filename)) {
                targetFile = f;
                System.out.println("File/Folder " + f.getName() + " found and returned!");
            }
        }
        return targetFile;
    }

    private void reuploadJSONS() {
        getCurrentStorageModel().updateConfig();
        getCurrentStorageModel().updateUsers();
        reupload(currentStorage, getCurrentStorageModel().getConfigJSON(), getCurrentStorageModel().getUsersJSON());
    }

    private void reupload(String destination, String ... sources) {
        String folderId = currentStorageID;
        //delete old json on drive
        for(String i : sources) {
            String source = i;
            if(source.contains("config"))
                source = "config.json";
            else if(source.contains("users"))
                source = "users.json";
            String fileId = findID(source);
            try {
                service.files().delete(fileId).execute();
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
            }
            getCurrentStorageModel().setCurrNumberOfFiles(getCurrentStorageModel().getCurrNumberOfFiles() - 1);
            System.out.println("File " + source + " deleted!");
        }
        //reupload new one
        for(String i : sources) {
            String source = i;
            Path original = Paths.get(source);
            java.io.File temp = new java.io.File(String.valueOf(original));

            File fileMetadata = new File();
            //fileMetadata.setName(temp.getName());
            fileMetadata.setName(temp.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));
            //java.io.File filePath = new java.io.File();
            FileContent mediaContent = new FileContent(null, temp);

            File file = null;
            try {
                file = service.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int filecount = getCurrentStorageModel().getCurrNumberOfFiles();
            getCurrentStorageModel().setCurrNumberOfFiles(filecount + 1);
            System.out.println("File reuploaded into Google Drive Storage - File ID: " + file.getId());
        }
    }

    // Pomocna metoda za proveravanje ekstenzije prilikom dodavanja fajla u skladiste
    private boolean checkExtensions(String filename){
        boolean found = false;
        for(String extension: currentStorageModel.getUnsupportedExtensions()){
            if(filename.endsWith(extension))
                found = true;
        }
        return found;
    }


    //GETTERI I SETTERI

    public DriveStorageModel getCurrentStorageModel() {
        return currentStorageModel;
    }

    public void setCurrentStorageModel(DriveStorageModel currentStorageModel) {
        this.currentStorageModel = currentStorageModel;
    }

    public String getCurrentStorage() {
        return currentStorage;
    }

    public void setCurrentStorage(String currentStorage) {
        this.currentStorage = currentStorage;
    }

    public String getCurrentStorageID() {
        return currentStorageID;
    }

    public void setCurrentStorageID(String currentStorageID) {
        this.currentStorageID = currentStorageID;
    }
}
