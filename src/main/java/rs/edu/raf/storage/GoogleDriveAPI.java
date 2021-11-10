package rs.edu.raf.storage;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoogleDriveAPI implements FileStorage {

    private Drive service;
    private File users;
    private File config;
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

    //Nije moguce uploadovati na google drive bez http-a!!!
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

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId());
        System.out.println("UnsupportedOperation cannot upload to remote google drive storage without http requests!");
    }

    @Override
    public void list() {
        String type;
        FileList result = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name, mimeType, size)")
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
                    .setFields("files(id, name, mimeType, size, modifiedTime, createdTime)")
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
    public void get(String path) {

        String fileId = findID(path);
        String fileName = getFile(path).getName();
        String fileMime = null;
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

    @Override
    public void initializeStorage(String s) {
        try {
            this.service = getDriveService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //mozda bespotrebna implementacija jedne instance nase GoogleDrive klase
        this.fileStorageInstance = new GoogleDriveAPI();

        //lokalni download folder
        java.io.File downloadFolder = new java.io.File("C:/skladiste/download/");
        downloadFolder.mkdir();

        // TODO: napraviti user.json i config.json fajlove da li ih bolje praviti u C:/skladiste gde je i download folder ili samo direktno na drajv?
        this.users = new File().setName("users.json");
        this.config = new File().setName("config.json");

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
                    .setFields("files(id, name)")
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

    public File getFile(String filename){
        FileList result = null;
        File targetFile = null;
        try {
            result = service.files().list()
                    .setFields("files(id, name)")
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
}
