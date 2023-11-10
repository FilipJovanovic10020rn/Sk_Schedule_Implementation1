package rs.raf;

import rs.raf.classes.Classroom;
import rs.raf.classes.Schedule;
import rs.raf.classes.Term;
import rs.raf.enums.AddOns;
import rs.raf.schedule_management.ClassSchedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        ClassSchedule classSchedule = new Implementation1();

        //TODO IZBACI DRUGI DATUM IZ EXCEPTION U IMPLEMENTACIJI1
        List<Classroom> classrooms = new ArrayList<>();
        // nemoj null kao addons
        //classrooms.add(classSchedule.createClassroom(classrooms,"Ucionica",2, AddOns.PEN));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Schedule schedule = null;
        try {
            Date startDate = dateFormat.parse("01.10.2023");
            Date endDate = dateFormat.parse("01.12.2023");

            schedule = classSchedule.initializeSchedule("kita", classrooms, startDate, endDate, 12, 20);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(schedule==null){
            System.out.println("pukli smo ko picke");
        }


        ///////////////////////////

        try {
            Date startDate = dateFormat.parse("02.10.2023");
            classSchedule.createClass(schedule, 13, 3, "Ucionica", "SK", "Surla", startDate, startDate);
            // OVO PROLAZI
//            classSchedule.createClass(schedule, 13, 1, "Ucionica", "SK", "Surla", startDate, startDate);

            List<Term> termList = classSchedule.findTerms(schedule,startDate,2,true,"Ucionica");

            for(Term term : termList){
                System.out.println(term.getStartTime());
            }
            System.out.println("?");
            // nisam proverio da li je null
//            List<Term> termList = classSchedule.findTerms(schedule,startDate,null,"Surla",false);
            // nisam proverio da li je null
//            List<Term> termList = classSchedule.findTerms(schedule,"SK");

            Date newstartDate = dateFormat.parse("03.10.2023");

            classSchedule.rescheduleClass(schedule,startDate,startDate,13,"Ucionica","SK",newstartDate,newstartDate,12,"Ucionica");

            termList = classSchedule.findTerms(schedule,startDate,2,true,"Ucionica");

            for(Term term : termList){
                System.out.println(term.getStartTime());
            }
            System.out.println("novi");
            termList = classSchedule.findTerms(schedule,newstartDate,2,true,"Ucionica");

            for(Term term : termList){
                System.out.println(term.getStartTime());
            }


        }
        catch (Exception e){
            e.printStackTrace();
        }



    }
}
