import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GamsRunner {
    private static final String PATH_TO_GAMS_SETS_FILE_PATTERN = "C:\\Users\\Maria\\Documents\\gamsdir\\projdir\\%d\\sets.inc";
    private static final String PATH_TO_GAMS_DATA_FILE_PATTERN = "C:\\Users\\Maria\\Documents\\gamsdir\\projdir\\%d\\data.inc";
    private static final String PATH_TO_GAMS_RESULTS_FILE_PATTERN = "C:\\Users\\Maria\\Documents\\gamsdir\\projdir\\%d\\results.txt";
    private static final String PATH_TO_MODEL_GAMS_PATTERN = "C:\\Users\\Maria\\Documents\\gamsdir\\projdir\\%d\\my_model.gms";

    private static final String WORKING_DIR_PATH_PATTERN = "C:\\Users\\Maria\\Documents\\gamsdir\\projdir\\%d";
    private static final String PATH_TO_GAMS_EXE = "\"C:\\Program Files (x86)\\GAMS23.2\\gams.exe\"";


    private static final String PATH_TO_INPUT_DATA_FILES_PATTERN = "C:\\Users\\Maria\\source\\repos\\maxima\\MKL_JOBS\\GAMS_INPUT";
    private static final String PATH_TO_INPUT_SETS_FILE_PATTERN = "D:\\Projects\\gams_runner\\src\\main\\resources\\%d_jobs_sets.txt";


    private static final String JOBS_TIME_FILENAME_FORMAT = "%d_jobs_gams_time_results.txt";
    private static final String PATH_TO_OUTPUT_PATTERN = "D:\\Projects\\gams_runner\\results\\%s";

    private static final int THREADS_COUNT = 4;
    private static ConcurrentLinkedQueue<Path> queuePath;

    public static void main(String[] args) throws IOException {
         try (Stream<Path> stream = Files.walk(Paths.get(PATH_TO_INPUT_DATA_FILES_PATTERN))) {
             List<Path> paths = stream.collect(Collectors.toList());
             paths.removeIf(p -> !p.getFileName().toString().endsWith(".txt"));
             queuePath = new ConcurrentLinkedQueue<>(paths);
             var threadPool = Executors.newFixedThreadPool(THREADS_COUNT);
             for (int i = 1; i <= THREADS_COUNT; i++) {
                 int dirNumber = i;
                 threadPool.submit(() -> run(dirNumber));
             }
         }
    }

    private static void run(int dirNumber) {
        File workingDir = new File(String.format(WORKING_DIR_PATH_PATTERN, dirNumber));
        ProcessBuilder processBuilder = new ProcessBuilder(PATH_TO_GAMS_EXE, String.format(PATH_TO_MODEL_GAMS_PATTERN, dirNumber));
        processBuilder.directory(workingDir);
        while (!queuePath.isEmpty()) {
            try {
                Path path = queuePath.poll();
                System.out.println("start to execute: " + path.getFileName().toString());
                int countJobs;
                if (path.getFileName().toString().contains("_4j_")) {
                    countJobs = 4;
                } else if (path.getFileName().toString().contains("_6j_")) {
                    countJobs = 6;
                } else if (path.getFileName().toString().contains("_7j_")) {
                    countJobs = 7;
                } else if (path.getFileName().toString().contains("_8j_")) {
                    countJobs = 8;
                } else {
                    continue;
//                    countJobs = 10;
                }
                Files.copy(Paths.get(String.format(PATH_TO_INPUT_SETS_FILE_PATTERN, countJobs)),
                        Paths.get(String.format(PATH_TO_GAMS_SETS_FILE_PATTERN, dirNumber)), StandardCopyOption.REPLACE_EXISTING);

                Files.copy(path, Paths.get(String.format(PATH_TO_GAMS_DATA_FILE_PATTERN, dirNumber)), StandardCopyOption.REPLACE_EXISTING);

                try {
                    long startTime = System.currentTimeMillis();
                    Process process = processBuilder.start();
                    process.waitFor();
                    long duration = System.currentTimeMillis() - startTime;
                    writeJobsTimeResults(path.getFileName().toString(), duration, countJobs);
                } catch (InterruptedException e) {
                    System.out.println("ERROR!!! CAN'T EXECUTE " + path.getFileName().toString() + " because " + e.getMessage());
                }
                String fileName = "out_" + path.getFileName().toString();
                Files.copy(Paths.get(String.format(PATH_TO_GAMS_RESULTS_FILE_PATTERN, dirNumber)),
                        Paths.get(String.format(PATH_TO_OUTPUT_PATTERN, fileName)),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                System.out.println("exception: " + ex.getMessage());
            }
        }
    }


    private static synchronized void writeJobsTimeResults(String filename, Long time, int count) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(String.format(JOBS_TIME_FILENAME_FORMAT, count), true))) {
            bufferedWriter.write(filename + "\t" + time);
            bufferedWriter.newLine();
        }
    }
}
