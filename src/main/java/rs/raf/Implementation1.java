package rs.raf;

import com.google.gson.Gson;
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


        // todo sort
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

                // changing the date format
//                Date dateFromUtilDate = t.getDate();
//
//                Instant instant = dateFromUtilDate.toInstant();
//                LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
//
//                String formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

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

    private String formatDate(Date date){
        Date dateFromUtilDate = date;

        Instant instant = dateFromUtilDate.toInstant();
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        String formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return formattedDate;
    }

    @Override
    public void importCSV(Schedule schedule, String filePath) {

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

    }

    @Override
    public void importPDF(Schedule schedule, String filePath) {

    }

    @Override
    public void exportJSON(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            // Convert the entry set to a list of maps
            List<Map<String, Object>> dataList = convertMapToListOfMaps(schedule.getScheduleMap());

            new Gson().toJson(dataList, writer);
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception according to your application's needs
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


            map.put("lecture", classLectureDetails);
            map.put("Term",termDetails);



            result.add(map);
        }

        return result;

    }



    @Override
    public void importJSON(Schedule schedule, String filePath) {

    }
}
