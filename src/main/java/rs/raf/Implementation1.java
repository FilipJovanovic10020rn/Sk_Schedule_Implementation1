package rs.raf;

import com.google.gson.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import rs.raf.classes.ClassLecture;
import rs.raf.classes.Classroom;
import rs.raf.classes.Schedule;
import rs.raf.classes.Term;
import rs.raf.exceptions.*;
import rs.raf.schedule_management.ClassSchedule;
import rs.raf.schedule_management.ScheduleManager;

import com.opencsv.CSVWriter;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Implementation1 implements ClassSchedule {

    static {
        ScheduleManager.registerClassScheduler(new Implementation1());
    }

    @Override
    public void createClass(Schedule schedule, int startTime, int duration, String classroomName, String lectureName, String professor, Date fromDate, Date toDate)
            throws DatesException,DurationException,ClassroomDoesntExistException,TermTakenException,WrongStartTimeException, InternalError {


        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }

        if(fromDate.before(schedule.getStartDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }

        if(duration<1){
            throw new DurationException("Trajanje mora biti minimum 1");
        }

        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim imenom");

        int count = 0;

        List<Term> termini = new ArrayList<>();

//        System.out.println(fromDate + " " + toDate);

        for(Map.Entry<Term, ClassLecture> entry : schedule.getScheduleMap().entrySet()){
//            System.out.println(entry.getKey().getDate() + " " + entry.getKey().getStartTime() + " " + entry.getKey().getClassroom());
//            if(entry.getValue()==null){
//                System.out.println("jesam null");
//            }
            for(int i =0 ;i<duration; i++){
                if(entry.getKey().getDate().equals(fromDate) && entry.getKey().getClassroom().getName().equals(classroomName)
                        && entry.getKey().getStartTime() == startTime+i){
                    if(entry.getValue()==null){
                        count++;
                        termini.add(entry.getKey());
                    }
                }
                if(count==duration)
                    break;
            }
            if(count==duration) {
                if(termini.isEmpty()){
                    throw new InternalError("Greska u bazi");
                }
                ClassLecture cl = new ClassLecture(lectureName, professor, startTime, duration, fromDate, toDate);
                for(Term t : termini){
                    schedule.getScheduleMap().put(t,cl);
                }
                return;
            }
        }
        System.out.println("count " + count + " dur " + duration);
        if(count!=duration)
        {
            throw new TermTakenException("ne postoji slobodan termin");
        }
    }


    @Override
    public void removeClass(Schedule schedule,Date date,Date toDate, int startTime, String classroomName, String lectureName)
            throws WrongStartTimeException,DatesException, WrongDateException,WrongLectureNameException, ClassroomDoesntExistException, ClassLectureDoesntExistException
    {

        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(date.before(schedule.getStartDate()) || date.before(schedule.getStartDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int duration = 0;


        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(date) && entry.getKey().getClassroom().getName().equals(classroomName)
                    && entry.getKey().getStartTime() == startTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
            }
        }
        if(duration==0)
        {
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }
        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i = 0; i<duration; i++){
                if(entry.getKey().getDate().equals(date) && entry.getKey().getClassroom().getName().equals(classroomName)
                        && entry.getKey().getStartTime() == startTime+i)
                {
                    schedule.getScheduleMap().put(entry.getKey(),null);
                }
            }
        }
    }


    @Override
    public void rescheduleClass(Schedule schedule, Date oldDate, Date oldToDate, int oldStartTime, String oldClassroomName, String lectureName,
                                Date newDate,Date newToDate, int newStartTime, String newClassroomName)
            throws DatesException,ClassroomDoesntExistException,WrongStartTimeException,WrongDateException,WrongLectureNameException,WrongClassroomNameException, TermTakenException{

        if(schedule.getStartHours()>oldStartTime || schedule.getEndHours()<oldStartTime || schedule.getStartHours()>newStartTime || schedule.getEndHours()<newStartTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(oldDate.before(schedule.getStartDate()) || oldDate.after(schedule.getEndDate()) || newDate.before(schedule.getStartDate()) || newDate.after(schedule.getEndDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag1 = false;
        boolean flag2 = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(oldClassroomName)){
                flag1 = true;
            }
            if(classroom.getName().equals(newClassroomName)){
                flag2 = true;
            }
        }
        if(!flag1 || !flag2)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int duration = 0;
        ClassLecture cl = null;

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(oldDate) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                    && entry.getKey().getStartTime() == oldStartTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
                cl = entry.getValue();
                break;
            }
        }
        if(cl==null){
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }

        ClassLecture cl2 = new ClassLecture(cl.getClassName(),cl.getProfessor(),newStartTime,duration,newDate,newToDate);
        int count = 0;

        List<Term> termini = new ArrayList<>();



        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i =0 ;i<duration; i++){
                if(entry.getKey().getDate().equals(newDate) && entry.getKey().getClassroom().getName().equals(newClassroomName)
                        && entry.getKey().getStartTime() == newStartTime+i){
                    if(entry.getValue()==null){
                        count++;
                        termini.add(entry.getKey());
                    }
                }
                if(count==duration)
                    break;
            }
            if(count==duration) {
                if(termini.isEmpty()){
                    throw new InternalError("Greska u bazi");
                }
                for(Term t : termini){
                    schedule.getScheduleMap().put(t,cl2);
                }
                break;
            }
        }
        if(count!=duration)
        {
            throw new TermDoesntExistException("ne postoji slobodan termin");
        }

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i = 0; i<duration; i++){
                if(entry.getKey().getDate().equals(oldDate) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                        && entry.getKey().getStartTime() == oldStartTime+i)
                {
                    schedule.getScheduleMap().put(entry.getKey(),null);
                }
            }
        }
    }

    @Override
    public void exportCSV(Schedule schedule, String filePath)
    {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Naziv predavanja", "Profesor", "Ucionica", "Datum", "Vreme od", "Vreme do"});
        //        "Naziv predavanja","Profesor","Ucionica","Datum od", "Datum do","Vreme od","Vreme do"


        List<Term> termList = new ArrayList<>();
        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            termList.add(entry.getKey());
        }
        Collections.sort(termList, Comparator
                .comparing(Term::getDate)
                .thenComparing(Term::getStartTime)
        );

        for(Term t : termList){
            if(schedule.getScheduleMap().get(t) == null){
                continue;
            }
            ClassLecture classLecture = schedule.getScheduleMap().get(t);
            if(t.getStartTime()==classLecture.getStartTime()){

                String formattedDate = formatDate(t.getDate());


                data.add(new String[]{classLecture.getClassName(), classLecture.getProfessor(), t.getClassroom().getName(),
                        formattedDate, t.getStartTime()+":00", (classLecture.getDuration()+t.getStartTime())+":00"});
            }

        }

        try {

            // Create directories if they don't exist
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                // Writing all data to the CSV file
                writer.writeAll(data); // ovo prima stringove



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public void importCSV(Schedule schedule, String filePath) {

        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Morate da inicijalizujete raspored prvo");
        }

        int duration;
        boolean flag = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Read the file line by line
            while ((line = br.readLine()) != null) {
                // Split the line by the CSV delimiter (comma, in this case)
                String[] fields = line.split(",");

                // Process the fields
                for (int i = 0; i < fields.length; i++) {
                    // Remove leading and trailing spaces and quotation marks
                    //if
                    fields[i] = fields[i].trim().replaceAll("^\"|\"$", "");


                    System.out.print(fields[i] + " ");
                }
                if(flag){

                    System.out.println(fields[1]);
                    System.out.println(fields[0]);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

                    Date date = dateFormat.parse(fields[3]);

                    String[] start = fields[4].split(":");
                    String[] end = fields[5].split(":");
                    int s = Integer.parseInt(start[0]);
                    int e = Integer.parseInt(end[0]);
                    duration = e-s;

                    ClassLecture cl = new ClassLecture(fields[0],fields[1],s,duration,date,null);

                    System.out.println(cl.toString());
                    for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                        for(int j = 0; j<duration; j++){
                            if(entry.getKey().getDate().equals(date) && entry.getKey().getClassroom().getName().equals(fields[2])
                                    && entry.getKey().getStartTime() == s+j)
                            {
                                schedule.getScheduleMap().put(entry.getKey(),cl);
                            }
                        }
                    }

                }

                flag=true;
                System.out.println(); // Move to the next line
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exportPDF(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }
        try {
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try(PDDocument document = new PDDocument()){

                List<Map<String, Object>> dataList = convertMapToListOfMaps(schedule.getScheduleMap());

                for(Map<String,Object> entry: dataList){
                    PDPage page = new PDPage();
                    document.addPage(page);


                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {



                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12); // Example font and size, adjust as needed
                        contentStream.newLineAtOffset(100, 700);

                        for (Map.Entry<String, Object> field : entry.entrySet()) {
                            contentStream.showText(field.getKey() + ": " + field.getValue().toString());
                            contentStream.newLineAtOffset(0, -20); // Adjust as needed
                        }
                        contentStream.endText();

                    }
                }
                document.save(filePath);
                System.out.println("PDF file exported successfully to: " + filePath);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void importPDF(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Morate da inicijalizujete raspored prvo");
        }

        File file = new File(filePath);
        if(!file.exists()){
            throw new FilePathException("File ne postoji");
        }

        List<Map<String,String>> result = new ArrayList<>();

        try(PDDocument document = PDDocument.load(file)){
            PDFTextStripper textStripper = new PDFTextStripper();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                textStripper.setStartPage(i + 1);
                textStripper.setEndPage(i + 1);

                try {
                    String content = textStripper.getText(document);

                    // Parse the content and extract data into a map
                    // You need to customize this part based on your PDF structure
                    Map<String, String> entry = parsePdfContent(content);

                    result.add(entry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        for(Map<String, String> map : result){

            String professorName = "";
            String classLectureName = "";
            int duration = 0;
            Term term = null;

            for(Map.Entry<String,String> entry: map.entrySet()){

                if(entry.getKey().toString().equals("ClassLecture")){

                    String[] keyValuePairs = entry.getValue()
                            .replaceAll("[{}]", "")
                            .split(", ");

                    Map<String, String> keyValueMap = new HashMap<>();
                    for (String pair : keyValuePairs) {
                        String[] single = pair.split("=");
                        if (single.length == 2) {
                            keyValueMap.put(single[0].trim(), single[1].trim());
                        }
                    }

                    // Extract values from the map
                    String durationString = keyValueMap.get("duration");

                    professorName = keyValueMap.get("professor");
                    classLectureName = keyValueMap.get("className");
                    duration = Integer.parseInt(durationString);


                }
                else{
                    String[] keyValuePairs = entry.getValue()
                            .replaceAll("[{}]", "")
                            .split(", ");

                    Map<String, String> keyValueMap = new HashMap<>();
                    for (String pair : keyValuePairs) {
                        String[] single = pair.split("=");
                        if (single.length == 2) {
                            keyValueMap.put(single[0].trim(), single[1].trim());
                        }
                    }

                    // Extract values from the map
                    String dateString = keyValueMap.get("date");
                    String classroomName = keyValueMap.get("classroom");
                    String startTimeString = keyValueMap.get("startTime");

                    // Parse date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    Date date = null;
                    try {
                        date = dateFormat.parse(dateString);
                    } catch (ParseException e) {
                        e.printStackTrace(); // Handle parsing exception
                    }
                    int startTime = Integer.parseInt(startTimeString);

                    term = new Term(schedule.getClassroomByName(classroomName),startTime,date);

                }
            }
            if(term == null || duration == 0 || classLectureName == "" || professorName == ""){
//                throw new ImportingException("");
            }
            else{
                ClassLecture classLecture = new ClassLecture(classLectureName,professorName, term.getStartTime(), duration, term.getDate(),null);

                boolean toAdd = false;

                for(Map.Entry<Term,ClassLecture> entryInSchedule : schedule.getScheduleMap().entrySet()){
                    Term termForUse = entryInSchedule.getKey();
                    if(termForUse.getStartTime() == term.getStartTime() && termForUse.getDate().equals(term.getDate()) && termForUse.getClassroom().getName().equals(term.getClassroom().getName())){
                        if(entryInSchedule.getValue() == null){
                            toAdd = true;
                            term = termForUse;
                        }
                        else{
                            System.out.println("Termin:"+ term.toString() + " je zauzet, preskace se dodavanje: " + classLecture.toString());
                        }
                    }
                }

                if(toAdd){
                    schedule.getScheduleMap().put(term,classLecture);
                }

            }
        }
        System.out.println("Finished importing file");
    }

    private Map<String,String> parsePdfContent(String content){
        Map<String, String> resultMap = new HashMap<>();

        // Split content into lines
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            // Split each line into key and value based on ": "
            String[] parts = line.split(": ", 2);

            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Add key-value pair to the result map
                resultMap.put(key, value);
            }
        }

        return resultMap;
    }

    @Override
    public void exportJSON(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }
        try {
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            try (FileWriter writer = new FileWriter(filePath)) {
                // Convert the entry set to a list of maps
                List<Map<String, Object>> dataList = convertMapToListOfMaps(schedule.getScheduleMap());

                new Gson().toJson(dataList, writer);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void importJSON(Schedule schedule, String filePath) {


        // Create a Gson instance
        Gson gson = new Gson();

        try (FileReader fileReader = new FileReader(filePath)) {
            // Deserialize JSON data into JsonArray
            JsonArray jsonArray = gson.fromJson(fileReader, JsonArray.class);

            // Access the data
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Access lecture and Term objects dynamically
                JsonObject lecture = jsonObject.getAsJsonObject("ClassLecture");
                int duration = lecture.getAsJsonPrimitive("duration").getAsInt();
                String professor = lecture.getAsJsonPrimitive("professor").getAsString();
                String className = lecture.getAsJsonPrimitive("className").getAsString();

                JsonObject term = jsonObject.getAsJsonObject("Term");
                String date = term.getAsJsonPrimitive("date").getAsString();
                String classroom = term.getAsJsonPrimitive("classroom").getAsString();
                int startTime = term.getAsJsonPrimitive("startTime").getAsInt();

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

                Date startDate = dateFormat.parse(date);

                ClassLecture cl = new ClassLecture(className,professor,startTime,duration,startDate,null);

                for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                    for(int j = 0; j<duration; j++){
                        if(entry.getKey().getDate().equals(startDate) && entry.getKey().getClassroom().getName().equals(classroom)
                                && entry.getKey().getStartTime() == startTime+j)
                        {
                            schedule.getScheduleMap().put(entry.getKey(),cl);
                        }
                    }
                }
                // Print the data
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    // Utility method to convert a map to a list of maps
    private List<Map<String, Object>> convertMapToListOfMaps(Map<Term, ClassLecture> data) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Term, ClassLecture> entry : data.entrySet()) {
            if(entry.getValue()==null){
                continue;
            }
            if(entry.getValue().getStartTime() != entry.getKey().getStartTime()){
                continue;
            }
            Map<String,Object> map = new HashMap<>();

            // adding term json
            Term term = entry.getKey();

            Map<String,Object> termDetails = new HashMap<>();

            termDetails.put("classroom",term.getClassroom().getName());
            termDetails.put("startTime",term.getStartTime());
            termDetails.put("date",formatDate(term.getDate()));


            // adding lecture to json map
            ClassLecture classLecture = entry.getValue();


            Map<String,Object> classLectureDetails = new HashMap<>();
            classLectureDetails.put("className", classLecture.getClassName());
            classLectureDetails.put("professor", classLecture.getProfessor());
            classLectureDetails.put("duration", classLecture.getDuration());


            map.put("ClassLecture", classLectureDetails);
            map.put("Term",termDetails);


            result.add(map);
        }

        return result;

    }

    private String formatDate(Date date){
        Date dateFromUtilDate = date;

        Instant instant = dateFromUtilDate.toInstant();
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        String formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return formattedDate;
    }
}
