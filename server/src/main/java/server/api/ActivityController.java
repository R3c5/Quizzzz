package server.api;

import commons.Activity;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.database.ActivityRepository;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static server.Config.isInvalid;
import static server.Config.isNullOrEmpty;


@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    public static final String ASSET_DIR = (System.getProperty("user.dir") + "/server/src/main/resources/assets/")
            .replace("server/server", "server");
    private final ActivityRepository repo;
    private final Random random;

    /**
     * Constructor method
     *
     * @param repo - ActivityRepository that the controller will use
     */
    public ActivityController(Random random, ActivityRepository repo) {
        this.random = random;
        this.repo = repo;
    }

    /**
     * Checks where the next directory/file is wrttten to make sure it will only write in the assets directory.
     *
     * @param destinationDir The assets directory.
     * @param zipEntry       The next entry in the zip archive.
     * @return The destination for the new entry if valid.
     * @throws IOException
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Check if all the attributes of the activity are neither null nor empty
     *
     * @param activity - Activity to be checked
     * @return true if any of the attributes is null or empty
     */
    private boolean invalidActivity(long id, Activity activity) {
        if (isNullOrEmpty(activity.title) || activity.consumption_in_wh <= 0L
                || isNullOrEmpty(activity.image_path) || isNullOrEmpty(activity.source)) {
            return true;
        }

        Optional<Activity> required = getAllActivities()
                .stream()
                .filter(a -> a.id != id)
                .filter(a -> a.title.equals(activity.title))
                .findFirst();

        return !(activity.title.matches("([a-zA-Z0-9-]+ ){2,}\\w(.*)") && required.isEmpty());
    }

    /**
     * Tries to create a new folder.
     *
     * @param folder The folder to be created.
     * @throws IOException
     */
    private void tryCreateFolder(File folder) throws IOException {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Failed to create directory " + folder);
        }
    }

    /**
     * Downloads the image if the path is a link providing a supported filetype and updates the activity path to a
     * relative path
     *
     * @param activity The activity that was added.
     */
    public void downloadImage(Activity activity) {
        try {
            URL url = new URL(activity.image_path);

            String[] stringURL = activity.image_path.split("\\.");
            String extension = stringURL[stringURL.length - 1];

            File f = new File(ASSET_DIR + "downloaded");
            f.mkdirs();

            switch (extension) {
                case "jpg", "jpeg", "png" -> {
                    activity.image_path = "downloaded/" + activity.id + "." + extension;
                }
                default -> throw new UnsupportedOperationException("Unsupported filetype");

            }

            URLConnection uCon = url.openConnection();

            try (OutputStream   OUTSTREAM = new BufferedOutputStream(
                    new FileOutputStream(ASSET_DIR + activity.image_path));
                 InputStream IS = uCon.getInputStream()
            ) {
                IS.transferTo(OUTSTREAM);
            }
        } catch (Exception ignored) {
        }
        repo.save(activity);
    }

    /**
     * Delete image corresponding to the provided path.
     *
     * @param path The path of the image.
     */
    public void deleteImage(String path) {
        File f = new File(ASSET_DIR + path);
        f.delete();
    }

    /**
     * Clears the assets directory.
     */
    public void deleteAllImages() {
        try {
            FileUtils.cleanDirectory(new File(ASSET_DIR));
        } catch (IOException | IllegalArgumentException ignored) {
        }
    }

    /**
     * Get all the activities from the repository
     *
     * @return a List containing all the activities from the repository
     */
    @GetMapping(path = {"", "/"})
    public List<Activity> getAllActivities() {
        return repo.findAll();
    }

    /**
     * Add a new activity to the repository and download the corresponding image if the path is a valid URL.
     *
     * @param activity - Activity to be added
     * @return the response with the new activity if added successfully or return bad request if not
     */
    @PostMapping(path = {"", "/"})
    public ResponseEntity<Activity> addActivity(@RequestBody Activity activity) {
        if (invalidActivity(activity.id, activity)) {
            return ResponseEntity.badRequest().build();
        }
        Activity saved = repo.save(activity);
        downloadImage(saved);
        return ResponseEntity.ok(saved);
    }

    /**
     * Removes all activities from the database and deletes all images.
     *
     * @return The number of removed entries
     */
    @DeleteMapping(path = {"", "/"})
    public ResponseEntity<HttpStatus> removeAllActivities() {
        repo.deleteAll();
        deleteAllImages();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Get one activity from the repository by its id
     *
     * @return an Activity from the repository that has the same id as the one in the URL
     */
    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable("id") long id) {
        if (isInvalid(id, repo)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repo.findById(id).get());
    }


    /**
     * Update the activity with the given id with the fields of the activity given in the body and downloads a new
     * image if the URL is valid.
     *
     * @param id              - long representing the id of the activity that will be modified
     * @param activityDetails - Activity containing the new information
     * @return the updated activity if updated successfully or bad request otherwise
     */
    @PutMapping(path = {"/{id}"})
    public ResponseEntity<Activity> updateActivityById(@PathVariable("id") long id,
                                                       @RequestBody Activity activityDetails) {

        if (isInvalid(id, repo) || invalidActivity(id, activityDetails)) {
            return ResponseEntity.badRequest().build();
        }

        Activity activity = repo.findById(id).get();

        //Update the activity with the new attributes given in the request
        activity.title = activityDetails.title;
        activity.consumption_in_wh = activityDetails.consumption_in_wh;
        activity.image_path = activityDetails.image_path;
        activity.source = activityDetails.source;

        //Save the attribute to the repo
        Activity saved = repo.save(activity);
        downloadImage(saved);
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete the activity with the given id from the repository and delete the corresponding image.
     *
     * @param id - long representing the id of the activity to be removed
     * @return the response with the id of the deleted activity or empty response if there was no activity with the id
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> removeActivityById(@PathVariable("id") long id) {
        //Get the activity with the id or null if it does not exist
        if (isInvalid(id, repo)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Activity activity = repo.findById(id).get();
        //Get the id and delete the activity
        deleteImage(activity.image_path);
        repo.delete(activity);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Gets a random activity from the database
     *
     * @return Randomly-fetched activity, if any entries exist
     */
    @GetMapping("/rnd")
    public ResponseEntity<Activity> getRandomActivity() {
        List<Activity> activities = this.getAllActivities();
        if (activities.size() == 0) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.ok(activities.get(random.nextInt(activities.size())));
    }

    /**
     * Unzips the provided zip file into the assets folder.
     *
     * @throws IOException
     */
    @RequestMapping(value = "/zip", method = RequestMethod.PUT, consumes = "application/zip")
    public void unzipFile(InputStream is) throws IOException {
        File destination = new File(ASSET_DIR);
        try (ZipInputStream ZIS = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = ZIS.getNextEntry()) != null) {
                File newFile = newFile(destination, zipEntry);
                if (zipEntry.isDirectory()) {
                    tryCreateFolder(newFile);
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    tryCreateFolder(parent);

                    // write file content
                    try (FileOutputStream FOS = new FileOutputStream(newFile)) {
                        ZIS.transferTo(FOS);
                    }
                }
            }
        }
    }

}
