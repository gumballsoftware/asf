package org.codeatlanta.natureevents.asf;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AsfScraper {

    private static final String ASF_URL = "https://atlantasciencefestival.org/events-2019/";
    private static final int JSOUP_TIMEOUT = 8000;

    public static void main(String[] args) {
        //Verify the output file doesn't exist before wasting resources getting the data
        final String fileName = "asf.csv";
        final Path path = Paths.get(fileName);
        //Get the data
        Elements content;
        List<AsfScraperRow> eventList = new ArrayList<>();
        try {
            final Connection.Response response = Jsoup.connect(ASF_URL).timeout(JSOUP_TIMEOUT).execute();
            final Document document = response.parse();

            if (document == null){
                System.out.println("Error: document is null.");
                return;
            }

            content = document.getElementsByClass("event");
            System.err.println("n events: "+ content.size());
            for (int idx = 0; idx < content.size(); idx++) {
                AsfScraperRow event = new AsfScraperRow();
                Element div = content.get(idx);
                Element info = div.getElementsByClass("info").get(0);
                event.url = info.children().get(0).child(0).absUrl("href");
                event.title = info.children().get(0).child(0).text();
                String dateString = info.children().get(1).text();
                String[] dateParts = dateString.split(" ");

                event.startDate = LocalDate.parse(dateParts[1], DateTimeFormat.forPattern("M/d/y").withDefaultYear(new LocalDate().getYear()));
                event.endDate = event.startDate;
                event.startTime = LocalTime.parse(dateParts[3], DateTimeFormat.forPattern("h:ma"));
                event.endTime = LocalTime.parse(dateParts[5], DateTimeFormat.forPattern("h:ma"));

                event.location = dateParts[6];
                event.description = info.children().get(2).text() + " " + info.children().get(3).text();
                eventList.add(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Format the data
        final StringBuilder sb = new StringBuilder();
        final String headerRow = "Organization/organizer,Title,Description (optional),URL of event,Location,"
                + "\"Category (e.g. hiking, birding, volunteering, class)\",Start Date,End Date (if multi-day),"
                + "Start Time,End Time,Free or paid?,RSVP info,Age group (if specified),Dog-friendly (if specified),"
                + "Indoor or outdoor?,Imported to Google Calendar";
        sb.append(headerRow);
        sb.append("\n");
        for(AsfScraperRow row : eventList){
            sb.append(row.organizer);
            sb.append(",");
            sb.append(escapeCommasAndQuotes(row.title));
            sb.append(",");
            sb.append(escapeCommasAndQuotes(row.description));
            sb.append(",");
            sb.append(escapeCommasAndQuotes(row.url));
            sb.append(",");
            sb.append(escapeCommasAndQuotes(row.location));
            sb.append(",");
            //no category sb.append(row);
            sb.append(",");
            sb.append(row.startDate);
            sb.append(",");
            if (row.endDate != null) sb.append(row.endDate);
            sb.append(",");
            if (row.startTime != null) sb.append(row.startTime.toString("HH:mm"));
            sb.append(",");
            if (row.endTime != null) sb.append(row.endTime.toString("HH:mm"));
            sb.append(",");
            sb.append(row.cost);
            sb.append(",");
            //No RSVP Info sb.append(row.url);
            sb.append(",");
            //no age group sb.append(row);
            sb.append(",");
            //no dog friendly sb.append(row);
            sb.append(",");
            //no indoor / outdoor sb.append(row);
            sb.append(",");
            sb.append("No");//imported to Google Calendar
            sb.append("\n");
        }

        //Write the data to file
        try{
            if (Files.exists(path)){
                Files.delete(path);
            }
            Files.createFile(path);
            final byte[] bytes = sb.toString().getBytes();
            Files.write(path, bytes);

            System.out.println("Wrote output to " + fileName);

        }catch(Exception e){
            System.out.println("Exception");
            e.printStackTrace();
        }
    }


    private static class AsfScraperRow {
        public final String organizer = "Atlanta Science Festival";
        public String title;
        public String description;
        public String url;
        public String location;
        public LocalDate startDate;
        public LocalDate endDate;
        public LocalTime startTime;
        public LocalTime endTime;
        public String cost = "";
        //public String rsvpInfo; // n/a
        public AsfScraperRow(){}
    }

    private static String escapeCommasAndQuotes(String input){
        if (input.indexOf("\"") != -1){
            input = input.replace("\"", "\"\"");
        }
        if (input.indexOf(",") != -1){
            input = "\"" + input + "\"";
        }
        return input.replaceAll("[^\\x00-\\x7F]", " ");
    }
}

