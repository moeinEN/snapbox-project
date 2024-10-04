package com.aut.snapboxfilereader;


import com.aut.snapboxfilereader.Model.DeliveryInfo;
import com.aut.snapboxfilereader.Model.FileProperty;
import com.aut.snapboxfilereader.Model.KafkaMessage;
import com.aut.snapboxfilereader.exceptions.DirNotExistException;
import com.aut.snapboxfilereader.exceptions.FileNotExistsException;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.aut.snapboxfilereader.Model.Constants.pathSeparator;

@Service
@EnableScheduling
public class MainService {
    @Autowired
    private ThreadPoolExecutor threadPoolExecutorFileReceiver;

    @Value(value = "${max.files.read}")
    private int maxFilesToRead;

    @Value(value = "${directory}")
    private String directoryPath;

    @Value(value = "${file.lock.extension}")
    private String fileLockExtension;

    @Value(value = "${file.rename.extension}")
    private String fileRenameExtension;

    @Value(value = "${file.format.extension}")
    private String fileFormatExtension;

    @Value(value = "${move.dir.name}")
    private String moveDirName;

    @Value(value = "${result.dir.name}")
    private String resultDirName;

    @Value(value = "${result.format.extension}")
    private String resultFormatExtension;

    @Value(value = "${kafka.work.topic.partitions}")
    private int workTopicPartition;

    @Value(value = "${kafka.work.topic.replications}")
    private short workTopicReplications;

    @Autowired
    private AdminClient adminClient;

    private boolean isSetup = false;

    @PostConstruct
    private void init() {
        try {
            createTopicIfNotExists(adminClient, "Snap-Box-Topic", workTopicPartition, workTopicReplications);
        } catch (Exception e){
            e.printStackTrace();
        }
        isSetup = true;
    }


    @Scheduled(cron = "${create.file.reader.task.cron}")
    public void createFileReceiverTask() {
        if (isSetup) {
            processReadingFiles();
        } else {
            System.out.println("Service init was not run successfully");
        }
    }



    public void processReadingFiles() {
        List<FileProperty> listOfFile;
        try {
            int maxFilesToRead = this.maxFilesToRead;
            listOfFile = getListFiles(directoryPath , maxFilesToRead);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // for each file of a path
        for (FileProperty file : listOfFile) {
            if (!file.getFileName().endsWith(fileLockExtension)) {
                // check if we have pattern and it matches
                boolean isMatched = true;
                if (Objects.nonNull(fileFormatExtension)) {
                    try {
                        isMatched = file.getFileName().matches(fileFormatExtension);
                    } catch (Exception e) {
                        e.printStackTrace();
                        isMatched = false;
                    }
                }
                if (isMatched) {
                    submitTaskToThreadPoolFileReceiver(() -> readFile(file, fileLockExtension, fileRenameExtension, moveDirName));
                }
            }
        }
    }
    private void submitTaskToThreadPoolFileReceiver(Runnable runnable) {
        while (this.threadPoolExecutorFileReceiver.getQueue().size() > 1000) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException var3) {
            }
        }
        this.threadPoolExecutorFileReceiver.submit(runnable);
    }
    private void readFile(FileProperty file, String fileLockExtension, String fileLRenameExtension, String moveDirName) {
        try {
            lockFile(file, fileLockExtension);
        } catch (FileNotExistsException e) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        getContent(file, fileLockExtension);
        try {
             renameAndMoveProcessedFile(file, fileLockExtension, fileLRenameExtension, moveDirName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTopicIfNotExists(AdminClient adminClient, String topicName, int partitions, short replicationFactor) throws Exception {
        try {
            if (!adminClient.listTopics().names().get().contains(topicName)) {
                NewTopic topic = new NewTopic(topicName, partitions, replicationFactor);
                adminClient.createTopics(Collections.singletonList(topic)).all().get();
            } else {
                System.out.println("Topic " + topicName + " already exists, skipping creation.");
            }
        } catch (Exception e) {
            System.out.println("Error while checking/creating topic " + topicName);
            throw e;
        }
    }
    public List<FileProperty> getListFiles(String directoryPath, int maxFilesToRead) throws Exception {

        List<FileProperty> filePropertyList = new ArrayList<>();

        File dir = new File(directoryPath);
        Stream<File> fileStream;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            fileStream = Arrays.stream(files).filter(file -> file.isFile());
            if (maxFilesToRead > 0) {
                fileStream = fileStream.limit(maxFilesToRead);
            }
            fileStream.forEach(file -> filePropertyList.add(new FileProperty(file.getName(), directoryPath)));
        } else {
            throw new DirNotExistException(directoryPath);
        }
        return filePropertyList;
    }

    public void getContent(FileProperty file, String fileLockExtension) {
        String line;
        String csvSplitBy = ",";
        try (BufferedReader br = new BufferedReader(new FileReader(file.getFilePath() + "/" + file.getFileName() + fileLockExtension))) {
            br.readLine();
            boolean flagChange = false;
            boolean flagStart = true;
            int idFlow = 0;
            List<DeliveryInfo> deliveryInfoList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(csvSplitBy);
                if (values[0].equals("id_delivery")) {
                    continue;
                }
                int idDelivery = Integer.parseInt(values[0]);
                double lat = Double.parseDouble(values[1]);
                double lng = Double.parseDouble(values[2]);
                long timestamp = Long.parseLong(values[3]);
                if (flagStart) {
                    idFlow = idDelivery;
                }
                if (!flagStart && idDelivery != idFlow) {
                    flagChange = true;
                    idFlow = idDelivery;
                } else if (!flagStart) {
                    flagChange = false;
                }
                flagStart = false;
                DeliveryInfo entry = new DeliveryInfo(idDelivery, lat, lng, timestamp);
                if (flagChange) {
                    KafkaMessage kafkaMessage = new KafkaMessage(deliveryInfoList);
                    sendToKafka(kafkaMessage, "Snap-Box-Topic");
                    deliveryInfoList = new ArrayList<>();
                    deliveryInfoList.add(entry);
                } else {
                    deliveryInfoList.add(entry);
                }
            }
            KafkaMessage kafkaMessage = new KafkaMessage(deliveryInfoList);
            sendToKafka(kafkaMessage, "Snap-Box-Topic");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeCsvFile(List<String> lines, String originalPath, String resultDir, String originalName, String resultExtension) {
        String filePath = originalPath + pathSeparator + resultDir + pathSeparator + originalName + resultExtension;
        File directory = new File(originalPath + pathSeparator + resultDir);
        if (!directory.exists())
            directory.mkdir();
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while writing to the file.");
        }
    }

    public void lockFile(FileProperty file, String lockExtension) throws Exception {
        File previousFile = new File(file.getFilePath() + pathSeparator + file.getFileName());
        File lockedFile = new File(file.getFilePath() + pathSeparator + file.getFileName() + lockExtension);
        if (previousFile.exists()) {
            previousFile.renameTo(lockedFile);
        } else {
            throw new FileNotExistsException(file.getFilePath() + pathSeparator + file.getFileName() + lockExtension);
        }
    }

    public void unLockFile(FileProperty file, String lockExtension) throws Exception {
        File previousFile = new File(file.getFilePath() + pathSeparator + file.getFileName() + lockExtension);
        File unlock = new File(file.getFilePath() + pathSeparator + file.getFileName());
        if (previousFile.exists()) {
            previousFile.renameTo(unlock);
        } else {
            throw new FileNotExistsException(file.getFilePath() + pathSeparator + file.getFileName() + lockExtension);
        }
    }

    public void renameAndMoveProcessedFile(FileProperty file, String lockExtension, String renameExtension, String moveDirName) throws Exception {
        File file1 = new File(file.getFilePath() + pathSeparator + file.getFileName() + lockExtension);
        try {
            File directory = new File(file.getFilePath() + pathSeparator + moveDirName);
            if (!directory.exists())
                directory.mkdir();
            file1.renameTo(new File(file.getFilePath() + pathSeparator + moveDirName + pathSeparator + file.getFileName() + renameExtension));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    private KafkaTemplate<String, KafkaMessage> kafkaTemplate;

    public boolean sendToKafka(KafkaMessage kafkaMessage, String destTopicName) {
        try {
            CompletableFuture<SendResult<String, KafkaMessage>> future = kafkaTemplate.send(destTopicName, kafkaMessage);
            SendResult<String, KafkaMessage> sendResult = future.get(20, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
