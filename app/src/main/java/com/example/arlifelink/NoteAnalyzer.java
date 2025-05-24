package com.example.arlifelink;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAnalyzer {
    // Define thresholds: e.g., 30 minutes (in milliseconds)
    private static final long MIN_TIME_GAP_MS = 3_600_000;
    // Optionally, if you use location, define a maximum allowed distance (in km)
    private static final double MAX_DISTANCE_KM = 5.0;
    // Define the date format for the dueDate string (adjust as needed)
    private static final String DATE_FORMAT = "dd/M/yyyy HH:mm";

    private static long getTimeOfDayMillis(String dueDateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        java.util.Date date = sdf.parse(dueDateStr);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int millisecond = cal.get(Calendar.MILLISECOND);
        return hour * 3600000L + minute * 60000L + second * 1000L + millisecond;
    }

    public static void analyzeNotes(List<Note> notes) {
        if (notes == null || notes.isEmpty()) return;

        // Sort notes based on the time-of-day (ignoring the date)
        Collections.sort(notes, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {

                try {
                    String full1 = n1.getDueDate() + " " + n1.getReminder();
                    String full2 = n2.getDueDate() + " " + n2.getReminder();

                    DateFormat fmt = new SimpleDateFormat("dd/M/yyyy HH:mm", Locale.getDefault());
                    fmt.setLenient(false);

                    long t1 = fmt.parse(full1).getTime();
                    long t2 = fmt.parse(full2).getTime();

                    return Long.compare(t1, t2);

                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        // Flag notes if the gap between consecutive notes is less than 1 hour.
        // The first note is never flagged since there’s no previous note.
        for (int i = 1; i < notes.size(); i++) {
            try {
                String full1 = notes.get(i).getDueDate() + " " + notes.get(i).getReminder();
                String full2 = notes.get(i-1).getDueDate() + " " + notes.get(i-1).getReminder();

                DateFormat fmt = new SimpleDateFormat("dd/M/yyyy HH:mm", Locale.getDefault());
                fmt.setLenient(false);

                long t1 = fmt.parse(full1).getTime();
                long t2 = fmt.parse(full2).getTime();
                long timeDiff = t1-t2;
                if (timeDiff < MIN_TIME_GAP_MS) {
                    notes.get(i).setFlagged(true);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // Optionally, if you have location data, flag notes that are too far apart.
        for (int i = 1; i < notes.size(); i++) {
            Note prev = notes.get(i - 1);
            Note curr = notes.get(i);
                double distance = calculateDistance(
                        getLatitude(prev), getLongitude(prev),
                        getLatitude(curr), getLongitude(curr)
                );
                if (distance > MAX_DISTANCE_KM) {
                    curr.setFlagged(true);
                }
        }

    }

    // Dummy helper methods—adjust these if you add location fields to Note

    private static double getLatitude(Note note) {
        String a = note.getLocation();
        String[] parts = a.split(",");
        String latPart = parts[0].split(":")[1].trim();
        double b = Double.parseDouble(latPart);
        return b;
    }

    private static double getLongitude(Note note) {
        String a = note.getLocation();
        String[] parts = a.split(",");
        String latPart = parts[1].split(":")[1].trim();
        double b = Double.parseDouble(latPart);
        return b;
    }


    // Haversine formula to calculate distance (in kilometers) between two coordinates.
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}