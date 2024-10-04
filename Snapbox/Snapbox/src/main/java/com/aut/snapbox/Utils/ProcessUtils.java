package com.aut.snapbox.Utils;

import com.aut.snapbox.Model.DeliveryInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.aut.snapbox.Model.Constants.pathSeparator;

@Component
public class ProcessUtils {
    @Value(value = "${directory}")
    private String directoryPath;
    public static List<Double> removeInvalidEntries(List<DeliveryInfo> deliveryInfos) {
        List<Double> speeds = new ArrayList<Double>();
        Iterator<DeliveryInfo> iterator = deliveryInfos.iterator();
        if (deliveryInfos.size() < 2) return speeds;

        DeliveryInfo p1 = iterator.next();
        while (iterator.hasNext()) {
            DeliveryInfo p2 = iterator.next();

            double distance = NormalDistanceCalc.calculateDistance(p1.getLat(), p1.getLng(), p2.getLat(), p2.getLng())[2];
            double timeDiffHours = Math.abs(p2.getTimestamp() - p1.getTimestamp()) / 3600.0;

            double velocity = distance / timeDiffHours; // Speed in km/h

            if (velocity > 100) {
                iterator.remove(); // Removes p2
            } else {
                distance = Haversine.calculateDistance(p1.getLat(), p1.getLng(), p2.getLat(), p2.getLng());
                double speed = distance / timeDiffHours; // Speed in km/h

                if (speed > 100) {
                    iterator.remove();
                } else {
                    speeds.add(speed);
                    p1 = p2;
                }
            }
        }
        return speeds;
    }

    public static double calculateFare(List<DeliveryInfo> deliveryInfos, List<Double> speeds) {
        double fare = 1.30;

        for (int i = 0; i < deliveryInfos.size() - 1; i++) {
            DeliveryInfo p1 = deliveryInfos.get(i);
            DeliveryInfo p2 = deliveryInfos.get(i + 1);
            double speed = speeds.get(i);

            LocalDateTime time1 = LocalDateTime.ofInstant(Instant.ofEpochSecond(p1.getTimestamp()), ZoneId.systemDefault());
            LocalDateTime time2 = LocalDateTime.ofInstant(Instant.ofEpochSecond(p2.getTimestamp()), ZoneId.systemDefault());

            double timeDifferenceHours = Math.abs(time2.getHour() - time1.getHour())
                    + Math.abs(time2.getMinute() - time1.getMinute()) / 60.0;

            if (speed > 10) {
                if (time1.getHour() >= 5 && time2.getHour() >= 5) {
                    fare += 0.74 * ((p2.getTimestamp() - p1.getTimestamp()) / 3600.0) * speed;
                } else {
                    fare += 1.30 * ((p2.getTimestamp() - p1.getTimestamp()) / 3600.0) * speed;
                }
            } else {
                fare += 11.90 * timeDifferenceHours;
            }
        }

        if (fare < 3.47) {
            fare = 3.47;
        }
        return fare;
    }

    public boolean processOnData(List<List<DeliveryInfo>> deliveryInfos) {
        List<String> csv = new ArrayList<>();
        for (List<DeliveryInfo> deliveryInfo : deliveryInfos) {
            List<Double> speeds = removeInvalidEntries(deliveryInfo);
            String res = deliveryInfo.get(0).getId_delivery() + ", " + calculateFare(deliveryInfo, speeds);
            csv.add(res);
        }

        String name = "CalculatedFee" + "_" + System.currentTimeMillis() + ".csv";
        try {
            writeCsvFile(csv, directoryPath, name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void writeCsvFile(List<String> lines, String directoryPath, String fileName) throws IOException {
        String filePath = directoryPath + pathSeparator + pathSeparator + fileName;
        File directory = new File(directoryPath);
        if (!directory.exists())
            directory.mkdir();
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            throw e;
        }
    }
}
