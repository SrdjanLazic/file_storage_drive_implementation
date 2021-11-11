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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoogleDriveAPI implements FileStorage {

    static{
        StorageManager.registerStorage(new GoogleDriveAPI());
    }

    private List<DriveStorageModel> driveStorageModelList = new ArrayList<>();
    private DriveStorageModel currentStorage;
    private Drive service;
    private FileStorage fileStorageInstance;


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


    /*
    public static void main(String[] args) throws IOException {

        Drive service = getDriveService();



        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }



    }
    */


    @Override
    public void createFolder(String path, String folderName) {
        /*
        FileList result = null;
        String folderId = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            if(f.getName().equalsIgnoreCase(path)) {
                folderId = f.getId();
            }
        }
        */

        String folderId = findID(path);
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setParents(Collections.singletonList(folderId));
        //java.io.File filePath = new java.io.File(path);
        //FileContent mediaContent = new FileContent("image/jpeg", filePath);

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId());
    }

    @Override
    public void createFile(String path, String filename) {

        String folderId = findID(path);

        File fileMetadata = new File();
        fileMetadata.setName(filename);
        fileMetadata.setParents(Collections.singletonList(folderId));
        //java.io.File filePath = new java.io.File();
        //FileContent mediaContent = new FileContent("image/jpeg", filePath);

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File created!");
        System.out.println("File ID: " + file.getId());
    }

    @Override
    public void createFolder(String folderName) {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder created!");
        System.out.println("Folder ID: " + file.getId());
    }

    @Override
    public void createFile(String filename) {
        File fileMetadata = new File();
        fileMetadata.setName(filename);


        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File created!");
        System.out.println("File ID: " + file.getId());
    }

    @Override
    public void delete(String path) {
        String fileId = findID(path);
        try {
            service.files().delete(fileId).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
        System.out.println("File " + path + " deleted!");
    }

    @Override
    public void move(String source, String destination) {

        String fileId = findID(source);
        String folderId = findID(destination);
        /*
        FileList result = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        for(File f: files){
            if(f.getName().equalsIgnoreCase(source)) {
                fileId = f.getId();
            }
            if(f.getName().equalsIgnoreCase(destination)){
                folderId = f.getId();
            }
        }
         */

        // Retrieve the existing parents to remove
        File file = null;
        try {
            file = service.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public void list() {
        String type;
        FileList result = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size, description)")
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
                Long size;
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
                type = (file.getMimeType().equals("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                if(file.getSize() == null){
                    size = Long.valueOf(0);
                } else{
                    size = file.getSize();
                }
                System.out.println(" --- " + size/1024 + " kB " + " --- " + type);
            }
        }
    }

    @Override
    public void list(String argument, Operations operation) {
        String type;
        FileList result = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size, modifiedTime, createdTime, description)")
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
        } /*else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }*/

        if (operation == Operations.FILTER_EXTENSION) {
            String extension = argument;
            System.out.println("\nLista fajlova sa datom ekstenzijom u skladistu:");
            System.out.println("------------------------------------------------\n");
            for (File file : files) {
                if (file.getName().endsWith(extension))
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        } else if (operation == Operations.FILTER_FILENAME) {
            String filename = argument;
            System.out.println("\nLista fajlova ciji nazivi sadrze dati tekst:");
            System.out.println("----------------------------------------------\n");
            for (File file : files) {
                if (file.getName().contains(filename))
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
                System.out.println( name + " --- " + size / (1024) + " kB " + " --- " + type);
            }
            /*
            if(order.equalsIgnoreCase(" rastuce ")) {

            } else {
                for (int i = files.size(); i-- > 0; ) {
                    type = (files.get(i).getMimeType().equals("application/vnd.google-apps.folder")) ? "FOLDER" : "FILE";
                    System.out.println(files.get(i).getName() + " --- " + files.size() / (1024 * 1024) + " MB " + " --- " + type);
                }
            }
             */
        } else if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC || operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
            String order;
            if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC) {
                files.sort(new FileModifiedDateComparator());
                order = " rastuce ";
            } else {
                files.sort(new FileModifiedDateComparator().reversed());
                order = " opadajuce ";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

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
                System.out.println(name + " --- " + size / (1024) + " kB" + " --- " + type + " --- " + modtime.toStringRfc3339());
            }
        } else if (operation == Operations.SORT_BY_DATE_CREATED) {
            files.sort(new FileDateCreatedComparator());
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
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
                System.out.println(file.getName() + " --- " + size / (1024) + " kB" + " --- " + type + " --- " + file.getCreatedTime().toStringRfc3339());
            }
        }
    }


    @Override
    public void put(String sources, String destination) {

        String folderId = findID(destination);

        Path original = Paths.get(sources);
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
        System.out.println("File uploaded into Google Drive Storage - File ID: " + file.getId());
    }

    @Override
    public void get(String path) {

        String fileId = findID(path);
        String fileName = getFile(path).getName();
        String fileMime = getFile(path).getMimeType();

        //print file metadata
        try {
            File file = service.files().get(fileId).execute();

            System.out.println("Name: " + file.getName());
            System.out.println("Description: " + file.getDescription());
            System.out.println("MIME type: " + file.getMimeType());
            fileMime = file.getMimeType();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }


        //download the file
        OutputStream outputStream = null;
        boolean googleDocCheck = false;
        try {
            outputStream = new FileOutputStream("C:/skladiste/download/" + fileName);
            System.out.println("Filename  " + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            service.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
            System.out.println("File downloaded!");
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("File is a google doc:");
            googleDocCheck = true;
            try {
                outputStream.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            try {
                //OutputStream outputStream1 = new FileOutputStream();
                service.files().export(fileId, fileMime)
                        .executeMediaAndDownloadTo(outputStream);
                outputStream.flush();
                outputStream.close();
                System.out.println("File downloaded!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }


    //provera da li vec postoji storage

    @Override
    public void initializeStorage(String rootName) {
        try {
            this.service = getDriveService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: redosled SVIH linija koda

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


        // TODO proveriti da li je findFile validna metoda za ovako nesto?
        if(findFile(rootName)) {
            System.out.println(getFile(rootName).getDescription());
            if (getFile(rootName).getDescription().equalsIgnoreCase("SKLADISTE"))
                isStorage = true;
        }

        // Ako jeste skladiste, procitaj user i config fajlove
        if(isStorage){
            System.out.println("Direktorijum je vec skladiste. Unesite username i password kako biste se konektovali na skladiste.");
            System.out.println("Username:");
            String username = scanner.nextLine();
            System.out.println("Password:");
            String password = scanner.nextLine();

            ObjectMapper objectMapper = new ObjectMapper();
            User user = null;
            try {
                user = objectMapper.readValue(new java.io.File(usersLocalPath), User.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Provera kredencijala - uporedjivanje prosledjenih username i password-a i procitanih iz users.json fajla
            if(username.equalsIgnoreCase(user.getUsername()) && password.equalsIgnoreCase(user.getPassword())){
                try {
                    // Ako se podaci User-a match-uju, procitaj config, setuj trenutni storage i dodaj skladiste u listu skladista
                    DriveStorageModel storageModel = objectMapper.readValue(new java.io.File(configLocalPath), DriveStorageModel.class);
                    this.driveStorageModelList.add(storageModel);
                    setCurrentStorage(storageModel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //TODO: throw exception
                System.out.println("Neispravan username ili password!");
                return;
            }
            // Pravimo novo skladiste, prilikom kreiranja User-u koji ga je kreirao dodeljujemo sve privilegije
        } else {
            System.out.println("Direktorijum nije skladiste. Da li zelite da kreirate novo skladiste? Unesite DA ili NE");
            String choice = scanner.nextLine();

            if(choice.equalsIgnoreCase("DA")){

                createStorage(rootName);

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

                //pravimo users.json i config.json U LOKALNOM
                java.io.File usersLocal = new java.io.File(usersLocalPath);
                java.io.File configLocal = new java.io.File(configLocalPath);

                DriveStorageModel storageModel = new DriveStorageModel(user, rootName, downloadPath, usersLocal, configLocal);
                put(usersLocalPath, rootName);
                put(configLocalPath, rootName);

                this.driveStorageModelList.add(storageModel);
                setCurrentStorage(storageModel);
            } else {
                System.out.println("Skladiste nije kreirano.");
                return;
            }

        }

    }
    @Override
    public void limitNumberOfFiles(int i, String s) {

    }

    @Override
    public void limitStorageSize(long l) {

    }

    @Override
    public void restrictExtension(String s) {

    }

    @Override
    public void addNewUser(AbstractUser abstractUser, Set<Privileges> set) {

    }


    @Override
    public void disconnectUser(AbstractUser abstractUser) {

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
                System.out.println("File/Folder ID found!");
            }
        }
        return id;
    }

    //MOZE BITI PROBLEMATICNA METODA

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
                System.out.println("File/Folder boolean found!");
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
                System.out.println("File/Folder found!");
            }
        }
        return targetFile;
    }

    private void createStorage(String folderName) {
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
        System.out.println("Storage created!");
        System.out.println("Storage ID: " + file.getId());
    }

    public DriveStorageModel getCurrentStorage() {
        return currentStorage;
    }

    public void setCurrentStorage(DriveStorageModel currentStorage) {
        this.currentStorage = currentStorage;
    }
}
